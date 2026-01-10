package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.exceptions.ConflictException;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.SubscriptionInfoDTO;


import java.util.Date;

import java.util.List;

@Stateless
public class TagBean {

    @PersistenceContext
    private EntityManager em;

    public List<Tag> getAllTags() {
        return em.createNamedQuery("getAllTags", Tag.class)
                .getResultList();
    }

    public Tag find(Long id) {
        return em.find(Tag.class, id);
    }

    public Tag findByName(String name) {
        return em.createNamedQuery("getTagByName", Tag.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public Tag create(String name, User creator) {
        Tag existing = findByName(name);
        if (existing != null) {
            return null; // já existe
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setVisible(true);
        tag.setCreatedBy(creator);
        tag.setCreatedAt(new Date());
        em.persist(tag);
        return tag;
    }

    // Método seguro para evitar LazyInitializationException e N+1 queries
    public List<pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO> getAllTagsWithSubscriptionStatus(String username) {
        // 1. Buscar todas as tags
        List<Tag> tags = getAllTags();
        // Converter para DTO
        List<pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO> dtos = pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO.from(tags);
        
        // Mapa para acesso rápido ao DTO por ID
        java.util.Map<Long, pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO> dtoMap = new java.util.HashMap<>();
        for (pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO dto : dtos) {
            dtoMap.put(dto.getId(), dto);
        }

        try {
            // 2. Contar Publicações (Agrupado por Tag)
            List<Object[]> pubCounts = em.createQuery(
                    "SELECT t.id, COUNT(p) FROM Publication p JOIN p.tags t GROUP BY t.id", Object[].class)
                    .getResultList();
            
            for (Object[] row : pubCounts) {
                Long tagId = (Long) row[0];
                Long count = (Long) row[1];
                if (dtoMap.containsKey(tagId)) {
                    dtoMap.get(tagId).setPublicationCount(count.intValue());
                }
            }

            // 3. Contar Subscritores (Agrupado por Tag)
            List<Object[]> subCounts = em.createQuery(
                    "SELECT t.id, COUNT(u) FROM User u JOIN u.subscribedTags t GROUP BY t.id", Object[].class)
                    .getResultList();

            for (Object[] row : subCounts) {
                Long tagId = (Long) row[0];
                Long count = (Long) row[1];
                if (dtoMap.containsKey(tagId)) {
                    dtoMap.get(tagId).setSubscriberCount(count.intValue());
                }
            }

            // 4. Se houver user, buscar quais ele segue
            if (username != null) {
                List<Long> subscribedTagIds = em.createQuery(
                        "SELECT t.id FROM User u JOIN u.subscribedTags t WHERE u.username = :username", Long.class)
                        .setParameter("username", username)
                        .getResultList();

                for (pt.ipleiria.estg.dei.ei.dae.academics.dtos.TagDTO dto : dtos) {
                    if (subscribedTagIds.contains(dto.getId())) {
                        dto.setSubscribed(true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return dtos;
    }

    @jakarta.ejb.EJB
    private ActivityBean activityBean;

    public void subscribe(Tag tag, User user) {
        Tag managedTag = em.merge(tag);
        User managedUser = em.merge(user);

        // Forçar inits ou usar query para verificar
        boolean alreadySubscribed = em.createQuery(
                "SELECT COUNT(t) FROM User u JOIN u.subscribedTags t WHERE u = :user AND t = :tag", Long.class)
                        .setParameter("user", managedUser)
                        .setParameter("tag", managedTag)
                        .getSingleResult() > 0;

        if (alreadySubscribed) {
            throw new ConflictException("User already subscribed to this tag");
        }

        managedTag.getSubscribers().add(managedUser);
        managedUser.getSubscribedTags().add(managedTag);
        
        activityBean.create(managedUser, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.TAG_SUBSCRIBE,
            "Seguiu a tag: " + managedTag.getName(), "Tag", managedTag.getId());
    }

    public void unsubscribe(Tag tag, User user) {
        Tag managedTag = em.merge(tag);
        User managedUser = em.merge(user);

        if (!managedTag.getSubscribers().contains(managedUser)) {
            throw new ConflictException("User is not subscribed to this tag");
        }

        managedTag.getSubscribers().remove(managedUser);
        managedUser.getSubscribedTags().remove(managedTag);

        activityBean.create(managedUser, pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType.TAG_UNSUBSCRIBE,
            "Deixou de seguir a tag: " + managedTag.getName(), "Tag", managedTag.getId());
    }

    public List<SubscriptionInfoDTO> listSubscriptions(User user) {
        User managedUser = em.merge(user);
        List<Tag> tags = managedUser.getSubscribedTags();

        return tags.stream().map(t -> {
            SubscriptionInfoDTO dto = new SubscriptionInfoDTO();
            dto.setId(t.getId());
            dto.setName(t.getName());
            dto.setSubscribedAt(null);
            dto.setPublicationsCount(0);
            dto.setNewPublicationsSinceLastVisit(0);
            return dto;
        }).toList();
    }

    public void delete(Tag tag) {
        Tag managed = em.merge(tag);
        em.remove(managed);
    }

    @jakarta.ejb.EJB
    private NotificationBean notificationBean; // Injected

    @jakarta.ejb.EJB
    private EmailBean emailBean;

    public void changeVisibility(Tag tag, boolean visible) {
        tag.setVisible(visible);
        em.merge(tag);
    }

    // Método para notificar subscritores de que uma nova publicação tem esta tag
    public void notifySubscribers(Tag tag, pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication publication) {
        Tag managedTag = em.find(Tag.class, tag.getId());
        if (managedTag == null) return;

        for (User subscriber : managedTag.getSubscribers()) {
            // Email
            if (subscriber.getEmail() != null && !subscriber.getEmail().isBlank()) {
                String subject = "Nova Publicação na Tag: " + managedTag.getName();
                String body = "Olá " + subscriber.getUsername() + ",\n\n" +
                        "Uma nova publicação foi adicionada à tag '" + managedTag.getName() + "'.\n" +
                        "Título: " + publication.getTitle() + "\n" +
                        "Autores: " + publication.getAuthors() + "\n\n" +
                        "Consulta na plataforma para mais detalhes.";
                
                emailBean.send(subscriber.getEmail(), subject, body);
            }

            // In-App Notification (NEW)
            // Não notificar o próprio autor da publicação
            if (!subscriber.getUsername().equals(publication.getUploadedBy().getUsername())) {
                 notificationBean.create(
                    subscriber,
                    "Nova Publicação na Tag " + managedTag.getName(),
                    "Nova publicação com a tag '" + managedTag.getName() + "': " + publication.getTitle(),
                    pt.ipleiria.estg.dei.ei.dae.academics.enums.NotificationType.TAG_ACTIVITY,
                    publication.getId(),
                    "Publication"
                );
            }
        }
    }

    // Método para notificar subscritores de tags sobre um novo comentário (Cenário 3)
    public void notifyCommentSubscribers(Tag tag, pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication publication, pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment comment) {
        Tag managedTag = em.find(Tag.class, tag.getId());
        if (managedTag == null) return;

        for (User subscriber : managedTag.getSubscribers()) {
            // Não notificar o próprio autor do comentário
            if (subscriber.getUsername().equals(comment.getAuthor().getUsername())) {
                continue;
            }

            // Email
            if (subscriber.getEmail() != null && !subscriber.getEmail().isBlank()) {
                String subject = "Novo Comentário em Publicação com Tag: " + managedTag.getName();
                String body = "Olá " + subscriber.getUsername() + ",\n\n" +
                        "Um novo comentário foi adicionado à publicação: " + publication.getTitle() + "\n" +
                        "Tag subscrita: " + managedTag.getName() + "\n" +
                        "Autor do comentário: " + comment.getAuthor().getUsername() + "\n" +
                        "Comentário: " + comment.getContent() + "\n\n" +
                        "Acede à plataforma para ver a discussão.";
                
                emailBean.send(subscriber.getEmail(), subject, body);
            }

            // In-App Notification (NEW)
            notificationBean.create(
                subscriber,
                "Atividade na Tag " + managedTag.getName(),
                comment.getAuthor().getUsername() + " comentou uma publicação com a tag '" + managedTag.getName() + "'.",
                pt.ipleiria.estg.dei.ei.dae.academics.enums.NotificationType.TAG_ACTIVITY,
                publication.getId(),
                "Publication"
            );
        }
    }

}
