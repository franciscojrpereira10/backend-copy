package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.CommentDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.CommentEditHistory;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.CommentBean;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ForbiddenException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;

import java.util.Date;

@Path("/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    @EJB
    private CommentBean commentBean;

    @EJB
    private UserBean userBean;

    // ===== EP19 - Editar comentário =====
    @PUT
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response editComment(@PathParam("id") Long id,
                                CommentDTO body,
                                @Context SecurityContext sc) {

        if (body == null || body.getContent() == null || body.getContent().isBlank()) {
            throw new BadRequestException("content é obrigatório");
        }

        Comment c = commentBean.find(id);
        if (c == null) {
            throw new EntityNotFoundException("Comentário não encontrado");
        }

        String username = sc.getUserPrincipal().getName();
        User editor = userBean.find(username);
        if (editor == null) {
            throw new UnauthorizedException("Utilizador não encontrado");
        }

        boolean isAuthor = c.getAuthor() != null &&
                c.getAuthor().getId().equals(editor.getId());
        boolean isManagerOrAdmin =
                sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");

        if (!isAuthor && !isManagerOrAdmin) {
            throw new ForbiddenException("Not allowed to edit this comment");
        }

        // guardar histórico da edição
        CommentEditHistory h = new CommentEditHistory();
        h.setComment(c);
        h.setOldContent(c.getContent());
        h.setNewContent(body.getContent());
        h.setEditedBy(editor);
        h.setEditedAt(new Date());
        commentBean.persistHistory(h);

        // atualizar comentário
        c.editContent(body.getContent());
        commentBean.update(c);

        CommentDTO dto = CommentDTO.from(c);
        return Response.ok(dto).build();
    }

    // DTO simples para EP20
    public static class VisibilityDTO {
        public Boolean visible;
        public String reason;
    }

    // ===== EP20 - Alterar visibilidade =====
    @PATCH
    @Path("/{id}/visibility")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response changeVisibility(@PathParam("id") Long id,
                                     VisibilityDTO body,
                                     @Context SecurityContext sc) {

        if (body == null || body.visible == null) {
            throw new BadRequestException("visible é obrigatório");
        }

        Comment c = commentBean.find(id);
        if (c == null) {
            throw new EntityNotFoundException("Comentário não encontrado");
        }

        String username = sc.getUserPrincipal().getName();
        User editor = userBean.find(username);
        if (editor == null) {
            throw new UnauthorizedException("Utilizador não encontrado");
        }

        // apenas altera visibilidade no próprio comentário
        c.changeVisibility(body.visible, body.reason);
        commentBean.update(c);

        CommentDTO dto = CommentDTO.from(c);
        return Response.ok(dto).build();
    }


    // ===== EP21 - Eliminar comentário =====
    @DELETE
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response deleteComment(@PathParam("id") Long id,
                                  @Context SecurityContext sc) {

        Comment c = commentBean.find(id);
        if (c == null) {
            throw new EntityNotFoundException("Comentário não encontrado");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("Utilizador não encontrado");
        }

        boolean isAuthor = c.getAuthor() != null &&
                c.getAuthor().getId().equals(user.getId());
        boolean isAdmin = sc.isUserInRole("ADMIN");

        if (!isAuthor && !isAdmin) {
            throw new ForbiddenException("Not allowed to delete this comment");
        }

        // por agora delete físico; se quiseres soft delete podes só marcar visível=false
        commentBean.remove(c.getId());

        return Response.ok().build();
    }
}
