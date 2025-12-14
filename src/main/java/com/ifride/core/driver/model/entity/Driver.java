package com.ifride.core.driver.model.entity;

import com.ifride.core.auth.model.entity.User;

import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver")
@Getter
@Setter
public class Driver extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cnh_number", nullable = false)
    private String cnhNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnh_category", nullable = false)
    private CnhCategory cnhCategory;

    @Column(name = "cnh_expiration", nullable = false)
    private LocalDateTime cnhExpiration;

}
