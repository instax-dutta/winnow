/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.ClientGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientGroupRepository
extends JpaRepository<ClientGroup, Long> {
    public List<ClientGroup> findAll();

    public List<ClientGroup> findByLoginId(Long var1);

    public ClientGroup findByLoginIdAndGroupCode(Long var1, String var2);

    public ClientGroup findByGroupCode(String var1);
}

