package com.ifride.core.driver.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, String>, JpaSpecificationExecutor<DriverApplication> {

    @Query("SELECT app from DriverApplication app WHERE app.requester.id = :userId")
    List<DriverApplication> findByUserId(@Param("userId") String userId);

    List<DriverApplication> findAllByRequesterOrderByCreatedAtDesc(User requester);
    Optional<DriverApplication> findByCnhNumberEncryptedAndApplicationStatusIn(String cnhNumberEncrypted, List<DriverApplicationStatus> statuses);
    boolean existsByRequesterAndApplicationStatus(User requester, DriverApplicationStatus status);
}
