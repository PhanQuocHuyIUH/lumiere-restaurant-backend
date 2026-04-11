package iuh.fit.se.identity.application.impl;

import iuh.fit.se.identity.api.dto.LoginRequest;
import iuh.fit.se.identity.api.dto.LoginResponse;
import iuh.fit.se.identity.api.dto.StaffResponse;
import iuh.fit.se.identity.application.AuthService;
import iuh.fit.se.identity.domain.Staff;
import iuh.fit.se.identity.domain.StaffStatus;
import iuh.fit.se.identity.infrastructure.StaffRepository;
import iuh.fit.se.shared.security.JwtService;
import iuh.fit.se.shared.security.StaffUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long jwtExpirationMs;

    public AuthServiceImpl(
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${spring.security.jwt.expiration:86400000}") long jwtExpirationMs
    ) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());

        Staff staff = staffRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(normalizedUsername)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new DisabledException("Staff account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        StaffUserDetails userDetails = StaffUserDetails.builder()
                .id(staff.getId())
                .username(staff.getUsername())
                .password(staff.getPasswordHash())
                .role(staff.getRole().name())
                .active(true)
                .build();

        String token = jwtService.generateToken(userDetails);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .staff(StaffResponse.from(staff))
                .build();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
