package com.smartdine.coreheart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Industry standard for hashing
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Disables default Spring Security in-memory user generation & warning
        return username -> {
            throw new UsernameNotFoundException("User not found in Spring Security context");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Stateless APIs don't need CSRF
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/ws/**", "/error", "/api/mock-cloud/**", "/api/activation/**").permitAll() // Allow Login, WebSockets, mock cloud, activation, and error path
                
                // Allow Waiters, Billers, and Admins to VIEW tables (GET) and update status (PATCH), but NOT create/edit them
                .requestMatchers(HttpMethod.GET, "/api/admin/tables", "/api/admin/tables/**").hasAnyRole("WAITER", "BILLER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/tables/**").hasAnyRole("WAITER", "BILLER", "ADMIN")
                
                // 1. Allow Waiters, Billers, Chefs, and Admins to VIEW Menu Items
                .requestMatchers(HttpMethod.GET, "/api/admin/menu/items").hasAnyRole("WAITER", "BILLER", "KITCHEN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/menu/modifier-groups/global").hasAnyRole("WAITER", "BILLER", "KITCHEN", "ADMIN")
                
                // 2. Allow active staff roles to toggle item AVAILABILITY (Out of Stock / 86-ing)
                .requestMatchers(HttpMethod.PUT, "/api/admin/menu/items/*/availability").hasAnyRole("WAITER", "BILLER", "KITCHEN", "ADMIN")
                
                // 3. Allow Billers and Admins to manage "Today's Menu" promotions
                .requestMatchers(HttpMethod.PUT, "/api/admin/menu/items/*/todays-menu").hasAnyRole("BILLER", "ADMIN")
                
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/kitchen/**").hasAnyRole("KITCHEN", "ADMIN")
                .requestMatchers("/api/waiter/**").hasAnyRole("WAITER", "BILLER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}