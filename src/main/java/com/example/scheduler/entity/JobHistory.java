package com.example.scheduler.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("job_history")
public record JobHistory(
        @Id Integer id,
        String jobName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String executedBy,
        String errorMessage) {
}
