package pt.ipleiria.estg.dei.ei.dae.academics.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Principal;

@Provider
@OptionalAuthenticated
@Priority(Priorities.AUTHENTICATION)
public class OptionalAuthenticationFilter implements ContainerRequestFilter {

    @EJB
    private UserBean userBean;

    @Context
    private UriInfo uriInfo;

    private io.jsonwebtoken.Claims getClaims(String token) {
        SecretKey key = new SecretKeySpec(
                TokenIssuer.SECRET_KEY, 0, TokenIssuer.SECRET_KEY.length, TokenIssuer.ALGORITHM);
        return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        
        try {
            var claims = getClaims(token);
            String username = claims.getSubject();
            String tokenVersion = claims.get("authVersion", String.class);
            
            var user = userBean.find(username);
            if (user == null) return;
            
            if (!user.isActive()) throw new NotAuthorizedException("Blocked");
            
            if (user.getAuthVersion() != null && !user.getAuthVersion().equals(tokenVersion)) {
                 throw new NotAuthorizedException("Session expired");
            }

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() { return user::getUsername; }
                @Override
                public boolean isUserInRole(String s) { 
                    return user.getRole() != null && user.getRole().name().equals(s); 
                }
                @Override
                public boolean isSecure() { return uriInfo.getAbsolutePath().toString().startsWith("https"); }
                @Override
                public String getAuthenticationScheme() { return "Bearer"; }
            });
            
        } catch (Exception e) {
            // Ignore invalid tokens for OptionalAuth
        }
    }
}
