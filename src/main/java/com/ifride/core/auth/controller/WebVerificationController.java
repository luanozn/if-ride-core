package com.ifride.core.auth.controller;

import com.ifride.core.auth.service.EmailVerificationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/v1/auth")
@AllArgsConstructor
public class WebVerificationController {

    private final EmailVerificationTokenService emailVerificationTokenService;


    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            emailVerificationTokenService.confirmEmailVerification(token);
            return "redirect:/auth/verification-success.html";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("errorMessage", e.getMessage());

            return "redirect:/auth/verification-error.html";
        }
    }
}
