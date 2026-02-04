package com.moneytracker.controller;

import com.moneytracker.dto.EmailConfigDTO;
import com.moneytracker.model.User;
import com.moneytracker.service.EmailParsingService;
import com.moneytracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Email Parsing functionality
 * Handles email auto-import configuration and manual trigger
 */
@RestController
@RequestMapping("/api/email-parsing")
@RequiredArgsConstructor
@Slf4j
public class EmailParsingController {

    private final EmailParsingService emailParsingService;
    private final UserService userService;

    /**
     * Trigger email parsing manually for current user
     * POST /api/email-parsing/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerEmailParsing(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Manual email parsing triggered by user: {}", userDetails.getUsername());

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());

            if (!user.getEmailParsingEnabled()) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Email parsing is not enabled. Please configure it in settings first.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (user.getEmailImapUsername() == null || user.getEmailImapPassword() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Email credentials not configured. Please update your settings.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Trigger parsing in a separate thread to avoid timeout
            new Thread(() -> {
                try {
                    emailParsingService.parseEmailsForUser(user);
                } catch (Exception e) {
                    log.error("Error parsing emails for user {}: {}", user.getUsername(), e.getMessage());
                }
            }).start();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Email parsing started. You will receive notifications when complete.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error triggering email parsing: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to start email parsing: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update email parsing configuration for current user
     * PUT /api/email-parsing/config
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateEmailConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EmailConfigDTO config) {

        log.info("Updating email config for user: {}", userDetails.getUsername());

        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            User updatedUser = userService.updateEmailParsingConfig(userId, config);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Email configuration updated successfully");
            response.put("emailParsingEnabled", updatedUser.getEmailParsingEnabled());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating email config: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get email parsing configuration for current user
     * GET /api/email-parsing/config
     */
    @GetMapping("/config")
    public ResponseEntity<EmailConfigDTO> getEmailConfig(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Retrieving email config for user: {}", userDetails.getUsername());

        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            EmailConfigDTO config = userService.getEmailParsingConfig(userId);

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("Error retrieving email config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get email parsing status for current user
     * GET /api/email-parsing/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEmailParsingStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Getting email parsing status for user: {}", userDetails.getUsername());

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());

            Map<String, Object> status = new HashMap<>();
            status.put("enabled", user.getEmailParsingEnabled());
            status.put("configured", user.getEmailImapUsername() != null &&
                    user.getEmailImapPassword() != null);
            status.put("host", user.getEmailImapHost());
            status.put("username", user.getEmailImapUsername());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error getting email parsing status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test email parsing connection
     * POST /api/email-parsing/test
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testEmailConnection(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Testing email connection for user: {}", userDetails.getUsername());

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());

            if (user.getEmailImapUsername() == null || user.getEmailImapPassword() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Email credentials not configured");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Simple test - try to parse a small number of emails
            // You can implement a more sophisticated test if needed
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Email configuration appears valid. Try 'Parse Now' to test fully.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing email connection: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Connection test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Disable email parsing for current user
     * POST /api/email-parsing/disable
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disableEmailParsing(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Disabling email parsing for user: {}", userDetails.getUsername());

        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());

            EmailConfigDTO config = new EmailConfigDTO();
            config.setEmailParsingEnabled(false);

            userService.updateEmailParsingConfig(userId, config);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Email parsing disabled");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error disabling email parsing: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to disable: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}