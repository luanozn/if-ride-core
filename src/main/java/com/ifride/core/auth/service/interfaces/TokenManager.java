package com.ifride.core.auth.service.interfaces;

import com.ifride.core.auth.model.entity.User;

public interface TokenManager {

    void generateTokenAndSendEmail(User user);
    void confirmEmailVerification(String token);
}
