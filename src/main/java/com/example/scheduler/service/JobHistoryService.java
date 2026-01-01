package com.example.scheduler.service;

import com.example.scheduler.entity.JobHistory;
import com.example.scheduler.repository.JobHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class JobHistoryService {

    private final JobHistoryRepository jobHistoryRepository;

    public JobHistoryService(JobHistoryRepository jobHistoryRepository) {
        this.jobHistoryRepository = jobHistoryRepository;
    }

    @Transactional
    public JobHistory recordStart(String jobName) {
        JobHistory job = new JobHistory(
                null,
                jobName,
                LocalDateTime.now(),
                null,
                "RUNNING",
                System.getenv("HOSTNAME"), // Kubernetes Pod Name
                null);
        return jobHistoryRepository.save(job);
    }

    @Transactional
    public void recordSuccess(Integer id) {
        jobHistoryRepository.findById(id).ifPresent(job -> {
            JobHistory updated = new JobHistory(
                    job.id(),
                    job.jobName(),
                    job.startTime(),
                    LocalDateTime.now(),
                    "SUCCESS",
                    job.executedBy(),
                    null);
            jobHistoryRepository.save(updated);
        });
    }

    @Transactional
    public void recordFailure(Integer id, String errorMessage) {
        jobHistoryRepository.findById(id).ifPresent(job -> {
            JobHistory updated = new JobHistory(
                    job.id(),
                    job.jobName(),
                    job.startTime(),
                    LocalDateTime.now(),
                    "FAILURE",
                    job.executedBy(),
                    errorMessage);
            jobHistoryRepository.save(updated);
        });
    }
}
