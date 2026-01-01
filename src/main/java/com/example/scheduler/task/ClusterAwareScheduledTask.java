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
    private final com.example.scheduler.service.JobHistoryService jobHistoryService;

    public ClusterAwareScheduledTask(com.example.scheduler.service.JobHistoryService jobHistoryService) {
        this.jobHistoryService = jobHistoryService;
    }

    // Runs every minute
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "ClusterAwareScheduledTask_run", lockAtLeastFor = "15s", lockAtMostFor = "50s")
    public void run() {
        var log = jobHistoryService.recordStart("OneMinuteTask");
        logger.info("Executing scheduled task on this member at {}", LocalDateTime.now());

        try {
            // Simulate job logic
            Thread.sleep(1000);
            jobHistoryService.recordSuccess(log.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobHistoryService.recordFailure(log.id(), "Interrupted");
        } catch (Exception e) {
            jobHistoryService.recordFailure(log.id(), e.getMessage());
        }

        logger.info("Scheduled task finished.");
    }

    // Runs every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "ClusterAwareScheduledTask_runFiveMinuteTask", lockAtLeastFor = "1m", lockAtMostFor = "4m")
    public void runFiveMinuteTask() {
        var log = jobHistoryService.recordStart("FiveMinuteTask");
        logger.info("Executing 5-minute scheduled task on this member at {}", LocalDateTime.now());

        try {
            // Simulate heavier job logic
            Thread.sleep(2000);
            jobHistoryService.recordSuccess(log.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobHistoryService.recordFailure(log.id(), "Interrupted");
        } catch (Exception e) {
            jobHistoryService.recordFailure(log.id(), e.getMessage());
        }

        logger.info("5-minute scheduled task finished.");
    }
}
