package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import pt.ipleiria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ConfigBean;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.CommentDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ForbiddenException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/publications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicationResource {

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private UserBean userBean;

    @EJB
    private ConfigBean configBean;

    // EP05 + EP10
    @GET
    @PermitAll
    public Response listAndSort(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("sortBy") String sortBy,
                                @QueryParam("order") @DefaultValue("desc") String order) {

        List<Publication> data = publicationBean.list(page, size);
        List<PublicationDTO> dtos = PublicationDTO.from(data);

        dtos = publicationBean.sortPublications(dtos, sortBy, order);

        return Response.ok(dtos).build();
    }

    // EP06
    @GET
    @Path("/{id}")
    @PermitAll
    public Response get(@PathParam("id") Long id) {
        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }
        PublicationDTO dto = publicationBean.toDetailedDTO(p);
        return Response.ok(dto).build();
    }

    // EP04
    @POST
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response create(PublicationDTO dto,
                           @Context SecurityContext sc) {

        if (dto == null || dto.getTitle() == null || dto.getFilename() == null) {
            throw new BadRequestException("title e filename são obrigatórios");
        }

        String username = sc.getUserPrincipal().getName();
        User uploader = userBean.find(username);
        if (uploader == null) {
            throw new UnauthorizedException("User not found");
        }

        FileType fileType;
        String lower = dto.getFilename().toLowerCase();
        if (lower.endsWith(".pdf")) {
            fileType = FileType.PDF;
        } else if (lower.endsWith(".zip")) {
            fileType = FileType.ZIP;
        } else {
            throw new BadRequestException("Tipo de ficheiro não suportado");
        }

        Long id = publicationBean.create(
                dto.getTitle(),
                dto.getSummary(),
                dto.getScientificArea(),
                dto.getAuthors(),
                dto.getFilename(),
                fileType,
                uploader.getId()
        );

        return Response.status(Response.Status.CREATED)
                .entity(id)
                .build();
    }

    // EP-13
    @POST
    @Path("/{id}/ratings")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response rate(@PathParam("id") Long id,
                         RatingDTO body,
                         @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        int stars = body != null ? body.getStars() : 0;
        if (stars < 1 || stars > 5) {
            throw new BadRequestException("stars deve ser entre 1 e 5");
        }

        Rating rating = publicationBean.createOrUpdateRating(user, p, stars);
        RatingDTO dto = RatingDTO.from(rating);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    // EP-15 (Estatísticas Públicas)
    @GET
    @Path("/{id}/ratings")
    @PermitAll
    public Response ratingStats(@PathParam("id") Long id,
                                @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        User current = null;
        if (sc.getUserPrincipal() != null) {
            current = userBean.find(sc.getUserPrincipal().getName());
        }

        RatingDTO dto = publicationBean.ratingStatsForPublication(p, current);
        return Response.ok(dto).build();
    }

    // --- NOVO ENDPOINT: Buscar o meu voto (Necessário para o frontend) ---
    @GET
    @Path("/{id}/ratings/my")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response getMyRating(@PathParam("id") Long id, @Context SecurityContext sc) {
        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        
        Rating rating = publicationBean.findRatingByUserAndPublication(user, p);
        
        RatingDTO dto = new RatingDTO();
        // Se rating for null (ainda não votou), devolve 0
        dto.setStars(rating != null ? rating.getStars() : 0);
        
        return Response.ok(dto).build();
    }
    // ---------------------------------------------------------------------

    @DELETE
    @Path("/{id}/ratings")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response deleteMyRating(@PathParam("id") Long id,
                                   @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        publicationBean.deleteRating(user, p);
        return Response.ok().build();
    }

    @GET
    @Path("/my")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response myPublications(@Context SecurityContext sc) {

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            throw new UnauthorizedException("User not found");
        }

        List<Publication> data = publicationBean.findByUploader(user);
        List<PublicationDTO> dtos = PublicationDTO.from(data);

        return Response.ok(dtos).build();
    }

    @PUT
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response update(@PathParam("id") Long id,
                           PublicationDTO body,
                           @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User editor = userBean.find(username);
        if (editor == null) {
            throw new UnauthorizedException("User not found");
        }

        boolean isOwner = p.getUploadedBy().getId().equals(editor.getId());
        boolean isManagerOrAdmin = sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");
        if (!isOwner && !isManagerOrAdmin) {
            throw new ForbiddenException("Not allowed to edit this publication");
        }

        if (body == null || body.getTitle() == null || body.getAuthors() == null
                || body.getScientificArea() == null) {
            throw new BadRequestException("title, authors e scientificArea são obrigatórios");
        }

        Publication updated = publicationBean.updateMetadata(
                id,
                body.getTitle(),
                body.getSummary(),
                body.getScientificArea(),
                body.getAuthors(),
                editor
        );

        PublicationDTO dto = publicationBean.toDetailedDTO(updated);
        return Response.ok(dto).build();
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("title") String title,
                           @QueryParam("authors") String authors,
                           @QueryParam("scientificArea") String scientificArea,
                           @QueryParam("tag") String tagName) {

        List<Publication> result;

        if (tagName != null && !tagName.isBlank()) {
            result = publicationBean.searchByTagName(tagName);
        } else {
            result = publicationBean.searchByText(title, authors, scientificArea);
        }

        var dtos = result.stream()
                .map(PublicationDTO::from)
                .toList();

        return Response.ok(dtos).build();
    }

    // EP-11
    @GET
    @Path("/{id}/download")
    @Authenticated
    public Response download(@PathParam("id") Long id,
                             @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        boolean isOwner = p.getUploadedBy() != null &&
                p.getUploadedBy().getId().equals(user.getId());
        boolean isManagerOrAdmin = sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");

        if (!p.isVisible() && !isOwner && !isManagerOrAdmin) {
            throw new ForbiddenException("Not allowed to download this publication");
        }

        java.nio.file.Path path = java.nio.file.Paths.get(
                configBean.getUploadsDir(),
                p.getFilename()
        );

        System.out.println("DOWNLOAD PATH = " + path.toAbsolutePath());

        if (!java.nio.file.Files.exists(path)) {
            throw new EntityNotFoundException("File not found at " + path.toAbsolutePath());
        }

        java.io.InputStream in;
        try {
            in = java.nio.file.Files.newInputStream(path);
        } catch (java.io.IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        String contentType = (p.getFileType() == FileType.PDF)
                ? "application/pdf"
                : "application/zip";

        // Incrementa o download na BD
        publicationBean.incrementDownloadCount(id);

        return Response.ok(in)
                .type(contentType)
                .header("Content-Disposition",
                        "attachment; filename=\"" + p.getFilename() + "\"")
                .build();
    }

    public static class VisibilityDTO {
        public Boolean visible;
        public String reason;
    }

    @PATCH
    @Path("/{id}/visibility")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeVisibility(@PathParam("id") Long id,
                                     VisibilityDTO body,
                                     @Context SecurityContext sc) {

        if (body == null || body.visible == null) {
            throw new BadRequestException("visible é obrigatório");
        }

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User editor = userBean.find(username);

        publicationBean.updateMetadata(
                p.getId(),
                p.getTitle(),
                p.getSummary(),
                p.getScientificArea(),
                p.getAuthors(),
                editor
        );
        p.changeVisibility(body.visible, body.reason);

        PublicationDTO dto = publicationBean.toDetailedDTO(p);
        return Response.ok(dto).build();
    }

    public static class StarsDTO {
        public Integer stars;
    }

    @PUT
    @Path("/{id}/ratings/me")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMyRating(@PathParam("id") Long id,
                                   StarsDTO body,
                                   @Context SecurityContext sc) {

        if (body == null || body.stars == null ||
                body.stars < 1 || body.stars > 5) {
            throw new BadRequestException("stars deve estar entre 1 e 5");
        }

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        Rating rating = publicationBean.createOrUpdateRating(user, p, body.stars);

        RatingDTO dto = publicationBean.ratingStatsForPublication(p, user);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/{id}/comments")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response createComment(@PathParam("id") Long id,
                                  CommentDTO body,
                                  @Context SecurityContext sc) {

        if (body == null || body.getContent() == null || body.getContent().isBlank()) {
            throw new BadRequestException("content é obrigatório");
        }

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User author = userBean.find(username);
        if (author == null) {
            throw new UnauthorizedException("User not found");
        }

        Comment c = publicationBean.createComment(author, p, body.getContent());
        CommentDTO dto = CommentDTO.from(c);

        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/{id}/comments")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response listComments(@PathParam("id") Long id,
                                 @QueryParam("includeHidden") @DefaultValue("false") boolean includeHidden,
                                 @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        boolean isManagerOrAdmin = sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");
        boolean finalIncludeHidden = includeHidden && isManagerOrAdmin;

        List<Comment> comments = publicationBean.commentsOfPublication(p, finalIncludeHidden);
        List<CommentDTO> dtos = CommentDTO.from(comments);
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}/history")
    @RolesAllowed({"CONTRIBUTOR","MANAGER","ADMIN"})
    public Response history(@PathParam("id") Long id) {

        var history = publicationBean.findHistory(id); 
        var dtos = PublicationHistoryDTO.from(history);

        Map<String, Object> resp = new HashMap<>();
        resp.put("publicationId", id);
        resp.put("editHistory", dtos);
        resp.put("totalEdits", dtos.size());

        return Response.ok(resp).build();
    }

    public static class SummaryRequestDTO {
        public String language;
        public Integer maxLength;
    }

    public static class AnalyzeRequestDTO {
        public String analysisType;
        public Map<String, Object> parameters;
    }

    @POST
    @Path("/{id}/generate-summary")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response generateSummary(@PathParam("id") Long id,
                                    SummaryRequestDTO body,
                                    @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String language = body != null && body.language != null ? body.language : "pt";
        int maxLength = body != null && body.maxLength != null ? body.maxLength : 500;

        String summary = publicationBean.generateAutomaticSummary(p, language, maxLength);

        java.time.Instant now = java.time.Instant.now();

        var resp = new java.util.HashMap<String, Object>();
        resp.put("publicationId", id);
        resp.put("summary", summary);
        resp.put("generatedAt", now.toString());
        resp.put("language", language);
        resp.put("status", "success");

        return Response.ok(resp).build();
    }

    @POST
    @Path("/{id}/analyze")
    @Authenticated
    public Response analyze(@PathParam("id") Long id,
                            AnalyzeRequestDTO body) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        if (body == null || body.analysisType == null) {
            throw new BadRequestException("analysisType é obrigatório");
        }

        Map<String, Object> results =
                publicationBean.analyzePublication(p, body.analysisType, body.parameters);

        java.time.Instant now = java.time.Instant.now();

        var resp = new java.util.HashMap<String, Object>();
        resp.put("publicationId", id);
        resp.put("analysisType", body.analysisType);
        resp.put("results", results);
        resp.put("generatedAt", now.toString());

        return Response.ok(resp).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response createPublicationWithPdf(MultipartFormDataInput input, @Context SecurityContext sc) {
        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

            String title = getValue(uploadForm, "title");
            String summary = getValue(uploadForm, "summary");
            String scientificArea = getValue(uploadForm, "scientificArea");
            String authors = getValue(uploadForm, "authors");

            if (title == null || title.isBlank()) {
                throw new BadRequestException("O título é obrigatório");
            }

            String username = sc.getUserPrincipal().getName();
            User uploader = userBean.find(username);
            if (uploader == null) {
                throw new UnauthorizedException("User not found");
            }

            List<InputPart> inputParts = uploadForm.get("file");
            String filename = null;
            FileType fileType = FileType.PDF; 

            if (inputParts != null && !inputParts.isEmpty()) {
                InputPart filePart = inputParts.get(0);
                String originalName = getFileName(filePart);
                
                if (originalName != null && originalName.toLowerCase().endsWith(".zip")) {
                    fileType = FileType.ZIP;
                }
                
                String extension = (fileType == FileType.ZIP) ? ".zip" : ".pdf";
                filename = UUID.randomUUID().toString() + extension;
                
                InputStream inputStream = filePart.getBody(InputStream.class, null);
                writeFile(inputStream, filename);
            } else {
                 throw new BadRequestException("O ficheiro é obrigatório");
            }

            Long id = publicationBean.create(
                    title,
                    summary,
                    scientificArea,
                    authors,
                    filename,
                    fileType,
                    uploader.getId()
            );

            return Response.status(Response.Status.CREATED)
                    .entity(id)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(500).entity("Erro ao processar ficheiro: " + e.getMessage()).build();
        }
    }

    private String getValue(Map<String, List<InputPart>> uploadForm, String key) throws IOException {
        List<InputPart> parts = uploadForm.get(key);
        if (parts != null && !parts.isEmpty()) {
            return parts.get(0).getBody(String.class, null);
        }
        return null;
    }

    private void writeFile(InputStream inputStream, String fileName) throws IOException {
        String uploadDir = configBean.getUploadsDir(); 
        
        File customDir = new File(uploadDir);
        if (!customDir.exists()) {
            customDir.mkdirs();
        }
        
        File file = new File(customDir, fileName);
        FileOutputStream fop = new FileOutputStream(file);

        byte[] content = new byte[1024];
        int len;
        while ((len = inputStream.read(content)) != -1) {
            fop.write(content, 0, len);
        }
        
        fop.flush();
        fop.close();
    }

    private String getFileName(InputPart part) {
        try {
            MultivaluedMap<String, String> header = part.getHeaders();
            String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
            for (String filename : contentDisposition) {
                if ((filename.trim().startsWith("filename"))) {
                    String[] name = filename.split("=");
                    String finalName = name[1].trim().replaceAll("\"", "");
                    return finalName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown.pdf";
    }
}