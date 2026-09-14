package com.ifride.core.auth.utils;

import static com.ifride.core.shared.utils.CpfViewConverter.convertFormatted;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.repository.UserRepository;
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

    public void validateDocument(RegisterRequestDTO registerRequest) {
        validateValidity(registerRequest);
        validateUnicity(registerRequest);
    }

    private void validateValidity(RegisterRequestDTO registerRequest) {
        try {
            cpfValidator.assertValid(convertFormatted(registerRequest.documentNumber()));
        } catch (InvalidStateException e) {
            throw new BadRequestException("O CPF inserido é inválido.");
        }
    }

    private void validateUnicity(RegisterRequestDTO registerRequest) {
        if (repository.existsUserByEmail(registerRequest.email())) {
            throw new ConflictException("Não é possível cadastrar o usuário. O email %s já está cadastrado!", registerRequest.email());
        } else if (repository.existsUserByCpf(convertFormatted(registerRequest.documentNumber()))) {
            throw new ConflictException("Não é possível cadastrar o usuário. O CPF %s já está cadastrado", CpfViewConverter.convert(registerRequest.documentNumber()));
        }
    }
}
