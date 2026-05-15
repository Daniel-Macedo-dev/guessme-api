package com.guessme.guessme.model;

import java.time.Instant;

public class GameSession {

    private String conversationHistory = "";
    private String currentCategory = "Geral";
    private volatile Instant lastAccess = Instant.now();

    public String getConversationHistory() {
        return conversationHistory;
    }

    public void appendHistory(String entry) {
        this.conversationHistory += entry;
        this.lastAccess = Instant.now();
    }

    public void resetHistory(String initial) {
        this.conversationHistory = initial;
        this.lastAccess = Instant.now();
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(String category) {
        this.currentCategory = category;
        this.lastAccess = Instant.now();
    }

    public Instant getLastAccess() {
        return lastAccess;
    }

    public void touch() {
        this.lastAccess = Instant.now();
    }
}
