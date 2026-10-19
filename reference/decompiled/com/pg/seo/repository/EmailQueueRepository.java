/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailQueue;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailQueueRepository
extends JpaRepository<EmailQueue, Long> {
    public List<EmailQueue> findByStatusAndScheduledTimeBefore(String var1, LocalDateTime var2);

    public List<EmailQueue> findByFromEmailAndScheduledTimeBetweenOrderByScheduledTimeDesc(String var1, LocalDateTime var2, LocalDateTime var3);
}

