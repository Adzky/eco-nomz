package id.co.economz.eco_nomz.config;

import id.co.economz.eco_nomz.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            @Value("${app.security.require-https:false}") boolean requireHttps
    ) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/payments/midtrans/notifications"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/customer", "/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/owner", "/owner/**").hasRole("RESTAURANT_OWNER")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/", "/error", "/register", "/login", "/language",
                                "/css/**", "/js/**", "/images/**",
                                "/economz-logo-horizontal.png", "/economz-symbol.png",
                                "/offers", "/offers/**", "/payments/midtrans/notifications",
                                "/restaurants", "/restaurants/**", "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendRedirect("/login"))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(false)
                        .maxAgeInSeconds(requireHttps ? 31_536_000 : 0)));
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
