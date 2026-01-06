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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.Principal;

@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @EJB
    private UserBean userBean;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NotAuthorizedException("O cabeçalho de autorização é obrigatório");
        }

        String token = header.substring("Bearer ".length()).trim();
        
        try {
            Key key = new SecretKeySpec(TokenIssuer.SECRET_KEY, TokenIssuer.ALGORITHM);
            
            // Obter as claims (dados) do token
            var claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
            String username = claims.getSubject();
            String tokenAuthVersion = claims.get("authVersion", String.class); // <--- LER DO TOKEN

            User user = userBean.find(username);
            
            // Validação extra: O user existe E a versão bate certo?
            if (user == null || 
                user.getAuthVersion() == null || 
                !user.getAuthVersion().equals(tokenAuthVersion)) {
                
                // Se a versão for diferente, significa que alguém fez login noutro sítio
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                return;
            }
        
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> username;
                }
                @Override
                public boolean isUserInRole(String s) {
                    return user.getRole() != null && user.getRole().name().equals(s);
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
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}