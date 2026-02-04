package com.moneytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigDTO {

    @NotBlank(message = "IMAP host is required")
    private String emailImapHost;

    @NotBlank(message = "IMAP username is required")
    @Email(message = "Invalid email format")
    private String emailImapUsername;

    @NotBlank(message = "IMAP password is required")
    private String emailImapPassword;

    private Boolean emailParsingEnabled;

    private Integer emailImapPort;
}