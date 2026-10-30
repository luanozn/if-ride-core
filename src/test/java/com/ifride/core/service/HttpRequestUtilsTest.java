package com.ifride.core.service;

import com.ifride.core.shared.utils.HttpRequestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestUtilsTest {

    @Nested
    @DisplayName("Extração de IP para Auditoria")
    class IpExtraction {

        @Test
        @DisplayName("Deve retornar o IP direto quando não houver X-Forwarded-For")
        void shouldReturnRemoteAddrWhenHeaderIsAbsent() {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.10");

            String ip = HttpRequestUtils.getClientIp(request);

            assertThat(ip).isEqualTo("192.168.1.10");
        }

        @Test
        @DisplayName("Deve converter IPv6 localhost para IPv4 (127.0.0.1)")
        void shouldConvertLocalIpv6ToIpv4() {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("0:0:0:0:0:0:0:1");

            String ip = HttpRequestUtils.getClientIp(request);

            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("Deve extrair o primeiro IP quando estiver atrás de um proxy (AWS/Gateway)")
        void shouldExtractFirstIpFromForwardedHeader() {
            var request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "187.45.12.10, 10.0.0.1, 10.0.0.2");

            String ip = HttpRequestUtils.getClientIp(request);

            assertThat(ip).isEqualTo("187.45.12.10");
        }
    }
}