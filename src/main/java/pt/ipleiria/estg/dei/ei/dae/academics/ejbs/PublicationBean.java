package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.*;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;

// --- Imports Adicionados para IA ---
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// -----------------------------------

@Stateless
public class PublicationBean {

    @PersistenceContext
    private EntityManager em;

    @jakarta.ejb.EJB
    private ActivityBean activityBean;

    // --- Configuração OLLAMA ---
    private static final String OLLAMA_API_URL = "http://ollama:11434/api/generate";
    private static final String MODEL = "llama3";
    // ---------------------------

    // ===== métodos base =====
    public List<Publication> getAllVisible() {
        return em.createQuery(
                "SELECT p FROM Publication p WHERE p.visible = true",
                Publication.class
        ).getResultList();
    }

    public List<Publication> list(int page, int size) {
        // Default behavior: only visible
        return list(page, size, false);
    }

    public List<Publication> list(int page, int size, boolean includeHidden) {
        String jpql = "SELECT p FROM Publication p";
        if (!includeHidden) {
            jpql += " WHERE p.visible = true";
        }
        jpql += " ORDER BY p.createdAt DESC";

        return em.createQuery(jpql, Publication.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public Publication find(Long id) {
        return em.find(Publication.class, id);
    }

    public Long create(String title,
                       String summary,
                       String scientificArea,
                       String authors,
                       String filename,
                       FileType fileType,
                       Long uploaderId) {

        User uploader = em.find(User.class, uploaderId);
        if (uploader == null) {
            throw new IllegalArgumentException("User not found with id " + uploaderId);
        }

        Publication p = new Publication();
        p.setTitle(title);
        p.setSummary(summary);
        p.setScientificArea(scientificArea);
        p.setAuthors(authors);
        p.setVisible(true);
        p.setVisibilityReason(null);
        p.setFilename(filename);
        p.setFileType(fileType);
        p.setDownloadCount(0);
        p.setCreatedAt(new Date());
        p.setUpdatedAt(new Date());
        p.setUploadedBy(uploader);

        em.persist(p);
        em.flush();

        activityBean.create(uploader, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.UPLOAD,
            "Criada publicação: " + title, "Publication", p.getId());

        return p.getId();
    }

    // ===== métodos de estatísticas =====

    public long countComments(Publication p) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM Comment c WHERE c.publication = :p", Long.class)
                .setParameter("p", p)
                .getSingleResult();
    }

    public long countRatings(Publication p) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM Rating r WHERE r.publication = :p", Long.class)
                .setParameter("p", p)
                .getSingleResult();
    }

    public double averageRating(Publication p) {
        Double avg = em.createQuery(
                        "SELECT AVG(r.stars) FROM Rating r WHERE r.publication = :p", Double.class)
                .setParameter("p", p)
                .getSingleResult();
        return avg != null ? avg : 0.0;
    }

    public List<pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO> tags(Publication p) {
        List<Tag> tags = em.createQuery(
                        "SELECT t FROM Tag t JOIN t.publications pub WHERE pub = :p", Tag.class)
                .setParameter("p", p)
                .getResultList();
        return pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO.from(tags);
    }

    // DTO detalhado para uma publicação
    public PublicationDTO toDetailedDTO(Publication p) {
        if (p == null) return null;

        PublicationDTO dto = PublicationDTO.from(p);
        dto.setCommentCount((int) countComments(p));
        dto.setRatingsCount((int) countRatings(p));
        dto.setAverageRating(averageRating(p));
        dto.setTags(tags(p));
        return dto;
    }

    public Rating findRatingByUserAndPublication(User user, Publication publication) {
        List<Rating> ratings = em.createQuery(
                        "SELECT r FROM Rating r WHERE r.user = :u AND r.publication = :p",
                        Rating.class)
                .setParameter("u", user)
                .setParameter("p", publication)
                .getResultList();
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    public Rating createOrUpdateRating(User user, Publication publication, int stars) {
        Rating rating = findRatingByUserAndPublication(user, publication);
        if (rating == null) {
            rating = new Rating();
            rating.setUser(user);
            rating.setPublication(publication);
            rating.setCreatedAt(new Date());
        }
        rating.setStars(stars);           // definir antes de persistir
        rating.setUpdatedAt(new Date());


        boolean isNew = (rating.getId() == null);

        if (isNew) {
            em.persist(rating);
        } else {
            rating = em.merge(rating);
        }
        em.flush();

        if (isNew) {
             activityBean.create(user, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.RATING,
                "Avaliou com " + rating.getStars() + " estrelas: " + publication.getTitle(), "Rating", rating.getId());
        }

        em.flush();
        return rating;
    }

    public void deleteRating(User user, Publication publication) {
        Rating rating = findRatingByUserAndPublication(user, publication);
        if (rating != null) {
            activityBean.create(user, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.DELETE,
                "Apagou a avaliação da publicação: " + publication.getTitle(), "Rating", rating.getId());
            em.remove(rating);
        }
    }

    public List<Rating> ratingsOfPublication(Publication publication) {
        return em.createQuery(
                        "SELECT r FROM Rating r WHERE r.publication = :p ORDER BY r.createdAt DESC",
                        Rating.class)
                .setParameter("p", publication)
                .getResultList();
    }

    public RatingDTO ratingStatsForPublication(Publication publication, User currentUser) {
        List<Rating> ratings = ratingsOfPublication(publication);

        RatingDTO dto = new RatingDTO();
        dto.setPublicationId(publication.getId());

        // média e contagem
        if (!ratings.isEmpty()) {
            double avg = ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0);
            dto.setAverage(avg);
            dto.setCount(ratings.size());

            // distribuição 1-5
            Map<Integer, Integer> distribution = new java.util.HashMap<>();
            for (int i = 1; i <= 5; i++) distribution.put(i, 0);
            ratings.forEach(r -> distribution.put(
                    r.getStars(),
                    distribution.get(r.getStars()) + 1
            ));
            dto.setDistribution(distribution);
        } else {
            dto.setAverage(0.0);
            dto.setCount(0);
            dto.setDistribution(java.util.Collections.emptyMap());
        }

        // rating do utilizador autenticado
        if (currentUser != null) {
            Rating my = findRatingByUserAndPublication(currentUser, publication);
            dto.setUserRating(my != null ? my.getStars() : null);
        }

        return dto;
    }
    // ===== EP07 - Publicações do próprio =====
    public List<Publication> findByUploader(User uploader) {
        return em.createNamedQuery("Publication.findByUploader", Publication.class)
                .setParameter("uploader", uploader)
                .getResultList();
    }
    // ===== EP08 - Editar metadados =====
    public Publication updateMetadata(Long id, String title, String summary,
                                      String scientificArea, String authors, 
                                      List<String> tagNames, User editor) {

        Publication p = em.find(Publication.class, id);
        if (p == null) return null;

        // título
        if (title != null && !title.equals(p.getTitle())) {
            registerHistory(p, "title", p.getTitle(), title, editor);
            p.setTitle(title);
        }

        // resumo
        if (summary != null && !summary.equals(p.getSummary())) {
            registerHistory(p, "summary", p.getSummary(), summary, editor);
            p.setSummary(summary);
        }

        // área científica
        if (scientificArea != null && !scientificArea.equals(p.getScientificArea())) {
            registerHistory(p, "scientificArea", p.getScientificArea(), scientificArea, editor);
            p.setScientificArea(scientificArea);
        }

        // autores
        if (authors != null && !authors.equals(p.getAuthors())) {
            registerHistory(p, "authors", p.getAuthors(), authors, editor);
            p.setAuthors(authors);
        }

        // tags (se a lista não for nula, atualizamos. Lista vazia remove todas)
        if (tagNames != null) {
            // 1. Limpar tags atuais que não estão na nova lista
            // (Para simplificar, limpamos tudo e re-adicionamos, ou fazemos diff. 
            //  O "p.getTags().clear()" pode ser bruto, melhor diff.)
            
            // Vamos usar uma abordagem simples: setTags com as novas.
            // Precisamos resolver os nomes para Entidades Tag
            
            p.getTags().clear(); // Remove associações atuais
            
            for (String name : tagNames) {
                String safeName = name.trim();
                if (!safeName.isEmpty()) {
                    Tag t = tagBean.findByName(safeName);
                    if (t == null) {
                        t = tagBean.create(safeName, editor);
                    }
                    if (!p.getTags().contains(t)) {
                        p.getTags().add(t);
                    }
                }
            }
            // Nota: Não registamos histórico de tags individualmente nesta versão simples
        }

        if (p.getUploadedBy() != null) {
             activityBean.create(editor, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.EDIT,
                "Editou a publicação: " + p.getTitle(), "Publication", p.getId());
        }
        
        p.edit(); // atualiza updatedAt
        return p;
    }

    public void update(Publication p) {
        em.merge(p);
        em.flush();
    }

    public void updateFile(Long id, String filename, FileType fileType) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            p.setFilename(filename);
            p.setFileType(fileType);
            p.edit();
        }
    }

    // auxiliar para gravar uma linha na tabela publication_history
    private void registerHistory(Publication p, String field, String oldValue,
                                 String newValue, User editor) {

        PublicationHistory h = new PublicationHistory();
        h.setPublication(p);
        h.setFieldChanged(field);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setChangedBy(editor);
        h.changed(); // define changedAt = new Date()
        em.persist(h);
    }
    // title / authors / scientificArea (LIKE, case-insensitive)
    public List<Publication> searchByText(String title, String authors, String scientificArea) {
        String jpql = "SELECT p FROM Publication p WHERE 1=1";

        if (title != null && !title.isBlank())
            jpql += " AND LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))";

        if (authors != null && !authors.isBlank())
            jpql += " AND LOWER(p.authors) LIKE LOWER(CONCAT('%', :authors, '%'))";

        if (scientificArea != null && !scientificArea.isBlank())
            jpql += " AND LOWER(p.scientificArea) LIKE LOWER(CONCAT('%', :area, '%'))";

        TypedQuery<Publication> q = em.createQuery(jpql, Publication.class);

        if (title != null && !title.isBlank()) q.setParameter("title", title);
        if (authors != null && !authors.isBlank()) q.setParameter("authors", authors);
        if (scientificArea != null && !scientificArea.isBlank()) q.setParameter("area", scientificArea);

        return q.getResultList();
    }

    public List<PublicationDTO> sortPublications(List<PublicationDTO> list,
                                                 String sortBy, String order) {
        if (sortBy == null || sortBy.isBlank()) return list;

        boolean asc = "asc".equalsIgnoreCase(order);

        Comparator<PublicationDTO> cmp;
        switch (sortBy) {
            case "rating" -> cmp = Comparator.comparingDouble(PublicationDTO::getAverageRating);
            case "comments" -> cmp = Comparator.comparingInt(PublicationDTO::getCommentCount);
            case "ratingsCount" -> cmp = Comparator.comparingInt(PublicationDTO::getRatingsCount);
            case "date" -> cmp = Comparator.comparing(PublicationDTO::getCreatedAt);
            default -> { return list; }
        }

        if (!asc) cmp = cmp.reversed();

        return list.stream().sorted(cmp).toList();
    }



    // por tag (nome)
    public List<Publication> searchByTagName(String tagName) {
        return em.createQuery(
                        "SELECT DISTINCT p FROM Publication p JOIN p.tags t " +
                                "WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :tag))",
                        Publication.class
                ).setParameter("tag", tagName)
                .getResultList();
    }

    @jakarta.ejb.EJB
    private TagBean tagBean;

    @jakarta.ejb.EJB
    private pt.ipleiria.estg.dei.ei.dae.academics.ejbs.NotificationBean notificationBean;

    public Comment createComment(User author, Publication publication, String content) {
        if (author == null || publication == null) {
            throw new IllegalArgumentException("author/publication cannot be null");
        }
        Comment c = new Comment();
        c.setAuthor(author);
        c.setPublication(publication);
        c.setContent(content);
        c.setVisible(true);
        c.setVisibleReason(null);
        c.setCreatedAt(new Date());
        c.setUpdatedAt(new Date());
        em.persist(c);
        em.flush();

        activityBean.create(author, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.COMMENT,
            "Comentou: \"" + content + "\" em " + publication.getTitle(), "Comment", c.getId());

        // Notificar o autor da publicação (se não for o próprio)
        if (publication.getUploadedBy() != null && !publication.getUploadedBy().getUsername().equals(author.getUsername())) {
            notificationBean.create(
                publication.getUploadedBy(),
                "Novo Comentário",
                author.getUsername() + " comentou a sua publicação: \"" + publication.getTitle() + "\"",
                pt.ipleiria.estg.dei.ei.dae.academics.enums.NotificationType.NEW_COMMENT,
                c.getId(),
                "Comment"
            );
        }

        em.flush();

        // Notificar subscritores das tags (Cenário 3)
        try {
            // Recarregar publicação para garantir acesso às tags
            Publication p = em.find(Publication.class, publication.getId());
            if (p != null) {
                for (Tag t : p.getTags()) {
                    tagBean.notifyCommentSubscribers(t, p, c);
                }
            }
        } catch (Exception e) {
            // Não deve bloquear a criação do comentário
            e.printStackTrace();
        }

        return c;
    }

    public List<Comment> commentsOfPublication(Publication publication, boolean includeHidden) {
        String jpql = "SELECT c FROM Comment c " +
                "JOIN FETCH c.author " +
                "WHERE c.publication = :p";

        if (!includeHidden) {
            jpql += " AND c.visible = true";
        }

        jpql += " ORDER BY c.createdAt ASC";

        return em.createQuery(jpql, Comment.class)
                .setParameter("p", publication)
                .getResultList();
    }



    public List<PublicationHistory> findHistory(Long publicationId) {
        return em.createQuery(
                        "SELECT h FROM PublicationHistory h " +
                                "WHERE h.publication.id = :pid " +
                                "ORDER BY h.changedAt DESC",   // <-- usa o nome real do campo de data
                        PublicationHistory.class)
                .setParameter("pid", publicationId)
                .getResultList();
    }

    // ===== INTEGRAÇÃO IA (OLLAMA) IMPLEMENTADA =====
    // Versão sobrecarregada para aceitar dados "crus" (antes de criar a entidade)
    public String generateAutomaticSummary(String title, String authors, String scientificArea, String currentSummary, String language, int maxLength) {
        try {
            // Constroi o prompt
            String promptText = String.format(
                "Summarize the following scientific publication info in %s (max %d chars). " +
                "Title: %s. Authors: %s. Area: %s. " +
                "Existing Summary: %s. " + 
                "Return ONLY the summary text, nothing else.",
                language, maxLength, title, authors, scientificArea, 
                currentSummary != null ? currentSummary : "None"
            );

            // Escapar JSON
            String escapedPrompt = promptText.replace("\"", "\\\"").replace("\n", " ");
            String jsonBody = String.format("{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}", MODEL, escapedPrompt);

            // Cliente HTTP
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Regex simples para capturar o campo "response" do JSON do Ollama
                Pattern pattern = Pattern.compile("\"response\"\\s*:\\s*\"(.*?)\",\\s*\"done\"", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(body);
                
                if (matcher.find()) {
                    String rawResponse = matcher.group(1);
                    return rawResponse.replace("\\n", "\n").replace("\\\"", "\"").replace("\\t", "\t");
                }
                return "Erro ao processar resposta da IA.";
            } else {
                return "Erro na API IA: Código " + response.statusCode();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Falha na geração de resumo: " + e.getMessage() + ". Verifique se o Ollama está a correr.";
        }
    }

    public String generateAutomaticSummary(Publication p, String language, int maxLength) {
        return generateAutomaticSummary(p.getTitle(), p.getAuthors(), p.getScientificArea(), p.getSummary(), language, maxLength);
    }

    public Map<String, Object> analyzePublication(Publication p,
                                                  String analysisType,
                                                  Map<String, Object> parameters) {

        Map<String, Object> results = new java.util.HashMap<>();

        if ("keywords".equalsIgnoreCase(analysisType)) {
            int max = 10;
            if (parameters != null && parameters.get("maxKeywords") instanceof Number n) {
                max = n.intValue();
            }

            // mock simples: usar palavras do título como "keywords"
            java.util.List<Map<String, Object>> keywords = new java.util.ArrayList<>();

            if (p.getTitle() != null) {
                String[] parts = p.getTitle().split("\\s+");
                for (int i = 0; i < parts.length && i < max; i++) {
                    Map<String, Object> k = new java.util.HashMap<>();
                    k.put("term", parts[i]);
                    k.put("relevance", 1.0 - (i * 0.1)); // valores fictícios
                    keywords.add(k);
                }
            }

            results.put("keywords", keywords);
        } else {
            // outros tipos de análise futuros
            results.put("info", "analysisType não suportado nesta demo");
        }

        return results;
    }

    public void associateTag(Long publicationId, Long tagId) {
        Publication p = em.find(Publication.class, publicationId);
        Tag t = em.find(Tag.class, tagId);
        
        if (p != null && t != null) {
            if (!p.getTags().contains(t)) {
                p.getTags().add(t);
                em.merge(p);
                em.flush(); // Force write
            }
        }
    }

    public void removeTag(Long publicationId, Long tagId) {
        Publication p = em.find(Publication.class, publicationId);
        Tag t = em.find(Tag.class, tagId);
        
        if (p != null && t != null) {
            p.removeTag(t);
            em.merge(p);
            em.flush();
        }
    }



    // Este método permite atualizar o contador dentro de uma transação
    public void incrementDownloadCount(Long id) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            p.incrementDownloadCount();
        }
    }

    public void delete(Long id, User performedBy) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            // Log before delete
             if (performedBy != null) {
                activityBean.create(performedBy, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.DELETE,
                    "Apagou a publicação: " + p.getTitle(), "Publication", p.getId()); // ID might be lost in DB but activity prevails? Activity has entityId but if entity is gone... history remains.
            }

            // Limpa as tags associadas (remove da tabela de junção)
            p.getTags().clear(); // Atualiza a relação ManyToMany

            // Apaga manualmente para garantir que não há erro de FK (Cascade pode falhar se não-carregado)
            // History
            em.createQuery("DELETE FROM PublicationHistory h WHERE h.publication.id = :pid")
              .setParameter("pid", id).executeUpdate();
            
            // Comments
            em.createQuery("DELETE FROM Comment c WHERE c.publication.id = :pid")
              .setParameter("pid", id).executeUpdate();

            // Ratings
            em.createQuery("DELETE FROM Rating r WHERE r.publication.id = :pid")
              .setParameter("pid", id).executeUpdate();

            em.remove(p);
        }
    }
    
    public void changeVisibility(Long id, boolean visible, User performedBy) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            p.setVisible(visible);
            // p.setVisibilityReason(reason); // Simplified
            p.setUpdatedAt(new Date());
            em.merge(p);
            
             activityBean.create(performedBy, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.VISIBILITY,
                (visible ? "Mostrou" : "Ocultou") + " a publicação: " + p.getTitle(), "Publication", p.getId());
        }
    }

    // --- Rating Management ---
    public Rating findRating(Long id) {
        return em.find(Rating.class, id);
    }

    public List<Rating> getRatings(Long publicationId) {
        return em.createQuery(
            "SELECT r FROM Rating r WHERE r.publication.id = :pubId ORDER BY r.createdAt DESC", 
            Rating.class)
            .setParameter("pubId", publicationId)
            .getResultList();
    }

    public void deleteRating(Long ratingId, User performedBy) {
        Rating r = findRating(ratingId);
        if (r != null) {
            activityBean.create(performedBy, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.DELETE,
                "Apagou a avaliação da publicação: " + r.getPublication().getTitle(), "Rating", r.getId());
            
            // Se estiver numa relação bidirecional com Publication, remove da lista lá
            if (r.getPublication() != null) {
                r.getPublication().getRatings().remove(r);
            }
            em.remove(r);
        }
    }

    public void deleteAllRatings(Long publicationId, User performedBy) {
        // Logging bulk action effectively:
        Publication p = find(publicationId);
        if (p != null) {
            activityBean.create(performedBy, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.DELETE,
                "Apagou todas as avaliações da publicação: " + p.getTitle(), "Publication", p.getId());
                
            p.getRatings().clear();
        }

        // Usa query para apagar tudo de uma vez
        em.createQuery("DELETE FROM Rating r WHERE r.publication.id = :pubId")
          .setParameter("pubId", publicationId)
          .executeUpdate();
    }
}