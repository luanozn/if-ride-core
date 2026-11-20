package com.ifride.core.auth.utils;

import static com.ifride.core.shared.utils.CpfViewConverter.convertFormatted;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.ride.service.validators.RideParticipantValidator;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.PreconditionFailedException;
import com.ifride.core.shared.model.enums.Status;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.utils.CpfViewConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final CPFValidator cpfValidator = new CPFValidator();
    private final UserRepository repository;
    private final DeterministicEncryptorService cryptoService;
    private final RideService rideService;
    private final RideParticipantValidator rideParticipantValidator;

    public void validateDeletion(User user) {
        validateNotLastAdmin(user);
        validateDriverRides(user);
        validateUserParticipations(user);
    }

    public void validateUser(RegisterRequestDTO registerRequest) {
        validateDocument(registerRequest);
        validateUnicity(registerRequest);
    }

    private void validateDocument(RegisterRequestDTO registerRequest) {
        try {
            cpfValidator.assertValid(convertFormatted(registerRequest.documentNumber()));
        } catch (InvalidStateException e) {
            throw new BadRequestException("O CPF inserido é inválido.");
        }
    }

    private void validateUnicity(RegisterRequestDTO registerRequest) {
        var encryptedCpf = cryptoService.encrypt(convertFormatted(registerRequest.documentNumber()));

        if (repository.existsUserByEmail(registerRequest.email())) {
            throw new ConflictException("Não é possível cadastrar o usuário. O email já está em uso!", registerRequest.email());
        } else if (repository.existsUserByCpfEncrypted(encryptedCpf)) {
            throw new ConflictException("Não é possível cadastrar o usuário. O CPF já está cadastrado!", CpfViewConverter.convert(registerRequest.documentNumber()));
        }
    }

    private void validateNotLastAdmin(User user) {
        if (user.has(Role.ADMIN)) {
            var activeAdmins = repository.findAllByRoleAndStatusForUpdate(Role.ADMIN, Status.ACTIVE);

            if (activeAdmins.size() <= 1) {
                throw new ForbiddenException("Não é possível realizar esta operação com o último administrador ativo do sistema. Cadastre outro administrador antes de prosseguir.");
            }
        }
    }

    private void validateDriverRides(User user) {
        if(rideService.driverHasValidUpcomingRides(user.getId())) {
            throw new PreconditionFailedException("Não é possível realizar a ação para usuários com caronas programadas.");
        }
    }

    private void validateUserParticipations(User user) {
        rideParticipantValidator.validateUserDeletion(user);
    }
}
