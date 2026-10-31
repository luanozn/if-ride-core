package com.ifride.core.admin;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminValidator {

    public void validateNotSelfDeletion(String administratorId, User author) {
        if(administratorId.equals(author.getId())) {
            throw new ForbiddenException("Você não pode deletar seu próprio usuário por esse fluxo, para isso, utilize a deleção de conta por dentro do aplicativo!");
        }
    }
}
