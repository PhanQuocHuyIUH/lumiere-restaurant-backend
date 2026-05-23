package iuh.fit.se.shared.config;

import iuh.fit.se.shared.security.AiServiceKeyAuthFilter;
import iuh.fit.se.shared.security.JwtAuthFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AiServiceKeyAuthFilter aiServiceKeyAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AiServiceKeyAuthFilter aiServiceKeyAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.aiServiceKeyAuthFilter = aiServiceKeyAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/ai/**").hasRole("AI_SERVICE")
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.GET, "/tables/qr/*/init", "/tables/qr/*/menu", "/tables/qr/*/trending").permitAll()
                    .requestMatchers(HttpMethod.GET, "/menu/items/*/combo").permitAll()
                    .requestMatchers(HttpMethod.GET, "/tables/qr/*/bill-summary", "/tables/qr/*/payment-request").permitAll()
                    .requestMatchers(HttpMethod.POST, "/tables/qr/*/payment-request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/orders", "/orders/*/revisions", "/orders/recommendations", "/chatbot", "/recommendation-logs").permitAll()
                        .requestMatchers(HttpMethod.POST, "/support").permitAll()
                        .requestMatchers(HttpMethod.GET, "/support/table/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payments/webhooks/vnpay").permitAll()
                        .requestMatchers(HttpMethod.POST, "/payments/webhooks/vnpay").permitAll()
                        .requestMatchers("/ws/**", "/ws", "/ws-native/**", "/ws-native").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/menu/**").hasAnyRole("KITCHEN", "WAITER", "CASHIER", "MANAGER")
                        .requestMatchers("/support/**").hasAnyRole("WAITER", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/tables/*/status").hasAnyRole("WAITER", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/tables/**").hasAnyRole("WAITER", "CASHIER", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/kitchen/**").hasAnyRole("KITCHEN", "WAITER", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/kitchen/**").hasAnyRole("KITCHEN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/kitchen/menu-items/**").hasAnyRole("KITCHEN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/items/*/serve", "/orders/*/serve-all")
                        .hasAnyRole("WAITER", "MANAGER")
                        .requestMatchers("/orders/**").hasAnyRole("WAITER", "CASHIER", "MANAGER")
                        // Waiter needs the payment status (to fetch QR for the table-side
                        // hand-off flow). Confirm/cancel/refund stay CASHIER-only below.
                        .requestMatchers(HttpMethod.GET, "/payments/orders/*/status")
                            .hasAnyRole("WAITER", "CASHIER", "MANAGER")
                        .requestMatchers("/payments/**").hasAnyRole("CASHIER", "MANAGER")
                        .requestMatchers("/payment-requests/**").hasAnyRole("CASHIER", "MANAGER")
                        .requestMatchers("/manager/inventory/**").hasRole("MANAGER")
                        .requestMatchers("/manager/tables/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/kitchen/inventory/**").hasAnyRole("KITCHEN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/kitchen/inventory/**").hasAnyRole("KITCHEN", "MANAGER")
                        .requestMatchers("/analytics/**").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .addFilterBefore(aiServiceKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
