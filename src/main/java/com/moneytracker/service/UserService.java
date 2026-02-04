package com.moneytracker.service;

import com.moneytracker.dto.EmailConfigDTO;
import com.moneytracker.exception.ResourceNotFoundException;
import com.moneytracker.model.User;
import com.moneytracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Long getUserIdByUsername(String username) {
        return getUserByUsername(username).getId();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateEmailParsingConfig(Long userId, EmailConfigDTO config) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (config.getEmailParsingEnabled() != null)
            user.setEmailParsingEnabled(config.getEmailParsingEnabled());

        if (config.getEmailImapHost() != null)
            user.setEmailImapHost(config.getEmailImapHost());

        if (config.getEmailImapUsername() != null)
            user.setEmailImapUsername(config.getEmailImapUsername());

        if (config.getEmailImapPassword() != null)
            user.setEmailImapPassword(config.getEmailImapPassword()); // encrypt later

        if (config.getEmailImapPort() != null)
            user.setEmailImapPort(config.getEmailImapPort());

        return userRepository.save(user);
    }

    public EmailConfigDTO getEmailParsingConfig(Long userId) {

        User user = getUserById(userId);

        return new EmailConfigDTO(
                user.getEmailImapHost(),
                user.getEmailImapUsername(),
                null, // NEVER send password back
                user.getEmailParsingEnabled(),
                user.getEmailImapPort()
        );
    }
}
