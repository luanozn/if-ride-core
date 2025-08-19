package com.ifride.core.auth.service.interfaces;

import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;

public interface TokenManager {

    ActionToken generateToken(User user);
    void confirmEmailVerification(String token);
}
