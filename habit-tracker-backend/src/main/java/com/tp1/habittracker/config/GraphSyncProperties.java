package com.tp1.habittracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.graph.sync")
public class GraphSyncProperties {

    /** Poll interval in milliseconds for the outbox processor. */
    private long pollMs = 2000;

    /** Maximum number of pending events processed per tick. */
    private int batchSize = 50;

    /** After this many failed attempts an event is left alone (still inspectable via SQL). */
    private int maxAttempts = 10;

    public long getPollMs() {
        return pollMs;
    }

    public void setPollMs(long pollMs) {
        this.pollMs = pollMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
