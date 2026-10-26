package com.futuremedia.config;

import com.futuremedia.services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth
                        .requestMatchers("/api/auth/login").permitAll()

                        // Public content (exact /public plus nested /public/**)
                        .requestMatchers(HttpMethod.GET,
                                "/api/news/public", "/api/news/public/**",
                                "/api/events/public", "/api/events/public/**",
                                "/api/gallery/public", "/api/gallery/public/**",
                                "/api/vacancies/public", "/api/vacancies/public/**",
                                "/api/freelancers/public", "/api/freelancers/public/**",
                                "/api/banners/public", "/api/banners/public/**"
                        ).permitAll()

                        // Public forms
                        .requestMatchers(HttpMethod.POST, "/api/applications/apply").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/freelancer-applications/apply").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/media-audit").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/shopbeat/demo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/gated-documents/*/request", "POST")).permitAll()

                        // Public dropdowns
                        .requestMatchers(HttpMethod.GET, "/api/brands/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sales/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shopbeat/feelings/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leads/sources/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/gated-documents/public", "/api/gated-documents/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/gated-documents/download/**").permitAll()

                        // Static uploads & swagger
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()

                        // User management — SUPER_ADMIN only
                        .requestMatchers("/api/users/**").hasRole("SUPER_ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "https://futuremedia.simplyfound.com.na",
                "https://media.simplyfound.com.na",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
