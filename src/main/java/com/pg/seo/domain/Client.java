package com.pg.seo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A contact that mail can be sent to. */
@Getter
@Setter
@Entity
@Table(name = "client")
public class Client extends TenantOwnedEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "web_link", length = 2048)
    private String webLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ClientGroup group;
}
