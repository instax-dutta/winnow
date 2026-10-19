/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.EmailTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateRepository
extends JpaRepository<EmailTemplate, Long> {
    public List<EmailTemplate> findByLoginId(Long var1);

    public EmailTemplate findByEmailTemplateId(Long var1);
}

