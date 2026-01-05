package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.EntityNotFoundException;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.FileType;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.persistence.TypedQuery;
import java.util.Comparator;

@Stateless
public class PublicationBean {

    @PersistenceContext
    private EntityManager em;

    // ===== métodos base =====
    public List<Publication> getAllVisible() {
        return em.createQuery(
                "SELECT p FROM Publication p WHERE p.visible = true",
                Publication.class
        ).getResultList();
    }

    public List<Publication> list(int page, int size) {
        return em.createQuery(
                        "SELECT p FROM Publication p WHERE p.visible = true ORDER BY p.createdAt DESC",
                        Publication.class)
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

    public List<String> tagNames(Publication p) {
        return em.createQuery(
                        "SELECT t.name FROM Tag t JOIN t.publications pub WHERE pub = :p", String.class)
                .setParameter("p", p)
                .getResultList();
    }

    // DTO detalhado para uma publicação
    public PublicationDTO toDetailedDTO(Publication p) {
        if (p == null) return null;

        PublicationDTO dto = PublicationDTO.from(p);
        dto.setCommentCount((int) countComments(p));
        dto.setRatingsCount((int) countRatings(p));
        dto.setAverageRating(averageRating(p));
        dto.setTags(tagNames(p));
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

        if (rating.getId() == null) {
            em.persist(rating);
        } else {
            rating = em.merge(rating);
        }

        em.flush();
        return rating;
    }

    public void deleteRating(User user, Publication publication) {
        Rating rating = findRatingByUserAndPublication(user, publication);
        if (rating != null) {
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
                                      String scientificArea, String authors, User editor) {

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

        p.edit(); // atualiza updatedAt
        return p;
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

    public void associateTag(Long publicationId, Long tagId) {
        Publication p = em.find(Publication.class, publicationId);
        if (p == null) {
            throw new EntityNotFoundException("Publication not found with id " + publicationId);
        }

        Tag t = em.find(Tag.class, tagId);
        if (t == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        if (p.getTags().contains(t)) {
            throw new ConflictException("Tag already associated to this publication");
        }

        p.getTags().add(t);
        // se tiveres o lado inverso:
        // t.getPublications().add(p);

        em.merge(p);
    }

    public void removeTag(Long publicationId, Long tagId) {
        Publication p = em.find(Publication.class, publicationId);
        if (p == null) {
            throw new EntityNotFoundException("Publication not found with id " + publicationId);
        }

        Tag t = em.find(Tag.class, tagId);
        if (t == null) {
            throw new EntityNotFoundException("Tag not found with id " + tagId);
        }

        if (!p.getTags().contains(t)) {
            throw new ConflictException("Tag is not associated to this publication");
        }

        p.getTags().remove(t);
        // se manténs o lado inverso:
        // t.getPublications().remove(p);

        em.merge(p);
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

    public String generateAutomaticSummary(Publication p, String language, int maxLength) {
        String base = p.getSummary();
        if (base == null || base.isBlank()) {
            base = "Resumo automático gerado para a publicação \"" + p.getTitle() + "\".";
        }
        if (base.length() > maxLength) {
            base = base.substring(0, Math.max(0, maxLength - 3)) + "...";
        }
        return base;
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

    // Este método permite atualizar o contador dentro de uma transação
    public void incrementDownloadCount(Long id) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            p.incrementDownloadCount();
        }
    }
}