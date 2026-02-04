package com.moneytracker.model;

public enum TransactionSource {
    MANUAL("Manual Entry"),
    EMAIL_PARSED("Email Parsed"),
    SMS_PARSED("SMS Parsed"),
    CSV_IMPORT("CSV Import"),
    API_INTEGRATION("API Integration");
    
    private final String displayName;
    
    TransactionSource(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
