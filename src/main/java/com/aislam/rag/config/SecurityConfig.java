package com.aislam.rag.config;

import com.aislam.rag.security.JwtAuthenticationFilter;
import com.aislam.rag.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // STATELESS + SseEmitter: session yok; async dispatch için context request attribute'ta tutulmalı.
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new RequestAttributeSecurityContextRepository())
                )
                .exceptionHandling(handler -> handler.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Token alma + sağlık
                        .requestMatchers("/api/auth/**", "/api/health/**").permitAll()
                        // Controller içinde secret doğrulanan admin / webhook uçları
                        .requestMatchers("/api/webhooks/revenuecat").permitAll()
                        // AdMob SSV callback (Google sunucuları; imza controller’da)
                        .requestMatchers(HttpMethod.GET, "/api/ads/ssv/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/send", "/api/notifications/test")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/config/ads").permitAll()
                        .requestMatchers("/api/documents/**").permitAll()
                        // Hesap profili — sadece kullanıcı JWT
                        .requestMatchers("/api/users/**").hasRole("USER")
                        // Geri kalan tüm API — cihaz veya kullanıcı JWT
                        .anyRequest().hasAnyRole("USER", "DEVICE")
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
