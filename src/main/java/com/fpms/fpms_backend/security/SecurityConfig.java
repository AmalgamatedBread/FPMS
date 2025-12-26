package com.fpms.fpms_backend.security;

import com.fpms.fpms_backend.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${cors.allowed.origins:http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins from configuration
        String[] origins = allowedOrigins.split(",");
        configuration.setAllowedOrigins(Arrays.asList(origins));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS Configuration: Allowed Origins = {}", Arrays.toString(origins));

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/api/auth/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/icons/**",
                                "/uploads/**",
                                "/error",
                                "/favicon.ico",
                                "/api/profile/health"
                        ).permitAll()

                        // Role-based access
                        .requestMatchers("/faculty-dashboard", "/my-portfolio", "/submissions", "/documents", "/calendar").hasRole("FACULTY")
                        .requestMatchers("/head-dashboard").hasRole("DEPT_HEAD")
                        .requestMatchers("/dean-dashboard").hasRole("DEAN")
                        .requestMatchers("/approval").hasAnyRole("DEAN", "DEPT_HEAD")

                        // Portfolio access for all authenticated users
                        .requestMatchers("/portfolio/**").authenticated()

                        // Common pages accessible to all authenticated users
                        .requestMatchers(
                                "/dashboard",
                                "/profile",
                                "/department",
                                "/settings",
                                "/notifications",
                                "/portfolios",
                                "/bin",
                                "/forward-docs",
                                "/access-denied"
                        ).authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .successHandler((request, response, authentication) -> {
                            log.info("Authentication SUCCESSFUL for user: {}", authentication.getName());
                            log.info("User roles: {}", authentication.getAuthorities());

                            String targetUrl = determineTargetUrl(authentication);
                            log.info("Redirecting to: {}", targetUrl);
                            response.sendRedirect(targetUrl);
                        })
                        .failureHandler((request, response, exception) -> {
                            log.error("Authentication FAILED: {}", exception.getMessage());
                            response.sendRedirect("/login?error=true");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    private String determineTargetUrl(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String authorityString = authority.getAuthority();
            log.debug("Checking authority: {}", authorityString);

            if (authorityString.equals("ROLE_DEAN")) {
                return "/dean-dashboard";
            } else if (authorityString.equals("ROLE_DEPT_HEAD")) {
                return "/head-dashboard";
            } else if (authorityString.equals("ROLE_FACULTY")) {
                return "/faculty-dashboard";
            }
        }
        return "/dashboard";
    }
}