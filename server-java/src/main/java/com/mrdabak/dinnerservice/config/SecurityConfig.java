package com.mrdabak.dinnerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("[SecurityConfig] Configuring SecurityFilterChain");
        System.out.println("[SecurityConfig] JWT Filter instance: " + (jwtAuthenticationFilter != null ? "EXISTS" : "NULL"));
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health", "/api/menu/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/employee/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/orders/**").authenticated()  // 인증 필요
                .requestMatchers("/api/reservations/**", "/api/change-requests/**").authenticated()
                .requestMatchers("/static/**", "/*.js", "/*.css", "/*.json", "/*.ico", "/*.png", "/*.jpg", "/*.svg", "/*.woff", "/*.woff2", "/*.ttf", "/*.eot").permitAll()  // 정적 리소스 허용
                .requestMatchers("/", "/login", "/register", "/order", "/orders", "/profile", "/delivery/**", "/admin", "/employee").permitAll()  // 프론트엔드 라우트 허용
                .anyRequest().permitAll()  // 나머지는 모두 허용 (프론트엔드 라우팅)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("========== [Security] Authentication Failed ==========");
                    System.out.println("[Security] Request Path: " + request.getRequestURI());
                    System.out.println("[Security] Request Method: " + request.getMethod());
                    System.out.println("[Security] Authorization Header: " + request.getHeader("Authorization"));
                    System.out.println("[Security] Exception Type: " + authException.getClass().getName());
                    System.out.println("[Security] Exception Message: " + authException.getMessage());
                    System.out.println("[Security] SecurityContext Auth Status: " + 
                        (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ? "AUTHENTICATED" : "NOT AUTHENTICATED"));
                    System.out.println("==========================================");
                    
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.out.println("========== [Security] Access Denied ==========");
                    System.out.println("[Security] Request Path: " + request.getRequestURI());
                    System.out.println("[Security] Exception: " + accessDeniedException.getMessage());
                    System.out.println("==========================================");
                    
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"Access denied\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("[SecurityConfig] SecurityFilterChain configured with JWT Filter");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all origins for production (Render deployment)
        // This allows requests from any domain, including Render's onrender.com domains
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

