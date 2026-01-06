package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.CommentEditHistory;

@Stateless
public class CommentBean {

    @PersistenceContext
    private EntityManager em;

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

    public void remove(Long id) {
        Comment c = em.find(Comment.class, id);
        if (c != null) {
            em.remove(c);
        }
    }

    public void persistHistory(CommentEditHistory history) {
        em.persist(history);
    }
}
