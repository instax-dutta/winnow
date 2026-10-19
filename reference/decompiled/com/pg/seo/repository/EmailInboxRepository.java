/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailInbox;
import java.sql.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailInboxRepository
extends JpaRepository<EmailInbox, Long> {
    public List<EmailInbox> findByEmailId(String var1);

    public void deleteByEmailIdAndDate(String var1, Date var2);
}

