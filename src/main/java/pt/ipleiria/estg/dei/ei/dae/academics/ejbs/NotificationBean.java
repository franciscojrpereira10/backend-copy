package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.Notification;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;

import java.util.List;

@Stateless
public class NotificationBean {

    @PersistenceContext
    private EntityManager em;

    // obter utilizador pelo username (reutiliza se já tiveres UserBean)
    public User findUser(String username) {
        TypedQuery<User> q = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :u", User.class);
        q.setParameter("u", username);
        List<User> res = q.getResultList();
        return res.isEmpty() ? null : res.get(0);
    }

    public Notification find(long id) {
        return em.find(Notification.class, id);
    }

    // EP45: lista notificações de um user, opcionalmente só não lidas
    public List<Notification> findByUser(User user, boolean unreadOnly) {
        if (unreadOnly) {
            TypedQuery<Notification> q = em.createQuery(
                    "SELECT n FROM Notification n " +
                            "WHERE n.user = :u AND n.readFlag = false " +
                            "ORDER BY n.createdAt DESC", Notification.class);
            q.setParameter("u", user);
            return q.getResultList();
        } else {
            TypedQuery<Notification> q = em.createQuery(
                    "SELECT n FROM Notification n " +
                            "WHERE n.user = :u " +
                            "ORDER BY n.createdAt DESC", Notification.class);
            q.setParameter("u", user);
            return q.getResultList();
        }
    }

    public long countUnread(User user) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(n) FROM Notification n " +
                        "WHERE n.user = :u AND n.readFlag = false", Long.class);
        q.setParameter("u", user);
        return q.getSingleResult();
    }

    // EP46: marcar 1 como lida
    public void markAsRead(Notification n) {
        n.markAsRead();
        em.merge(n);
    }

    // EP47: marcar todas do user como lidas; devolve nº marcadas
    public int markAllAsRead(User user) {
        TypedQuery<Notification> q = em.createQuery(
                "SELECT n FROM Notification n " +
                        "WHERE n.user = :u AND n.readFlag = false", Notification.class);
        q.setParameter("u", user);
        List<Notification> list = q.getResultList();
        list.forEach(Notification::markAsRead);
        // merge em lote simples
        for (Notification n : list) {
            em.merge(n);
        }
        return list.size();
    }

    // opcional: criar notificação a partir de código de negócio
    public Notification create(User user, String title, String message,
                               pt.ipleiria.estg.dei.ei.dae.academics.enums.NotificationType type,
                               Long relatedEntityId, String relatedEntityType) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setRelatedEntityId(relatedEntityId);
        n.setRelatedEntityType(relatedEntityType);
        em.persist(n);
        return n;
    }
}
