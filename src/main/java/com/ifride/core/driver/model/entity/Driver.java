package com.ifride.core.driver.model.entity;

import com.ifride.core.auth.model.entity.User;

import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.shared.model.AuditEntity;
import com.ifride.core.shared.model.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@SQLDelete(sql = "UPDATE drivers SET status = 'DELETED' WHERE id = ? AND version = ?")
@SQLRestriction("status <> 'DELETED'")
public class Driver extends AuditEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cnh_number", nullable = false)
    private String cnhNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnh_category", nullable = false)
    private CnhCategory cnhCategory;

    @Column(name = "cnh_expiration", nullable = false)
    private LocalDateTime cnhExpiration;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
}
