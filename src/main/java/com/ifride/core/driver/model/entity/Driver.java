package com.ifride.core.driver.model.entity;

import com.ifride.core.auth.model.entity.User;

import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.shared.model.AuditEntity;
import com.ifride.core.shared.model.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@SQLDelete(sql = "UPDATE drivers SET status = 'DELETED' WHERE id = ? AND version = ?")
@SQLRestriction("status <> 'DELETED'")
@ToString(callSuper = true)
public class Driver extends AuditEntity {

    @Id
    @Column(name = "id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id", nullable = false)
    private User user;

    @Column(name = "cnh_number", nullable = false)
    private String cnhNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnh_category", nullable = false)
    private CnhCategory cnhCategory;

    @Column(name = "cnh_expiration", nullable = false)
    private LocalDate cnhExpiration;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Version
    private Long version;
}
