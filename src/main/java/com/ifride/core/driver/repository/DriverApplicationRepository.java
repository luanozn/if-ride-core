package com.ifride.core.driver.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, String>, JpaSpecificationExecutor<DriverApplication> {

    List<DriverApplication> findAllByRequesterOrderByCreatedAtDesc(User requester);
}
