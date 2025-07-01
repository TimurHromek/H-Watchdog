// FILE: src/main/java/eu/hindustries/timur/hwatchdog/model/OPSession.java
package eu.hindustries.timur.hwatchdog.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OPSession {
    private final UUID playerUUID;
    private String lastReason;
    private long reasonSetTimestamp;
    private final List<Long> violationTimestamps = new ArrayList<>();

    public OPSession(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setReason(String reason) {
        this.lastReason = reason;
        this.reasonSetTimestamp = System.currentTimeMillis();
    }

    public String getReason(long expirySeconds) {
        if (lastReason != null && (System.currentTimeMillis() - reasonSetTimestamp) < (expirySeconds * 1000L)) {
            return lastReason;
        }
        return null; // Reason is expired or not set
    }

    public void clearReason() {
        this.lastReason = null;
        this.reasonSetTimestamp = 0;
    }

    public void addViolation() {
        violationTimestamps.add(System.currentTimeMillis());
    }

    public int getRecentViolationCount(int timeWindowSeconds) {
        long cutoff = System.currentTimeMillis() - (timeWindowSeconds * 1000L);
        violationTimestamps.removeIf(timestamp -> timestamp < cutoff);
        return violationTimestamps.size();
    }
}