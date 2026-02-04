package com.moneytracker.model;

public enum NotificationType {
    BUDGET_ALERT("Budget Alert"),
    BUDGET_EXCEEDED("Budget Exceeded"),
    TRANSACTION_ADDED("Transaction Added"),
    MONTHLY_SUMMARY("Monthly Summary"),
    EMAIL_PARSED("Email Parsed Transaction"),
    SYSTEM("System Notification");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
