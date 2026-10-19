/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailExtractorData;
import java.sql.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailExtractorDataRepository
extends JpaRepository<EmailExtractorData, Long> {
    public List<EmailExtractorData> findByEmailExtractorId(Long var1);

    public List<EmailExtractorData> findByStartDateBetween(Date var1, Date var2);
}

