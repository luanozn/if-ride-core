package com.ifride.core.driver.model.enums;

public enum CnhCategory {

    A,
    B,
    C,
    D,
    E,
    AB,
    AC,
    AD,
    AE;

    public boolean canDriveCar() {
        return this != CnhCategory.A;
    }

    public boolean canDriveMotorcycle() {
        return this.name().contains("A");
    }
}