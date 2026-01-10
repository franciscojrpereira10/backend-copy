package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ActivityBean;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;
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

    @EJB
    private pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ConfigBean configBean;

    @EJB
    private ActivityBean activityBean;

    // EP32 - Listar utilizadores
    @GET
    @RolesAllowed({"ADMIN"})
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
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
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
    public Response delete(@PathParam("id") Long id) {
        boolean removed = userBean.remove(id); // Changed to hard delete
        if (!removed) {
            throw new EntityNotFoundException("Utilizador não encontrado");
        }
        return Response.ok()
                .entity(java.util.Map.of("message", "Utilizador eliminado permanentemente."))
                .build();
    }

    // EP - Importar utilizadores via CSV
    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"ADMIN"})
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
    public Response importUsers(org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput input) throws java.io.IOException {
        Map<String, List<org.jboss.resteasy.plugins.providers.multipart.InputPart>> uploadForm = input.getFormDataMap();
        List<org.jboss.resteasy.plugins.providers.multipart.InputPart> inputParts = uploadForm.get("file");

        int processed = 0;
        int created = 0;
        int failed = 0;

        for (org.jboss.resteasy.plugins.providers.multipart.InputPart inputPart : inputParts) {
            // Ler o conteúdo do ficheiro
            java.io.InputStream inputStream = inputPart.getBody(java.io.InputStream.class, null);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("username")) continue; // Ignorar header ou vazias

                processed++;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String username = parts[0].trim();
                    String email = parts[1].trim();
                    String roleStr = parts[2].trim();
                    
                    try {
                        Role role = Role.valueOf(roleStr.toUpperCase());
                        // Tentar criar
                        User existing = userBean.find(username);
                        if (existing == null) {
                           userBean.create(username, email, "changeme123", role);
                           created++;
                        } else {
                            // Poderia atualizar, mas para já ignora
                            failed++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        failed++;
                    }
                } else {
                    failed++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processed", processed);
        result.put("created", created);
        result.put("failed", failed);

        return Response.ok(result).build();
    }

    // EP - Upload Profile Picture
    @POST
    @Path("/me/picture")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
    public Response uploadProfilePicture(org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput input,
                                         @jakarta.ws.rs.core.Context jakarta.ws.rs.core.SecurityContext sc) {
        try {
            String username = sc.getUserPrincipal().getName();
            User user = userBean.find(username);
            
            if (user == null) {
                throw new pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException("User not found");
            }

            Map<String, List<org.jboss.resteasy.plugins.providers.multipart.InputPart>> uploadForm = input.getFormDataMap();
            List<org.jboss.resteasy.plugins.providers.multipart.InputPart> inputParts = uploadForm.get("file");

            if (inputParts != null && !inputParts.isEmpty()) {
                org.jboss.resteasy.plugins.providers.multipart.InputPart filePart = inputParts.get(0);
                
                // Save file
                // Better to detect extension if possible, but for profile pics, usually safe to rename or just save content.
                // Let's get original filename to check extension
                String originalName = getFileName(filePart);
                String extension = "";
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                } else {
                    extension = ".png"; // default
                }
                
                String filename = "profile_" + user.getId() + "_" + java.util.UUID.randomUUID().toString() + extension;

                java.io.InputStream inputStream = filePart.getBody(java.io.InputStream.class, null);
                writeFile(inputStream, filename);
                
                userBean.updateProfilePicture(user.getId(), filename);
                
                activityBean.create(user, ActivityType.UPLOAD, "Adicionou foto de perfil", "USER_PROFILE", user.getId());

                return Response.ok(java.util.Map.of("filename", filename)).build();
            } else {
                 throw new BadRequestException("File is required");
            }
        } catch (java.io.IOException e) {
             return Response.status(500).entity("Error uploading file").build();
        }
    }

    // EP - Delete Profile Picture
    @DELETE
    @Path("/me/picture")
    @pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated
    public Response deleteProfilePicture(@jakarta.ws.rs.core.Context jakarta.ws.rs.core.SecurityContext sc) {
        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        
        if (user.getProfilePictureFilename() != null) {
            java.nio.file.Path path = java.nio.file.Paths.get(configBean.getUploadsDir(), user.getProfilePictureFilename());
            try {
                java.nio.file.Files.deleteIfExists(path);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }

        userBean.updateProfilePicture(user.getId(), null);
        
        activityBean.create(user, ActivityType.DELETE, "Eliminou foto de perfil", "USER_PROFILE", user.getId());
        
        return Response.noContent().build();
    }

    @GET
    @Path("/{username}/picture")
    public Response getProfilePicture(@PathParam("username") String username) {
        User user = userBean.find(username);
        if (user == null || user.getProfilePictureFilename() == null) {
            // Return default or 404? 404 is cleaner for frontend to show default avatar
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        java.nio.file.Path path = java.nio.file.Paths.get(
                configBean.getUploadsDir(),
                user.getProfilePictureFilename()
        );

        if (!java.nio.file.Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            // Detect mime type
            String mimeType = java.nio.file.Files.probeContentType(path);
            if (mimeType == null) mimeType = "image/png";

            return Response.ok(java.nio.file.Files.newInputStream(path))
                    .type(mimeType)
                    .build();
        } catch (java.io.IOException e) {
            return Response.status(500).build();
        }
    }

    // Helpers
    private String getFileName(org.jboss.resteasy.plugins.providers.multipart.InputPart part) {
        jakarta.ws.rs.core.MultivaluedMap<String, String> header = part.getHeaders();
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalName = name[1].trim().replaceAll("\"", "");
                return finalName;
            }
        }
        return "unknown";
    }

    private void writeFile(java.io.InputStream inputStream, String filename) throws java.io.IOException {
        java.io.File file = new java.io.File(configBean.getUploadsDir());
        if (!file.exists()) {
            file.mkdirs();
        }
        java.io.File finalFile = new java.io.File(configBean.getUploadsDir() + java.io.File.separator + filename);
        try (java.io.FileOutputStream fop = new java.io.FileOutputStream(finalFile)) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                fop.write(bytes, 0, read);
            }
            fop.flush();
        }
    }
}
