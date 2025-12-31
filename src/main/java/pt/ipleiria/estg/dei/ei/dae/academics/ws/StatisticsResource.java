package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.StatisticsBean;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;

@Path("/statistics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatisticsResource {

    @EJB
    private StatisticsBean statisticsBean;

    // EP48 - Estatísticas globais
    @GET
    @Path("/global")
    @Authenticated
    @RolesAllowed({"ADMIN"})
    public Response global() {
        return Response.ok(statisticsBean.getGlobalStatistics()).build();
    }

    // EP49 - Estatísticas pessoais
    @GET
    @Path("/personal")
    @Authenticated
    public Response personal(@Context SecurityContext sc) {
        if (sc.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        String username = sc.getUserPrincipal().getName();
        return Response.ok(statisticsBean.getPersonalStatistics(username)).build();
    }
}
