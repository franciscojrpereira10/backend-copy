package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;
import java.util.HashMap;
import java.util.Map;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @EJB
    private UserBean userBean;

    public static class PreferencesDTO {
        public Boolean emailNotifications;
        public String language;
    }

    public static class UpdateProfileDTO {
        public String email;
        public PreferencesDTO preferences;
    }

    // EP43 - Consultar perfil
    @GET
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR","MANAGER","ADMIN"})
    public Response me(@Context SecurityContext sc) {

        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        Map<String,Object> stats = new HashMap<>();
        stats.put("publicationsSubmitted", userBean.countPublicationsSubmitted(user));
        stats.put("commentsPosted", userBean.countCommentsPosted(user));
        stats.put("ratingsGiven", userBean.countRatingsGiven(user));
        stats.put("subscribedTags", userBean.countSubscribedTags(user));

        Map<String,Object> prefs = new HashMap<>();
        prefs.put("emailNotifications", user.isEmailNotifications());
        prefs.put("language", user.getPreferredLanguage());

        Map<String,Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("email", user.getEmail());
        resp.put("role", user.getMainRole());   // ajusta conforme o teu modelo
        resp.put("status", user.getStatus().name());
        resp.put("statistics", stats);
        resp.put("preferences", prefs);

        return Response.ok(resp).build();
    }

    // EP44 - Editar perfil
    @PUT
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR","MANAGER","ADMIN"})
    public Response update(@Context SecurityContext sc,
                           UpdateProfileDTO body) {

        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        if (body == null) {
            throw new BadRequestException("Body é obrigatório");
        }

        if (body.email != null && !body.email.isBlank()) {
            user.setEmail(body.email.trim());
        }

        if (body.preferences != null) {
            if (body.preferences.emailNotifications != null) {
                user.setEmailNotifications(body.preferences.emailNotifications);
            }
            if (body.preferences.language != null) {
                user.setPreferredLanguage(body.preferences.language);
            }
        }

        userBean.update(user); // método simples que faz merge(user)

        Map<String,Object> prefs = new HashMap<>();
        prefs.put("emailNotifications", user.isEmailNotifications());
        prefs.put("language", user.getPreferredLanguage());

        Map<String,Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("email", user.getEmail());
        resp.put("preferences", prefs);
        resp.put("message", "Perfil atualizado");

        return Response.ok(resp).build();
    }
}
