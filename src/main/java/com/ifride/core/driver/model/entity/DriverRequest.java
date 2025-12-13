package com.ifride.core.driver.model.entity;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverRequestStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "driver_requests")
@Getter
@Setter
public class DriverRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverRequestStatus status = DriverRequestStatus.PENDING;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

}