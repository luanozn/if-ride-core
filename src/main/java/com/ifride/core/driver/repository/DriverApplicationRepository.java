package com.ifride.core.driver.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, String> {

    List<DriverApplication> findAllByRequesterOrderByCreatedAtDesc(User requester);

    @Query("""
        SELECT app FROM DriverApplication app
        JOIN FETCH app.requester
        WHERE (:email IS NULL OR app.requester.email LIKE CONCAT(:email, '%'))
          AND (:name IS NULL OR app.requester.name LIKE CONCAT(:name, '%'))
          AND (COALESCE(:statuses, NULL) IS NULL OR app.status IN :statuses)
    """)
    Page<DriverApplication> findApplications(
            @Param("statuses") List<DriverApplicationStatus> statuses,
            @Param("email") String email,
            @Param("name") String name,
            Pageable pageable
    );
}
