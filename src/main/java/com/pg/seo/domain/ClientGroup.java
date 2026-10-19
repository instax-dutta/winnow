package com.pg.seo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A named group of clients. The code is unique across the platform. */
@Getter
@Setter
@Entity
@Table(name = "client_group")
public class ClientGroup extends TenantOwnedEntity {

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
