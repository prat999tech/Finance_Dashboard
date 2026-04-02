package com.finance.dashboard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Finance Dashboard API",
                version = "1.0",
                description = """
                        Role-based finance dashboard backend.

                        **Roles:**
                        - ADMIN — full access (users, transactions, dashboard)
                        - ANALYST — read transactions + dashboard analytics
                        - VIEWER — read transactions only

                        **Auth flow:** POST /api/auth/login → copy token → click Authorize → paste as Bearer <token>
                        """,
                contact = @Contact(name = "Finance Dashboard")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
