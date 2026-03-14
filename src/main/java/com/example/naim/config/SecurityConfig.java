package com.example.naim.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                // Static files + login page + auth endpoints — সব open
                .requestMatchers(
                    "/",
                    "/login.html",
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    "/favicon.ico", "/webjars/**",
                    "/oauth2/**",           // ← Google redirect
                    "/login/oauth2/**",     // ← callback URL
                    "/api/auth/status",     // ← frontend check
                    "/api/auth/me"          // ← user info
                ).permitAll()
                // বাকি সব API — login লাগবে, কিন্তু HTML redirect না করে 401 দেবে
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login.html")
                .defaultSuccessUrl("/index.html", true)
                .failureUrl("/login.html?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // ← KEY FIX: API call এ HTML redirect না করে 401 return করো
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        // API call → 401 JSON (frontend handle করবে)
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"authenticated\":false}");
                    } else {
                        // Page request → login redirect
                        response.sendRedirect("/login.html");
                    }
                })
            );

        return http.build();
    }
}