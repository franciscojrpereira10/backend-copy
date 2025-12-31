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
                                     Instant from, Instant to, int limit) {

        String jpql = "SELECT a FROM Activity a WHERE a.user.id = :uid";
        if (type != null) jpql += " AND a.type = :type";
        if (from != null) jpql += " AND a.timestamp >= :from";
        if (to   != null) jpql += " AND a.timestamp <= :to";
        jpql += " ORDER BY a.timestamp DESC";

        var q = em.createQuery(jpql, Activity.class)
                .setParameter("uid", userId)
                .setMaxResults(limit);

        if (type != null) q.setParameter("type", type);
        if (from != null) q.setParameter("from", from);
        if (to   != null) q.setParameter("to", to);

        return q.getResultList();
    }
}
