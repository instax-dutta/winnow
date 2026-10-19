/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailExtractor;
import java.sql.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailExtractorRepository
extends JpaRepository<EmailExtractor, Long> {
    public List<EmailExtractor> findByLoginId(Long var1);

    @Query(nativeQuery=true, value="select * FROM email_extractor d WHERE d.startDate between :startDate and :endDate  and loginId =:loginId")
    public List<EmailExtractor> findByLoginIdMonth(Long var1, Date var2, Date var3);

    public EmailExtractor findByEmailExtractorIdAndLoginId(Long var1, Long var2);

    public EmailExtractor findByEmailExtractorId(Long var1);
}

