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
    public void subscribe(Tag tag, User user) {
        Tag managedTag = em.merge(tag);
        User managedUser = em.merge(user);

        if (managedTag.getSubscribers().contains(managedUser)) {
            throw new ConflictException("User already subscribed to this tag");
        }

        managedTag.getSubscribers().add(managedUser);
        managedUser.getSubscribedTags().add(managedTag);
    }

    public void unsubscribe(Tag tag, User user) {
        Tag managedTag = em.merge(tag);
        User managedUser = em.merge(user);

        if (!managedTag.getSubscribers().contains(managedUser)) {
            throw new ConflictException("User is not subscribed to this tag");
        }

        managedTag.getSubscribers().remove(managedUser);
        managedUser.getSubscribedTags().remove(managedTag);
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

    public void changeVisibility(Tag tag, boolean visible) {
        tag.setVisible(visible);
        em.merge(tag);
    }

}
