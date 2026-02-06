package com.moneytracker.service;

import com.moneytracker.model.*;
import com.moneytracker.repository.TransactionRepository;
import com.moneytracker.repository.CategoryRepository;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailParsingService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    // Email patterns for different services
    private static final Map<String, EmailPattern> EMAIL_PATTERNS = new HashMap<>();

    static {
        // Netflix
        EMAIL_PATTERNS.put("netflix", new EmailPattern(
                "netflix.com",
                "Your Netflix bill",
                "(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Entertainment"
        ));

        // Amazon
        EMAIL_PATTERNS.put("amazon", new EmailPattern(
                "amazon.in",
                "Your Amazon.*order",
                "(?:Total:|Order Total:)\\s*(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Shopping"
        ));

        // Swiggy
        EMAIL_PATTERNS.put("swiggy", new EmailPattern(
                "swiggy.in",
                "Your Swiggy order",
                "(?:Total bill|Bill total)\\s*(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Food & Dining"
        ));

        // Zomato
        EMAIL_PATTERNS.put("zomato", new EmailPattern(
                "zomato.com",
                "Your Zomato order",
                "(?:Total|Bill Amount)\\s*(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Food & Dining"
        ));

        // Uber
        EMAIL_PATTERNS.put("uber", new EmailPattern(
                "uber.com",
                "Your.*trip with Uber",
                "(?:Trip Fare|Total)\\s*(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Transportation"
        ));

        // Credit Card Statements (Generic)
        EMAIL_PATTERNS.put("creditcard", new EmailPattern(
                "statement|credit card",
                "Transaction Alert|Purchase",
                "(?:Amount|Transaction)\\s*(?:Rs\\.|INR|₹)\\s*(\\d+(?:\\.\\d{2})?)",
                "Other"
        ));

        // Bank Debit Alerts
        EMAIL_PATTERNS.put("bank", new EmailPattern(
                "bank|hdfc|icici|sbi|axis",
                "Debit Alert|Debited",
                "(?:debited|withdrawn)\\s*(?:with|by|for)?\\s*(?:Rs\\.|INR|₹)?\\s*(\\d+(?:\\.\\d{2})?)",
                "Other"
        ));
    }

    @Transactional
    public void parseEmailsForUser(User user) {
        if (!user.getEmailParsingEnabled() ||
                user.getEmailImapHost() == null ||
                user.getEmailImapUsername() == null) {
            log.info("Email parsing not enabled or not configured for user: {}", user.getUsername());
            return;
        }

        try {
            Store store = connectToEmailServer(user);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // Get unread messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Found {} unread messages for user: {}", messages.length, user.getUsername());

            int parsedCount = 0;
            for (Message message : messages) {
                try {
                    Transaction transaction = parseEmailMessage(message, user);
                    if (transaction != null) {
                        transactionRepository.save(transaction);
                        parsedCount++;

                        // Create notification
                        notificationService.createNotification(
                                user,
                                NotificationType.EMAIL_PARSED,
                                "Transaction Auto-Added",
                                String.format("₹%.2f transaction added from email: %s",
                                        transaction.getAmount(), transaction.getDescription())
                        );

                        // Mark as read
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                } catch (Exception e) {
                    log.error("Error parsing email: {}", e.getMessage());
                }
            }

            inbox.close(false);
            store.close();

            log.info("Successfully parsed {} transactions from emails for user: {}", parsedCount, user.getUsername());

        } catch (Exception e) {
            log.error("Error connecting to email server for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private Store connectToEmailServer(User user) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", user.getEmailImapHost());
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(user.getEmailImapHost(), user.getEmailImapUsername(), user.getEmailImapPassword());

        return store;
    }

    private Transaction parseEmailMessage(Message message, User user) throws Exception {
        String from = getEmailAddress(message.getFrom());
        String subject = message.getSubject();
        String content = getEmailContent(message);

        // Try to match against known patterns
        for (Map.Entry<String, EmailPattern> entry : EMAIL_PATTERNS.entrySet()) {
            EmailPattern pattern = entry.getValue();

            if (from.toLowerCase().contains(pattern.fromPattern.toLowerCase()) ||
                    subject.toLowerCase().matches(".*" + pattern.subjectPattern.toLowerCase() + ".*")) {

                // Extract amount
                Pattern amountPattern = Pattern.compile(pattern.amountPattern, Pattern.CASE_INSENSITIVE);
                Matcher matcher = amountPattern.matcher(content);

                if (matcher.find()) {
                    String amountStr = matcher.group(1).replace(",", "");
                    BigDecimal amount = new BigDecimal(amountStr);

                    // ✅ SECURE - Find category for THIS USER only
                    Category category = categoryRepository.findByNameAndUserId(pattern.defaultCategory, user.getId())
                            .orElseGet(() -> createDefaultCategory(pattern.defaultCategory, user));  // ✅ Pass user

                    // Create transaction
                    Transaction transaction = new Transaction();
                    transaction.setUser(user);
                    transaction.setDescription(subject);
                    transaction.setAmount(amount);
                    transaction.setTransactionDate(
                            message.getSentDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                    );
                    transaction.setPaymentMethod(detectPaymentMethod(content, from));
                    transaction.setCategory(category);
                    transaction.setSource(TransactionSource.EMAIL_PARSED);
                    transaction.setSourceReference(message.getHeader("Message-ID")[0]);
                    transaction.setNotes("Auto-parsed from email: " + from);

                    return transaction;
                }
            }
        }

        return null;
    }

    private String getEmailAddress(Address[] addresses) {
        if (addresses != null && addresses.length > 0) {
            return addresses[0].toString();
        }
        return "";
    }

    private String getEmailContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.getContent() instanceof String) {
                    sb.append(bodyPart.getContent());
                }
            }
            return sb.toString();
        }
        return "";
    }

    private PaymentMethod detectPaymentMethod(String content, String from) {
        String lowerContent = content.toLowerCase();
        String lowerFrom = from.toLowerCase();

        if (lowerContent.contains("upi") || lowerFrom.contains("upi")) {
            return PaymentMethod.UPI;
        } else if (lowerContent.contains("credit card") || lowerFrom.contains("credit")) {
            return PaymentMethod.CREDIT_CARD;
        } else if (lowerContent.contains("debit card") || lowerFrom.contains("debit")) {
            return PaymentMethod.DEBIT_CARD;
        } else if (lowerContent.contains("wallet") || lowerFrom.contains("paytm") || lowerFrom.contains("amazonpay")) {
            return PaymentMethod.WALLET;
        } else if (lowerFrom.contains("netflix") || lowerFrom.contains("spotify") || lowerFrom.contains("subscription")) {
            return PaymentMethod.SUBSCRIPTION;
        }

        return PaymentMethod.NET_BANKING;
    }

    /**
     * ✅ SECURE - Creates category for specific user
     */
    private Category createDefaultCategory(String categoryName, User user) {
        Category category = new Category();
        category.setUser(user);  // ✅ CRITICAL - Set the owner!
        category.setName(categoryName);
        category.setDescription("Auto-created from email parsing");
        category.setColorCode("#667eea");
        return categoryRepository.save(category);
    }

    // Helper class to store email patterns
    private static class EmailPattern {
        String fromPattern;
        String subjectPattern;
        String amountPattern;
        String defaultCategory;

        EmailPattern(String fromPattern, String subjectPattern, String amountPattern, String defaultCategory) {
            this.fromPattern = fromPattern;
            this.subjectPattern = subjectPattern;
            this.amountPattern = amountPattern;
            this.defaultCategory = defaultCategory;
        }
    }
}
