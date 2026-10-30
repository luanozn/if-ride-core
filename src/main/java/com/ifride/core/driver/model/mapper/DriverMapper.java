package com.ifride.core.driver.model.mapper;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.driver.model.dto.CnhSecurityNumberDTO;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.PENDING;

@Component
@RequiredArgsConstructor
public class DriverMapper {

    private final UserMapper userMapper;
    private final DeterministicEncryptorService deterministicCrypto;
    private final RandomizedEncryptorService randomizedCrypto;

    public CnhSecurityNumberDTO fromDriverApplication(DriverApplication application) {
        return new CnhSecurityNumberDTO(
                randomizedCrypto.decrypt(application.getCnhSecurityNumberEncrypted())
        );
    }

    public Driver fromApplication(DriverApplication app) {
        Driver driver = new Driver();
        driver.setId(app.getRequester().getId());
        driver.setUser(app.getRequester());
        driver.setCnhNumberEncrypted(app.getCnhNumberEncrypted());
        driver.setCnhCategory(app.getCnhCategory());
        driver.setCnhExpiration(app.getCnhExpiration());

        return driver;
    }

    public Driver fromApplicationRequestAndUser(DriverApplicationRequestDTO app, User user) {
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setCnhNumberEncrypted(deterministicCrypto.encrypt(app.cnhNumber()));
        driver.setCnhCategory(app.cnhCategory());
        driver.setCnhExpiration(app.expiration());

        return driver;
    }

    public DriverApplication fromRequest(DriverApplicationRequestDTO request, User author) {
        var driverApplication = new DriverApplication();
        driverApplication.setRequester(author);
        driverApplication.setCnhNumberEncrypted(deterministicCrypto.encrypt(request.cnhNumber()));
        driverApplication.setCnhCategory(request.cnhCategory());
        driverApplication.setCnhExpiration(request.expiration());
        driverApplication.setApplicationStatus(PENDING);
        driverApplication.setCnhSecurityNumberEncrypted(randomizedCrypto.encrypt(request.cnhSecurityNumber()));

        return driverApplication;
    }

    public DriverApplicationSummaryDTO toListItem(DriverApplication app) {
        var requesterDto = userMapper.toDto(app.getRequester(), null);
        var reviewedByDto = app.getReviewedBy() != null
                ? userMapper.toDto(app.getReviewedBy(), null)
                : null;

        return new DriverApplicationSummaryDTO(
                app.getId(),
                requesterDto,
                app.getApplicationStatus(),
                null,
                null,
                null,
                reviewedByDto,
                app.getRejectionReason()
        );
    }

    public DriverApplicationSummaryDTO toDto(DriverApplication app, User currentUser) {
        var requesterDto = userMapper.toDto(app.getRequester(), currentUser);
        var reviewedByDto = app.getReviewedBy() != null
                ? userMapper.toDto(app.getReviewedBy(), currentUser)
                : null;

        return new DriverApplicationSummaryDTO(
                app.getId(),
                requesterDto,
                app.getApplicationStatus(),
                cnhFor(app, currentUser),
                app.getCnhCategory(),
                app.getCnhExpiration(),
                reviewedByDto,
                app.getRejectionReason()
        );
    }

    private String cnhFor(DriverApplication app, User currentUser) {
        if (currentUser.has(Role.ADMIN)) return deterministicCrypto.decrypt(app.getCnhNumberEncrypted());
        if (currentUser.getId().equals(app.getRequester().getId())) return deterministicCrypto.decrypt(app.getCnhNumberEncrypted());
        return null;
    }
}