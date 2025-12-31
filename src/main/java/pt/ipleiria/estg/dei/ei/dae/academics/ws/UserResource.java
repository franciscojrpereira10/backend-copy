package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @EJB
    private UserBean userBean;

    // EP32 - Listar utilizadores
    @GET
    @RolesAllowed({"ADMIN"})
    public Response getAll() {
        List<User> users = userBean.getAll();
        List<UserDTO> dtos = UserDTO.from(users);

        Map<String, Object> response = new HashMap<>();
        response.put("users", dtos);
        response.put("totalUsers", dtos.size());

        return Response.ok(response).build();
    }

    // EP31 - Criar utilizador
    @POST
    @RolesAllowed({"ADMIN"})
    public Response create(UserDTO dto) {
        if (dto == null || dto.getUsername() == null ||
                dto.getEmail() == null || dto.getRole() == null) {
            throw new BadRequestException("username, email e role são obrigatórios");
        }

        String defaultPassword = "changeme123";

        User user = userBean.create(
                dto.getUsername(),
                dto.getEmail(),
                defaultPassword,
                dto.getRole()
        );

        UserDTO created = UserDTO.from(user);

        return Response.status(Response.Status.CREATED)
                .entity(created)
                .build();
    }

    // EP33 - Detalhe de utilizador
    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response get(@PathParam("id") Long id) {
        User user = userBean.find(id);
        if (user == null) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }
        UserDTO dto = userBean.toDetailedDTO(user);
        return Response.ok(dto).build();
    }

    // EP34 - Editar utilizador
    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response update(@PathParam("id") Long id, UserDTO dto) {
        if (dto == null || dto.getEmail() == null) {
            throw new BadRequestException("email é obrigatório");
        }

        Role newRole = dto.getRole();

        User updated = userBean.updateUser(id, dto.getEmail(), newRole, true);
        if (updated == null) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }

        UserDTO responseDto = UserDTO.from(updated);
        return Response.ok(responseDto).build();
    }

    // EP35 - Alterar status (ativar/suspender)
    public static class ChangeStatusDTO {
        public String status;
        public String reason;
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"ADMIN"})
    public Response changeStatus(@PathParam("id") Long id, ChangeStatusDTO body) {
        if (body == null || body.status == null) {
            throw new BadRequestException("status é obrigatório");
        }

        // NÃO fazer UserStatus.valueOf aqui
        User updated = userBean.changeStatus(id, body.status);
        if (updated == null) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }

        var response = new java.util.HashMap<String, Object>();
        response.put("id", updated.getId());
        response.put("username", updated.getUsername());
        response.put("status", updated.getStatus());
        response.put("message", "Utilizador suspenso");
        return Response.ok(response).build();
    }


    // EP36 - Alterar role
    public static class ChangeRoleDTO {
        public String role;
    }

    @PATCH
    @Path("/{id}/role")
    @RolesAllowed({"ADMIN"})
    public Response changeRole(@PathParam("id") Long id, ChangeRoleDTO body) {
        if (body == null || body.role == null) {
            throw new BadRequestException("role é obrigatório");
        }

        User updated = userBean.changeRole(id, body.role);
        if (updated == null) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }

        var response = new java.util.HashMap<String, Object>();
        response.put("id", updated.getId());
        response.put("username", updated.getUsername());
        response.put("role", updated.getRole());
        response.put("message", "Role atualizado");
        return Response.ok(response).build();
    }

    // EP37 - Eliminar utilizador
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        boolean removed = userBean.softDelete(id);
        if (!removed) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }
        return Response.ok()
                .entity(java.util.Map.of("message", "Utilizador eliminado; publicações preservadas"))
                .build();
    }

}
