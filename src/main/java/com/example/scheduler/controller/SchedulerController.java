package com.example.scheduler.controller;

import com.example.scheduler.dto.LockInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final JdbcTemplate jdbcTemplate;

    private final com.example.scheduler.repository.JobHistoryRepository jobHistoryRepository;

    public SchedulerController(JdbcTemplate jdbcTemplate,
            com.example.scheduler.repository.JobHistoryRepository jobHistoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobHistoryRepository = jobHistoryRepository;
    }

    @GetMapping("/locks")
    public List<LockInfo> getLocks() {
        return jdbcTemplate.query("SELECT * FROM shedlock", (rs, rowNum) -> new LockInfo(
                rs.getString("name"),
                toLocalDateTime(rs.getTimestamp("lock_until")),
                toLocalDateTime(rs.getTimestamp("locked_at")),
                rs.getString("locked_by")));
    }

    @GetMapping("/history")
    public Iterable<com.example.scheduler.entity.JobHistory> getHistory() {
        return jobHistoryRepository.findAll();
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
