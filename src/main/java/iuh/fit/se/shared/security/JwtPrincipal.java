package iuh.fit.se.shared.security;

import java.security.Principal;

public class JwtPrincipal implements Principal {

    private final String username;
    private final Long staffId;
    private final String role;
    private final String tableCode;
    private final String sessionId;

    public JwtPrincipal(String username, Long staffId, String role, String tableCode, String sessionId) {
        this.username = username;
        this.staffId = staffId;
        this.role = role;
        this.tableCode = tableCode;
        this.sessionId = sessionId;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public Long getStaffId() {
        return staffId;
    }

    public String getRole() {
        return role;
    }

    public String getTableCode() {
        return tableCode;
    }

    public String getSessionId() {
        return sessionId;
    }
}
