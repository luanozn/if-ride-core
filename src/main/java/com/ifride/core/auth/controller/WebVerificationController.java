package com.ifride.core.auth.controller;

import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Log4j2
@Controller
@RequestMapping("/v1/auth")
@AllArgsConstructor
public class WebVerificationController {

    private final EmailVerificationTokenService emailVerificationTokenService;
    private final UserService userService;

    @PostMapping(
            value = "/v1/auth/resend-verification-form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<String> resendVerificationForm(@RequestParam String email) throws IOException {
        try {
            var user = userService.findByEmail(email);
            emailVerificationTokenService.generateTokenAndSendEmail(user);
        } catch (Exception e) {
            log.warn("Falha em reenvio via form para {}: {}", email, e.getMessage());
        }

        var html = new ClassPathResource("auth/verification-resend.html")
                .getContentAsString(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

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
