package iuh.fit.se.identity.application;

import iuh.fit.se.identity.api.dto.LoginRequest;
import iuh.fit.se.identity.api.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
