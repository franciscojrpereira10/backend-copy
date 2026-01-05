package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.TagBean;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.SubscriptionInfoDTO;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagResource {

    @EJB
    private TagBean tagBean;

    @EJB
    private UserBean userBean;


    // EP22 - Listar tags
    @GET
    @pt.ipleiria.estg.dei.ei.dae.academics.security.OptionalAuthenticated
    public Response getAllTags(@Context SecurityContext sc) {
        String username = null;
        if (sc.getUserPrincipal() != null) {
             username = sc.getUserPrincipal().getName();
        }
        
        // Passa o username para o Bean tratar da verificação de subscrição de forma eficiente
        List<TagDTO> dtos = tagBean.getAllTagsWithSubscriptionStatus(username);
        
        return Response.ok(dtos).build();
    }

    public static class CreateTagDTO {
        public String name;
    }

    // EP23 - Criar tag
    @POST
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response createTag(CreateTagDTO body,
                              @Context SecurityContext sc) {

        if (body == null || body.name == null || body.name.isBlank()) {
            throw new BadRequestException("name é obrigatório");
        }

        String username = sc.getUserPrincipal().getName();
        User creator = userBean.find(username);
        if (creator == null) {
            throw new UnauthorizedException("Utilizador não encontrado");
        }

        Tag tag = tagBean.create(body.name.trim(), creator);
        if (tag == null) {
            throw new ConflictException("Tag já existe");
        }

        TagDTO dto = TagDTO.from(tag);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    // ===== EP26 - Subscrever tag =====
    @POST
    @Path("/{id}/subscribe")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response subscribeTag(@PathParam("id") long tagId,
                                 @Context SecurityContext sc) {

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        Tag tag = tagBean.find(tagId);
        if (tag == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        tagBean.subscribe(tag, user);  // deve lançar ConflictException se já estiver subscrito

        Map<String, Object> response = new HashMap<>();
        response.put("tagId", tag.getId());
        response.put("tagName", tag.getName());
        response.put("subscribed", true);
        response.put("message", "Subscrição efetuada");

        return Response.ok(response).build();
    }

    // ===== EP27 - Cancelar subscrição =====
    @DELETE
    @Path("/{id}/unsubscribe")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response unsubscribeTag(@PathParam("id") long tagId,
                                   @Context SecurityContext sc) {

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        Tag tag = tagBean.find(tagId);
        if (tag == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        tagBean.unsubscribe(tag, user); // pode lançar ConflictException se não estiver subscrito

        Map<String, Object> response = new HashMap<>();
        response.put("tagId", tag.getId());
        response.put("tagName", tag.getName());
        response.put("subscribed", false);
        response.put("message", "Subscrição removida");

        return Response.ok(response).build();
    }

    // ===== EP28 - Listar subscrições do user =====
    @GET
    @Path("/subscriptions")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response listSubscriptions(@Context SecurityContext sc) {

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }
        List<SubscriptionInfoDTO> subs = tagBean.listSubscriptions(user);

        Map<String, Object> response = new HashMap<>();
        response.put("subscriptions", subs);
        response.put("totalSubscriptions", subs.size());

        return Response.ok(response).build();
    }

    // ===== EP29 - Remover tag =====
    @DELETE
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response deleteTag(@PathParam("id") long tagId) {

        Tag tag = tagBean.find(tagId);
        if (tag == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        tagBean.delete(tag);  // podes fazer soft delete / marcar como descontinuada

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tag eliminada");

        return Response.ok(response).build();
    }

    // DTO para EP30
    public static class VisibilityDTO {
        public Boolean visible;
    }

    // ===== EP30 - Alterar visibilidade de tag =====
    @PATCH
    @Path("/{id}/visibility")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response changeVisibility(@PathParam("id") long tagId,
                                     VisibilityDTO body) {

        if (body == null || body.visible == null) {
            throw new BadRequestException("visible é obrigatório");
        }

        Tag tag = tagBean.find(tagId);
        if (tag == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        tagBean.changeVisibility(tag, body.visible);

        Map<String, Object> response = new HashMap<>();
        response.put("id", tag.getId());
        response.put("name", tag.getName());
        response.put("visible", body.visible);
        response.put("message", "Tag atualizada");

        return Response.ok(response).build();
    }


}
