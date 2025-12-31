package pt.ipleiria.estg.dei.ei.dae.academics.security;

import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
@Authenticated
@Priority(1001)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return;
        }

        RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            // Sem @RolesAllowed, não há verificação extra
            return;
        }

        SecurityContext sc = requestContext.getSecurityContext();
        if (sc == null || sc.getUserPrincipal() == null) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"User not authenticated\"}")
                            .build()
            );
            return;
        }

        String[] allowedRoles = rolesAllowed.value();
        boolean hasRole = Arrays.stream(allowedRoles)
                .anyMatch(sc::isUserInRole); // usa isUserInRole do AuthenticationFilter

        if (!hasRole) {
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\":\"User does not have required role\"}")
                            .build()
            );
        }
    }
}
