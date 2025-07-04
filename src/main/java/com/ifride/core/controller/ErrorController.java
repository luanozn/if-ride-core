package com.ifride.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {

    @GetMapping("/error")
    public String errorController() {
        return "Something went wrong, please, try again later.";
    }
}
