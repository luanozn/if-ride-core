package com.ifride.core.driver.model.entity;

import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vehicles")
@SQLDelete(sql = "UPDATE vehicles SET status = 'DELETED' WHERE id = ? AND version = ?")
@SQLRestriction("status <> 'DELETED'")
@Data
@EqualsAndHashCode(callSuper = true)
public class Vehicle extends BaseEntity {

    private String model;
    private String plate;
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver owner;
}
