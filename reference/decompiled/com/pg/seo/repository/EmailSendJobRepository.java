/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailSendJob;
import java.sql.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailSendJobRepository
extends JpaRepository<EmailSendJob, Long> {
    public List<EmailSendJob> findByDateAndLoginId(Date var1, Long var2);

    @Query(nativeQuery=true, value="select * FROM email_send_job d WHERE d.date between :startDate and :endDate  and loginId =:loginId")
    public List<EmailSendJob> findByLoginIdMonth(Long var1, Date var2, Date var3);
}

