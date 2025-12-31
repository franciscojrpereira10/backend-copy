package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.NotificationDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.NotificationBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Notification;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @EJB
    private NotificationBean notificationBean;

    // EP45 - Listar notificações
    @GET
    @Authenticated   // Auth/roles: todos com token válido
    public Response list(
            @Context SecurityContext sc,
            @QueryParam("unreadOnly") @DefaultValue("false") boolean unreadOnly,
            @QueryParam("limit") Integer limit) {

        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = sc.getUserPrincipal().getName();
        User user = notificationBean.findUser(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        List<Notification> all = notificationBean.findByUser(user, unreadOnly);
        if (limit != null && limit > 0 && limit < all.size()) {
            all = all.subList(0, limit);
        }

        List<NotificationDTO> dtos = NotificationDTO.from(all);

        long unreadCount = notificationBean.countUnread(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("notifications", dtos);
        resp.put("unreadCount", unreadCount);

        return Response.ok(resp).build();
    }

    // EP46 - Marcar notificação como lida
    @PATCH
    @Path("/{id}/read")
    @Authenticated
    public Response markAsRead(@Context SecurityContext sc,
                               @PathParam("id") long id) {

        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = sc.getUserPrincipal().getName();
        Notification n = notificationBean.find(id);
        if (n == null) {
            throw new EntityNotFoundException("Notification not found");
        }

        // garantir que a notificação pertence ao user autenticado
        if (!n.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("Cannot change notifications of other users");
        }

        notificationBean.markAsRead(n);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", n.getId());
        resp.put("read", true);
        resp.put("message", "Notificação marcada como lida");

        return Response.ok(resp).build();
    }

    // EP47 - Marcar todas como lidas
    @PATCH
    @Path("/read-all")
    @Authenticated
    public Response markAllAsRead(@Context SecurityContext sc) {

        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = sc.getUserPrincipal().getName();
        User user = notificationBean.findUser(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        int marked = notificationBean.markAllAsRead(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("markedAsRead", marked);
        resp.put("message", "Todas as notificações foram marcadas como lidas");

        return Response.ok(resp).build();
    }
}
