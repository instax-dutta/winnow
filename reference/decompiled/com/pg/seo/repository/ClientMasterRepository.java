/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.stereotype.Repository
 */
package com.pg.seo.repository;

import com.pg.seo.entity.ClientMaster;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientMasterRepository
extends JpaRepository<ClientMaster, Long> {
    public List<ClientMaster> findByLoginId(Long var1);

    @Query(nativeQuery=true, value="select * from client_master c where c.groupCode=:groupCode and c.loginId=:loginId and c.isactive=1")
    public List<ClientMaster> findByGroupCodeAndLoginId(String var1, Long var2);

    @Query(nativeQuery=true, value="select * from client_master c where c.groupCode=:groupCode and c.isactive=1")
    public List<ClientMaster> findByGroupCode(String var1);

    public ClientMaster findByClientMasterId(Long var1);

    public Page<ClientMaster> findAllByLoginId(Long var1, Pageable var2);
}

