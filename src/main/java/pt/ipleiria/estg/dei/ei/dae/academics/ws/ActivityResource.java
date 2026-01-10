package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.ActivityDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ActivityBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Activity;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;

import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActivityResource {

    @EJB
    private ActivityBean activityBean;

    @EJB
    private pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean userBean;

    // EP38 - histórico do próprio
    @GET
    @Path("/my")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR","MANAGER","ADMIN"})
    public Response myHistory(@Context jakarta.ws.rs.core.SecurityContext sc,
                              @QueryParam("type") ActivityType type,
                              @QueryParam("dateFrom") String dateFrom,
                              @QueryParam("dateTo") String dateTo,
                              @QueryParam("offset") @DefaultValue("0") int offset,
                              @QueryParam("limit") @DefaultValue("50") int limit) {

        if (sc.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String username = sc.getUserPrincipal().getName();
        pt.ipleiria.estg.dei.ei.dae.academics.entities.User user = userBean.find(username);
        if (user == null) {
             return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Instant from = dateFrom != null ? Instant.parse(dateFrom) : null;
        Instant to   = dateTo   != null ? Instant.parse(dateTo)   : null;

        List<Activity> list = activityBean.findByUser(user.getId(), type, from, to, offset, limit);
        List<ActivityDTO> dtos = ActivityDTO.from(list);

        Map<String,Object> resp = new HashMap<>();
        resp.put("activities", dtos);
        resp.put("totalActivities", dtos.size());

        return Response.ok(resp).build();
    }

    // EP39 - histórico de um utilizador (admin)
    @GET
    @Path("/user/{userId}")
    @RolesAllowed({"ADMIN"})
    public Response userHistory(@PathParam("userId") Long userId,
                                @QueryParam("type") ActivityType type,
                                @QueryParam("dateFrom") String dateFrom,
                                @QueryParam("dateTo") String dateTo,
                                @QueryParam("offset") @DefaultValue("0") int offset,
                                @QueryParam("limit") @DefaultValue("50") int limit) {

        Instant from = dateFrom != null ? Instant.parse(dateFrom) : null;
        Instant to   = dateTo   != null ? Instant.parse(dateTo)   : null;

        List<Activity> list = activityBean.findByUser(userId, type, from, to, offset, limit);
        List<ActivityDTO> dtos = ActivityDTO.from(list);

        Map<String,Object> resp = new HashMap<>();
        resp.put("userId", userId);
        resp.put("activities", dtos);
        resp.put("totalActivities", dtos.size());

        return Response.ok(resp).build();
    }
}
