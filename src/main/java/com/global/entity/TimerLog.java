package com.global.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "timer_logs")
@NamedQueries({
        @NamedQuery(name = "TimerLog.findAll",
                query = "SELECT t FROM TimerLog t ORDER BY t.executedAt DESC"),
        @NamedQuery(name = "TimerLog.findByType",
                query = "SELECT t FROM TimerLog t WHERE t.timerType = :timerType ORDER BY t.executedAt DESC"),
        @NamedQuery(name = "TimerLog.findRecent",
                query = "SELECT t FROM TimerLog t WHERE t.executedAt >= :since ORDER BY t.executedAt DESC")
})
public class TimerLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timer_type", nullable = false, length = 100)
    private String timerType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "success", nullable = false)
    private boolean success = true;

    @Column(name = "records_processed")
    private int recordsProcessed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.executedAt == null) {
            this.executedAt = LocalDateTime.now();
        }
    }

    public TimerLog() {
        this.executedAt = LocalDateTime.now();
    }

    public TimerLog(String timerType, String description) {
        this.timerType = timerType;
        this.description = description;
        this.executedAt = LocalDateTime.now();
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTimerType() { return timerType; }
    public void setTimerType(String timerType) { this.timerType = timerType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(int recordsProcessed) { this.recordsProcessed = recordsProcessed; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
