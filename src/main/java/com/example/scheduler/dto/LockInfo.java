package com.example.scheduler.dto;

import java.time.LocalDateTime;

public record LockInfo(String name,LocalDateTime lockUntil,LocalDateTime lockedAt,String lockedBy){}
