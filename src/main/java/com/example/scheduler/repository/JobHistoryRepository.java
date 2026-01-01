package com.example.scheduler.repository;

import com.example.scheduler.entity.JobHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobHistoryRepository extends CrudRepository<JobHistory, Integer> {
}
