package com.ifride.core.shared.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GatewayOriginFilter extends OncePerRequestFilter {

    @Value("${app.gateway.origin}")
    private String expectedOrigin;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String originHeader = request.getHeader("X-Origin");
        if (originHeader == null || !originHeader.equals(expectedOrigin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Direct access not allowed\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}