package com.ifride.core.driver.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRequestRepository extends JpaRepository<DriverRequest, String> {

    List<DriverRequest> findAllByRequesterOrderByCreatedAtDesc(User requester);
}
