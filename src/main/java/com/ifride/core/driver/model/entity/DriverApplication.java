package com.ifride.core.driver.model.entity;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "driver_applications")
@Getter
@Setter
@SQLDelete(sql = "UPDATE driver_applications SET status = 'DELETED' WHERE id = ? AND version = ?")
@SQLRestriction("status <> 'DELETED'")
public class DriverApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false)
    private DriverApplicationStatus applicationStatus = DriverApplicationStatus.PENDING;

    @Column(name = "cnh_number", nullable = false)
    private String cnhNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnh_category", nullable = false)
    private CnhCategory cnhCategory;

    @Column(name = "cnh_expiration", nullable = false)
    private LocalDate cnhExpiration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

}