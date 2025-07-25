package com.example.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Value("${app.security.api-key}")
  private String apiKey;

  @Value("${app.security.enabled:true}")
  private boolean securityEnabled;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    if (!securityEnabled) {
      return http
          .csrf(csrf -> csrf.disable())
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }

    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/actuator/**").permitAll()
            .requestMatchers("/api/v1/swagger-ui/**", "/api/v1/api-docs/**").permitAll()
            .requestMatchers("/api/v1/inventory/*/availability").permitAll()
            .anyRequest().authenticated())
        .httpBasic(httpBasic -> {})
        .build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    UserDetails admin = User.builder()
        .username("admin")
        .password(passwordEncoder().encode("admin123"))
        .roles("ADMIN")
        .authorities("INVENTORY_READ", "INVENTORY_UPDATE", "INVENTORY_ADJUST",
            "INVENTORY_SYNC", "PURCHASE_CANCEL", "PURCHASE_REFUND",
            "SALES_READ", "REPORTS_READ")
        .build();

    UserDetails operator = User.builder()
        .username("operator")
        .password(passwordEncoder().encode("operator123"))
        .roles("OPERATOR")
        .authorities("INVENTORY_READ", "PURCHASE_CANCEL")
        .build();

    return new InMemoryUserDetailsManager(admin, operator);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}