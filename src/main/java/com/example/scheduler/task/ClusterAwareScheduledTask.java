package com.example.scheduler.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ClusterAwareScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAwareScheduledTask.class);

    // Runs every minute
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "ClusterAwareScheduledTask_run", lockAtLeastFor = "15s", lockAtMostFor = "50s")
    public void run() {
        logger.info("Executing scheduled task on this member at {}", LocalDateTime.now());

        try {
            // Simulate job logic
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Scheduled task finished.");
    }
}
