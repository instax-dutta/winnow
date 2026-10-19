/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailMaster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailMasterRepository
extends JpaRepository<EmailMaster, Long> {
    public EmailMaster findByEmail(String var1);

    public List<EmailMaster> findByLoginId(Long var1);

    public EmailMaster findByEmailMasterId(Long var1);
}

