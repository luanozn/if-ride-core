package com.ifride.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/user/auth")
    public String auth() {
        return "Hello World";
    }
}
