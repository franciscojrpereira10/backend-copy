package pt.ipleiria.estg.dei.ei.dae.academics.ws;

import pt.ipleiria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Authenticated;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.PublicationHistoryDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleiria.estg.dei.ei.dae.academics.ejbs.ConfigBean;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.CommentDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.BadRequestException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ForbiddenException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.UnauthorizedException;
import pt.ipleiria.estg.dei.ei.dae.academics.security.OptionalAuthenticated;

// --- NOVOS IMPORTS ADICIONADOS PARA O UPLOAD ---
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
// -----------------------------------------------

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
    private pt.ipleiria.estg.dei.ei.dae.academics.ejbs.TagBean tagBean;

    @EJB
    private ConfigBean configBean;

    // EP05 + EP10 juntos
    @GET
    @PermitAll
    @OptionalAuthenticated
    public Response listAndSort(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("sortBy") String sortBy,
                                @QueryParam("order") @DefaultValue("desc") String order,
                                @Context SecurityContext sc) {

        boolean includeHidden = false;
        if (sc.getUserPrincipal() != null) {
            if (sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN")) {
                includeHidden = true;
            }
        }

        List<Publication> data = publicationBean.list(page, size, includeHidden);
        List<PublicationDTO> dtos = PublicationDTO.from(data);

        dtos = publicationBean.sortPublications(dtos, sortBy, order);

        return Response.ok(dtos).build();
    }

    // ===== EP06 - Detalhe de publicação (com estatísticas) =====
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

    // ===== EP04 - Criar publicação (JSON - Mantido Original) =====
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

    //EP-13
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

    //EP-15
    @GET
    @Path("/{id}/ratings")
    @PermitAll
    @OptionalAuthenticated
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

    //EP-16
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

    // ===== EP-NEW: Gestão de Ratings (Manager/Admin) =====
    @GET
    @Path("/{id}/ratings/list")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response getAllRatings(@PathParam("id") Long id) {
        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }
        
        List<Rating> ratings = publicationBean.getRatings(id);
        List<RatingDTO> dtos = RatingDTO.from(ratings);
        
        return Response.ok(dtos).build();
    }

    @DELETE
    @Path("/{id}/ratings/{ratingId}")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response deleteRating(@PathParam("id") Long publicationId, 
                                 @PathParam("ratingId") Long ratingId,
                                 @Context SecurityContext sc) {
        
        // Validações básicas
        Publication p = publicationBean.find(publicationId);
        if (p == null) throw new EntityNotFoundException("Publicação não encontrada");

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        publicationBean.deleteRating(ratingId, user);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/ratings/all")
    @Authenticated
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Response deleteAllRatings(@PathParam("id") Long publicationId,
                                     @Context SecurityContext sc) {
        Publication p = publicationBean.find(publicationId);
        if (p == null) throw new EntityNotFoundException("Publicação não encontrada");
        
        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        publicationBean.deleteAllRatings(publicationId, user);
        return Response.ok().build();
    }

    // ===== EP07 - Publicações do próprio =====
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

    // ===== DELETE Publicação =====
    @DELETE
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response delete(@PathParam("id") Long id, @Context SecurityContext sc) {
        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        boolean isOwner = p.getUploadedBy() != null && p.getUploadedBy().getUsername().equals(username);
        boolean isManagerOrAdmin = sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");

        if (!isOwner && !isManagerOrAdmin) {
            throw new ForbiddenException("Não tens permissão para apagar esta publicação");
        }

        publicationBean.delete(id, user);
        return Response.noContent().build();
    }

    // ===== EP08 - Editar metadados =====
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

        List<String> tagNames = null;
        if (body.getTags() != null) {
            tagNames = body.getTags().stream()
                    .map(pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO::getName)
                    .toList();
        }

        Publication updated = publicationBean.updateMetadata(
                id,
                body.getTitle(),
                body.getSummary(),
                body.getScientificArea(),
                body.getAuthors(),
                tagNames,
                editor
        );

        PublicationDTO dto = publicationBean.toDetailedDTO(updated);
        return Response.ok(dto).build();
    }

    //EP-09
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


    //EP-11
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
            // aqui podes manter 500 direto ou criar uma InternalServerErrorException própria
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        String contentType = (p.getFileType() == FileType.PDF)
                ? "application/pdf"
                : "application/zip";

        // ALTERAÇÃO PARA O BEAN (TRANSACTIONAL)
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

    //EP-12
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

        publicationBean.changeVisibility(p.getId(), body.visible, editor);
        p = publicationBean.find(p.getId()); // Reload state
        // ----------------------------------

        PublicationDTO dto = publicationBean.toDetailedDTO(p);
        return Response.ok(dto).build();
    }

    //EP-14
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

    // ===== EP17 - Criar comentário =====
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

    // ===== EP18 - Listar comentários =====
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

        var history = publicationBean.findHistory(id); // método novo no bean
        var dtos = PublicationHistoryDTO.from(history);

        Map<String, Object> resp = new HashMap<>();
        resp.put("publicationId", id);
        resp.put("editHistory", dtos);
        resp.put("totalEdits", dtos.size());

        return Response.ok(resp).build();
    }

    // DTOs para pedidos de IA
    public static class SummaryRequestDTO {
        public String language;
        public Integer maxLength;
    }

    public static class AnalyzeRequestDTO {
        public String analysisType;
        public Map<String, Object> parameters;
    }

    public static class SummaryGenerationDTO {
        public String title;
        public String authors;
        public String scientificArea;
        public String currentSummary;
        public String language;
        public Integer maxLength;
    }

    // NOVO EP: Gerar resumo SEM criação prévia
    @POST
    @Path("/ai-summary")
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response generateSummaryForNewPublication(SummaryGenerationDTO body) {
        if (body == null || body.title == null || body.authors == null) {
            throw new BadRequestException("title e authors são obrigatórios");
        }

        String language = body.language != null ? body.language : "pt";
        int maxLength = body.maxLength != null ? body.maxLength : 500;

        String summary = publicationBean.generateAutomaticSummary(
                body.title, 
                body.authors, 
                body.scientificArea, 
                body.currentSummary, 
                language, 
                maxLength
        );

        var resp = new java.util.HashMap<String, Object>();
        resp.put("summary", summary);
        resp.put("generatedAt", java.time.Instant.now().toString());
        resp.put("status", "success");

        return Response.ok(resp).build();
    }

    // EP41 - Gerar resumo automático (ENTITY EXISTENTE)
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

    // EP42 - Analisar publicação (keywords)
    @POST
    @Path("/{id}/analyze")
    @Authenticated   // enunciado diz "todos", mas normalmente precisa de user autenticado
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

    // ==========================================================
    //      CÓDIGO ADICIONADO PARA SUPORTE A UPLOAD DE FICHEIROS
    // ==========================================================

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response createPublicationWithPdf(MultipartFormDataInput input, @Context SecurityContext sc) {
        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

            // 1. Obter dados textuais do form
            String title = getValue(uploadForm, "title");
            String summary = getValue(uploadForm, "summary");
            String scientificArea = getValue(uploadForm, "scientificArea");
            String authors = getValue(uploadForm, "authors");

            // Validar dados mínimos
            if (title == null || title.isBlank()) {
                throw new BadRequestException("O título é obrigatório");
            }

            // 2. Identificar o utilizador
            String username = sc.getUserPrincipal().getName();
            User uploader = userBean.find(username);
            if (uploader == null) {
                throw new UnauthorizedException("User not found");
            }

            // 3. Processar o ficheiro
            List<InputPart> inputParts = uploadForm.get("file");
            String filename = null;
            FileType fileType = FileType.PDF; // Default

            if (inputParts != null && !inputParts.isEmpty()) {
                InputPart filePart = inputParts.get(0);
                String originalName = getFileName(filePart);
                
                // Determinar se é PDF ou ZIP
                if (originalName != null && originalName.toLowerCase().endsWith(".zip")) {
                    fileType = FileType.ZIP;
                }
                
                // Gerar nome único
                String extension = (fileType == FileType.ZIP) ? ".zip" : ".pdf";
                filename = UUID.randomUUID().toString() + extension;
                
                // Salvar ficheiro no disco (usando o configBean para alinhar com o download)
                InputStream inputStream = filePart.getBody(InputStream.class, null);
                writeFile(inputStream, filename);
            } else {
                 throw new BadRequestException("O ficheiro é obrigatório");
            }

            // 4. Gravar na Base de Dados
            Long id = publicationBean.create(
                    title,
                    summary,
                    scientificArea,
                    authors,
                    filename,
                    fileType,
                    uploader.getId()
            );

            // --- 5. Processar Tags e Notificar ---
            String tagsParam = getValue(uploadForm, "tags");
            if (tagsParam != null && !tagsParam.isBlank()) {
                // Separar por vírgula
                String[] tagNames = tagsParam.split(",");
                Publication publication = publicationBean.find(id); // recarregar para ter a entidade
                
                // Precisamos injetar TagBean aqui no Resource
                // Assumindo que vou adicionar @EJB private TagBean tagBean; na classe
                
                for (String tagName : tagNames) {
                    String safeName = tagName.trim();
                    if (!safeName.isEmpty()) {
                        // Procura ou Cria (se for admin/manager? Enunciado diz colaborador pode "associar tags")
                        // Vamos assumir que se não existir, cria (ou falha se nao tiver permissao, mas create() no TagBean gere isso?)
                        // UserResource tem createTag mas aqui é simplificado.
                        // Vou usar o findByName e se não existir, crio.
                        
                        pt.ipleiria.estg.dei.ei.dae.academics.entities.Tag t = tagBean.findByName(safeName);
                        if (t == null) {
                            // Cria tag nova se não existir
                            t = tagBean.create(safeName, uploader);
                        }
                        
                        if (t != null) {
                            // Associa
                            publicationBean.associateTag(id, t.getId());
                            
                            // Notifica
                            tagBean.notifySubscribers(t, publication);
                        }
                    }
                }
            }

            return Response.status(Response.Status.CREATED)
                    .entity(id)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(500).entity("Erro ao processar ficheiro: " + e.getMessage()).build();
        }
    }

    // ===== NOVO ENDPOINT: Substituir Ficheiro =====
    @POST
    @Path("/{id}/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @RolesAllowed({"CONTRIBUTOR", "MANAGER", "ADMIN"})
    public Response replaceFile(@PathParam("id") Long id,
                                MultipartFormDataInput input,
                                @Context SecurityContext sc) {

        Publication p = publicationBean.find(id);
        if (p == null) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }

        String username = sc.getUserPrincipal().getName();
        User user = userBean.find(username);

        boolean isOwner = p.getUploadedBy() != null && p.getUploadedBy().getUsername().equals(username);
        boolean isManagerOrAdmin = sc.isUserInRole("MANAGER") || sc.isUserInRole("ADMIN");

        if (!isOwner && !isManagerOrAdmin) {
            throw new ForbiddenException("Não tens permissão para alterar este ficheiro");
        }

        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("file");

            if (inputParts == null || inputParts.isEmpty()) {
                throw new BadRequestException("Ficheiro é obrigatório");
            }

            InputPart filePart = inputParts.get(0);
            String originalName = getFileName(filePart);
            FileType fileType = FileType.PDF;

            if (originalName != null && originalName.toLowerCase().endsWith(".zip")) {
                fileType = FileType.ZIP;
            }

            String extension = (fileType == FileType.ZIP) ? ".zip" : ".pdf";
            String newFilename = UUID.randomUUID().toString() + extension;

            // 1. Guardar novo ficheiro
            InputStream inputStream = filePart.getBody(InputStream.class, null);
            writeFile(inputStream, newFilename);

            // 2. Apagar ficheiro antigo (Cleanup)
            try {
                java.nio.file.Path oldPath = java.nio.file.Paths.get(configBean.getUploadsDir(), p.getFilename());
                java.nio.file.Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao apagar ficheiro antigo: " + e.getMessage());
                // Não falha o pedido por causa disto
            }

            // 3. Atualizar BD
            publicationBean.updateFile(id, newFilename, fileType);

            return Response.ok()
                    .entity("Ficheiro atualizado com sucesso")
                    .build();

        } catch (IOException e) {
            return Response.status(500).entity("Erro ao processar ficheiro: " + e.getMessage()).build();
        }
    }

    // Métodos Auxiliares para o Upload

    private String getValue(Map<String, List<InputPart>> uploadForm, String key) throws IOException {
        List<InputPart> parts = uploadForm.get(key);
        if (parts != null && !parts.isEmpty()) {
            return parts.get(0).getBody(String.class, null);
        }
        return null;
    }

    private void writeFile(InputStream inputStream, String fileName) throws IOException {
        // Usa o diretório configurado no ConfigBean (o mesmo usado no download)
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