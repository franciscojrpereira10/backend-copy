package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashMap;
import java.util.Map;

@Stateless
public class StatisticsBean {

    @PersistenceContext
    private EntityManager em;

    // EP48
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = (long) em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
        long activeUsers = (long) em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'", Long.class)
                .getSingleResult();
        long suspendedUsers = (long) em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.status = 'SUSPENDED'", Long.class)
                .getSingleResult();

        long totalPublications = (long) em.createQuery(
                        "SELECT COUNT(p) FROM Publication p", Long.class)
                .getSingleResult();
        long visiblePublications = (long) em.createQuery(
                        "SELECT COUNT(p) FROM Publication p WHERE p.visible = true", Long.class)
                .getSingleResult();
        long hiddenPublications = totalPublications - visiblePublications;

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("suspendedUsers", suspendedUsers);
        stats.put("totalPublications", totalPublications);
        stats.put("visiblePublications", visiblePublications);
        stats.put("hiddenPublications", hiddenPublications);

        long totalComments = (long) em.createQuery("SELECT COUNT(c) FROM Comment c", Long.class).getSingleResult();
        long totalRatings = (long) em.createQuery("SELECT COUNT(r) FROM Rating r", Long.class).getSingleResult();
        long totalTags = (long) em.createQuery("SELECT COUNT(t) FROM Tag t", Long.class).getSingleResult();

        stats.put("totalComments", totalComments);
        stats.put("totalRatings", totalRatings);
        stats.put("totalTags", totalTags);

        return stats;
    }


    // EP49
    public Map<String, Object> getPersonalStatistics(String username) {
        Map<String, Object> stats = new HashMap<>();

        // nº publicações submetidas pelo user (uploadedBy)
        long publicationsSubmitted = (long) em.createQuery(
                        "SELECT COUNT(p) FROM Publication p " +
                                "WHERE p.uploadedBy.username = :u", Long.class)
                .setParameter("u", username)
                .getSingleResult();

        // nº comentários escritos pelo user (ajusta 'author' ou 'user' conforme a tua entidade Comment)
        long commentsPosted = (long) em.createQuery(
                        "SELECT COUNT(c) FROM Comment c " +
                                "WHERE c.author.username = :u", Long.class)
                .setParameter("u", username)
                .getSingleResult();

        // nº ratings dados pelo user (ajusta 'user' ou 'rater' conforme a tua entidade Rating)
        long ratingsGiven = (long) em.createQuery(
                        "SELECT COUNT(r) FROM Rating r " +
                                "WHERE r.user.username = :u", Long.class)
                .setParameter("u", username)
                .getSingleResult();

        stats.put("publicationsSubmitted", publicationsSubmitted);
        stats.put("commentsPosted", commentsPosted);
        stats.put("ratingsGiven", ratingsGiven);

        return stats;
    }
}
