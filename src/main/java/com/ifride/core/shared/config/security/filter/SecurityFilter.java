package com.ifride.core.shared.config.security.filter;

import com.ifride.core.shared.exceptions.api.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.JwtService;

import lombok.AllArgsConstructor;

import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository repository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v1/auth") || path.startsWith("/ws") || path.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var jwt = retrieveToken(request);

        if (jwt != null) {
            try {
                var subject = jwtService.validateToken(jwt);
                var user = repository.findByEmail(subject);

                if (user.isPresent()) {
                    boolean verified = user.get().isEmailVerified();
                    boolean pathAllowed = isAllowedForUnverified(request.getServletPath());

                    if (verified || pathAllowed) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.get(),
                                null,
                                user.get().getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(
                                "{\"message\":\"Verifique seu e-mail antes de acessar este recurso.\",\"statusCode\":403}"
                        );
                        return;
                    }
                }
            } catch (ForbiddenException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"" + e.getMessage() + "\",\"statusCode\":401}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String retrieveToken(HttpServletRequest request) {
        var token = request.getHeader("Authorization");

        return token == null ?
                null :
                token.replace("Bearer ", "");
    }

    private boolean isAllowedForUnverified(String path) {
        return path.startsWith("/v1/users/me")
                || path.startsWith("/v1/ride-participant/me")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/auth/");
    }
}
