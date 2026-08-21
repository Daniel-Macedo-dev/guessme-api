package com.guessme.guessme.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GameSession {

    private String conversationHistory = "";
    private String currentCategory = "Geral";
    private volatile Instant lastAccess = Instant.now();

    private final AtomicInteger questionCount = new AtomicInteger(0);
    private final AtomicInteger hintCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastRequestAt = new AtomicReference<>(null);

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

    public int getQuestionCount() {
        return questionCount.get();
    }

    public int incrementQuestionCount() {
        return questionCount.incrementAndGet();
    }

    public boolean tryReserveQuestion(int maxQuestions) {
        return tryReserve(questionCount, maxQuestions);
    }

    public void releaseQuestion() {
        questionCount.updateAndGet(current -> Math.max(0, current - 1));
    }

    public int getHintCount() {
        return hintCount.get();
    }

    public int incrementHintCount() {
        return hintCount.incrementAndGet();
    }

    public boolean tryReserveHint(int maxHints) {
        return tryReserve(hintCount, maxHints);
    }

    public void releaseHint() {
        hintCount.updateAndGet(current -> Math.max(0, current - 1));
    }

    private boolean tryReserve(AtomicInteger counter, int maximum) {
        while (true) {
            int current = counter.get();
            if (current >= maximum) return false;
            if (counter.compareAndSet(current, current + 1)) return true;
        }
    }

    /**
     * CAS-based cooldown gate. Returns true if the caller is allowed to proceed
     * (either no previous request or enough time has elapsed), false if still in cooldown.
     * Thread-safe without external locking.
     */
    public boolean tryAcquire(long cooldownMs) {
        if (cooldownMs <= 0) return true;
        Instant now = Instant.now();
        while (true) {
            Instant last = lastRequestAt.get();
            if (last != null && ChronoUnit.MILLIS.between(last, now) < cooldownMs) {
                return false;
            }
            if (lastRequestAt.compareAndSet(last, now)) {
                return true;
            }
        }
    }
}
