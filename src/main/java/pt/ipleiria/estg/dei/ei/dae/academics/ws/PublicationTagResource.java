package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ForbiddenException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;

@Path("/publications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicationTagResource {

    @EJB
    private PublicationBean publicationBean;

    // EP24 - Associar tag a publicação
    @PUT
    @Path("{id}/tags")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response associateTagToPublication(@PathParam("id") long publicationId,
                                              TagDTO tagDTO) {
        if (tagDTO == null || tagDTO.getId() == null) {
            throw new BadRequestException("Tag id é obrigatório");
        }
        try {
            publicationBean.associateTag(publicationId, tagDTO.getId());

            Publication p = publicationBean.find(publicationId);
            var dto = publicationBean.toDetailedDTO(p);   // já tem lista de tags.[file:1]

            var response = new java.util.HashMap<String, Object>();
            response.put("publicationId", publicationId);
            response.put("tags", dto.getTags());
            response.put("message", "Tag associada");

            return Response.ok(response).build();
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Publication or Tag not found");
        } catch (ConflictException e) {
            throw new ConflictException("Tag already associated to this publication");
        }
    }

    // EP25 - Desassociar tag
    @DELETE
    @Path("{publicationId}/tags/{tagId}")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response removeTagFromPublication(@PathParam("publicationId") long publicationId,
                                             @PathParam("tagId") long tagId) {
        try {
            publicationBean.removeTag(publicationId, tagId);

            Publication p = publicationBean.find(publicationId);
            var dto = publicationBean.toDetailedDTO(p);   // tags atualizadas.[file:1]

            var response = new java.util.HashMap<String, Object>();
            response.put("publicationId", publicationId);
            response.put("tags", dto.getTags());
            response.put("message", "Tag removida");

            return Response.ok(response).build();
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Publication or Tag not found");
        } catch (ConflictException e) {
            throw new ConflictException("Tag is not associated to this publication");
        }
    }
}
