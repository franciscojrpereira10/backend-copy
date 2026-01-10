package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Activity;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;

import java.time.Instant;
import java.util.List;

@Stateless
public class ActivityBean {

    @PersistenceContext
    private EntityManager em;

    public List<Activity> findByUser(Long userId, ActivityType type,
                                     Instant from, Instant to, int offset, int limit) {

        String jpql = "SELECT a FROM Activity a JOIN FETCH a.user WHERE a.user.id = :uid";
        if (type != null) jpql += " AND a.type = :type";
        if (from != null) jpql += " AND a.timestamp >= :from";
        if (to   != null) jpql += " AND a.timestamp <= :to";
        jpql += " ORDER BY a.timestamp DESC";

        var q = em.createQuery(jpql, Activity.class)
                .setParameter("uid", userId)
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (type != null) q.setParameter("type", type);
        if (from != null) q.setParameter("from", java.util.Date.from(from));
        if (to   != null) q.setParameter("to", java.util.Date.from(to));

        return q.getResultList();
    }
    public void create(pt.ipleiria.estg.dei.ei.dae.academics.entities.User user, ActivityType type, String description, String entityType, Long entityId) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setType(type);
        activity.setDescription(description);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);
        activity.setTimestamp(new java.util.Date());
        em.persist(activity);
    }
}
