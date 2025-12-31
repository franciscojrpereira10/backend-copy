package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.ActivityDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ActivityBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Activity;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;

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

    // EP38 - histórico do próprio
    @GET
    @Path("/my")
    @RolesAllowed({"CONTRIBUTOR","MANAGER","ADMIN"})
    public Response myHistory(@QueryParam("type") ActivityType type,
                              @QueryParam("dateFrom") String dateFrom,
                              @QueryParam("dateTo") String dateTo,
                              @QueryParam("limit") @DefaultValue("50") int limit,
                              @HeaderParam("x-user-id") Long authUserId) {

        Instant from = dateFrom != null ? Instant.parse(dateFrom) : null;
        Instant to   = dateTo   != null ? Instant.parse(dateTo)   : null;

        List<Activity> list = activityBean.findByUser(authUserId, type, from, to, limit);
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
                                @QueryParam("limit") @DefaultValue("50") int limit) {

        Instant from = dateFrom != null ? Instant.parse(dateFrom) : null;
        Instant to   = dateTo   != null ? Instant.parse(dateTo)   : null;

        List<Activity> list = activityBean.findByUser(userId, type, from, to, limit);
        List<ActivityDTO> dtos = ActivityDTO.from(list);

        Map<String,Object> resp = new HashMap<>();
        resp.put("userId", userId);
        resp.put("activities", dtos);
        resp.put("totalActivities", dtos.size());

        return Response.ok(resp).build();
    }
}
