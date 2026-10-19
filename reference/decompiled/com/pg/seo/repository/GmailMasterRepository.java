/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.GmailMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GmailMasterRepository
extends JpaRepository<GmailMaster, Long> {
    public GmailMaster findByEmail(String var1);
}

