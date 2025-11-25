package com.ifride.core.auth.model.entity;

import com.ifride.core.auth.model.enums.DriverRequestStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "driver_requests")
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