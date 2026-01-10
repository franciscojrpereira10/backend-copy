package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.CommentEditHistory;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;

@Stateless
public class CommentBean {

    @PersistenceContext
    private EntityManager em;

    @jakarta.ejb.EJB
    private ActivityBean activityBean;

    public Comment find(Long id) {
        return em.createQuery(
                        "SELECT c FROM Comment c " +
                                "JOIN FETCH c.author " +
                                "JOIN FETCH c.publication " +
                                "WHERE c.id = :id", Comment.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public Comment update(Comment comment) {
        Comment merged = em.merge(comment);
        em.flush();
        return merged;
    }

    public void remove(Long id, User performedBy) {
        Comment c = em.find(Comment.class, id);
        if (c != null) {
            String commentSummary = c.getContent().length() > 20 ? c.getContent().substring(0, 20) + "..." : c.getContent();
            activityBean.create(performedBy, ActivityType.DELETE,
                "Apagou o comentário: \"" + commentSummary + "\"", "Comment", c.getId());
            em.remove(c);
        }
    }
    
    public void toggleVisibility(Long id, boolean visible, User performedBy) {
        Comment c = em.find(Comment.class, id);
        if (c != null) {
            c.setVisible(visible);
            em.merge(c);
            
            activityBean.create(performedBy, ActivityType.VISIBILITY,
                (visible ? "Mostrou" : "Ocultou") + " o comentário", "Comment", c.getId());
        }
    }

    public void persistHistory(CommentEditHistory history) {
        em.persist(history);
    }
}
