package com.ifride.core.driver.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, String> {

    List<DriverApplication> findAllByRequesterOrderByCreatedAtDesc(User requester);
}
