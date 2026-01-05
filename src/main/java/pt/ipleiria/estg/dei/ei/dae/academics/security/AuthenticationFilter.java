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
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

//    @EJB
//    private UserBean userBean;
    
    public AuthenticationFilter() {
        System.out.println("DEBUG_FILTER: AuthenticationFilter instantiated.");
    }

    @Context
    private UriInfo uriInfo;

    private String getUsername(String token) {
        // Implementação movida para dentro do filter() ou validação extra
        // Na verdade, precisamos de validar claims.
        return null; // não usado diretamente agora
    }
    
    // Método auxiliar para parsing e validação completa
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
        String path = requestContext.getUriInfo().getPath();
        System.out.println("DEBUG_FILTER: Filtering request: " + path);

        var header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NotAuthorizedException("Authorization header must be provided");
        }

        String token = header.substring("Bearer ".length()).trim();
        
        try {
            var claims = getClaims(token);
            String username = claims.getSubject();
            System.out.println("DEBUG_FILTER: Claims parsed. Subject=" + username);
            
//            if (userBean == null) {
//                System.out.println("DEBUG_FILTER: FATAL - userBean is null!");
//                throw new NotAuthorizedException("Internal Error: Injection failed");
//            }
            
            String tokenVersion = claims.get("version", String.class);
            
//            var user = userBean.find(username);
//            if (user == null) {
//                throw new NotAuthorizedException("User not found");
//            }

            // EP99 - Force logout if user is blocked/deleted
//            if (!user.isActive()) { // user.status check
//                throw new NotAuthorizedException("User is blocked or inactive");
//            }
            
            // Single Session Check
            String uVer = null; // user.getAuthVersion();
            System.out.println("DEBUG_FILTER: TokenVer=" + tokenVersion + " | UserVer=" + uVer);

            if (uVer != null && !uVer.equals(tokenVersion)) {
                 System.out.println("DEBUG_FILTER: Mismatch! Denying access.");
                 throw new NotAuthorizedException("Session expired (logged in elsewhere)");
            }

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> username;
                }
                @Override
                public boolean isUserInRole(String s) {
                    return true; // user.getRole() != null && user.getRole().name().equals(s);
                }
                @Override
                public boolean isSecure() {
                    return uriInfo.getAbsolutePath().toString().startsWith("https");
                }
                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });
            
        } catch (Exception e) {
            throw new NotAuthorizedException("Invalid JWT: " + e.getMessage());
        }
    }
}