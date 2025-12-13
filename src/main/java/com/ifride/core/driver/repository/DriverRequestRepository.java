package com.ifride.core.driver.repository;

import com.ifride.core.driver.model.entity.DriverRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRequestRepository extends JpaRepository<DriverRequest, String> {
}
