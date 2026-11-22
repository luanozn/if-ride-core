package com.ifride.core.auth.model.dto;

import com.ifride.core.auth.model.entity.ActionToken;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ActionTokenWrapper {
    private ActionToken token;
    private String plainToken;
}
