package com.ifride.core.auth.model.mapper;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.utils.CpfViewConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.ifride.core.shared.utils.CpfViewConverter.convertFormatted;

@Component
public class UserMapper {

    private final DeterministicEncryptorService crypto;
    private final PasswordEncoder encoder;

    public UserMapper(DeterministicEncryptorService cpfCrypto, PasswordEncoder encoder) {
        this.crypto = cpfCrypto;
        this.encoder = encoder;
    }

    public User userFromRegisterRequest(RegisterRequestDTO registerRequestDTO, Role role) {
        User user = new User();

        user.setEmail(registerRequestDTO.email());
        user.setPassword(encoder.encode(registerRequestDTO.password()));
        user.setName(registerRequestDTO.name());
        user.setBirthDate(registerRequestDTO.birthDate());
        user.setCpfEncrypted(crypto.encrypt(convertFormatted(registerRequestDTO.documentNumber())));
        user.setRole(role);

        return user;
    }

    public UserDto toListItem(User target) {
        if (target == null) return null;
        return toSummary(target, true);
    }

    public UserDto toDto(User target, User requester) {
        if (target == null) return null;
        if (canSeeFull(target, requester)) return toFull(target);
        return toSummary(target, false);
    }

    private boolean canSeeFull(User target, User requester) {
        return requester != null
                && (requester.has(Role.ADMIN) || requester.getId().equals(target.getId()));
    }

    private UserDto toFull(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                CpfViewConverter.convert(crypto.decrypt(user.getCpfEncrypted())),
                user.getRole(),
                user.getBirthDate(),
                user.isEmailVerified()
        );
    }

    private UserDto toSummary(User user, boolean withEmail) {
        return new UserDto(
                user.getId(),
                user.getName(),
                withEmail ? user.getEmail() : null,
                null,
                user.getRole(),
                null,
                user.isEmailVerified()
        );
    }
}