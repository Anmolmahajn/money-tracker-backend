package com.moneytracker.model;

public enum PaymentMethod {
    UPI("UPI Apps"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    WALLET("Digital Wallet"),
    CASH("Cash"),
    NET_BANKING("Net Banking"),
    SUBSCRIPTION("Subscription");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
