package pt.ipleiria.estg.dei.ei.dae.academics.security;

import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;

import java.security.Principal;

public class CustomSecurityContext implements SecurityContext {

    private Long userId;
    private String username;
    private Role role;

    public CustomSecurityContext(Long userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return username;
            }
        };
    }

    @Override
    public boolean isUserInRole(String roleStr) {
        return role.toString().equals(roleStr);
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }
}
