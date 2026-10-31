package com.ifride.core.service.admin;

import com.ifride.core.admin.AdminValidator;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminValidatorTest {

    private AdminValidator validator;
    private User author;

    @BeforeEach
    void setUp() {
        validator = new AdminValidator();

        author = new User();
        author.setId("admin-123");
    }

    @Nested
    @DisplayName("Validação de Auto-Exclusão")
    class SelfDeletionValidation {

        @Test
        @DisplayName("Deve permitir a exclusão quando o ID do alvo for diferente do autor")
        void shouldAllowDeletionWhenTargetIsDifferent() {
            String targetAdminId = "admin-456";

            assertThatCode(() -> validator.validateNotSelfDeletion(targetAdminId, author))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar ForbiddenException ao tentar deletar o próprio usuário")
        void shouldThrowForbiddenWhenDeletingSelf() {
            String targetAdminId = "admin-123";

            assertThatThrownBy(() -> validator.validateNotSelfDeletion(targetAdminId, author))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Você não pode deletar seu próprio usuário");
        }
    }
}