package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.Role;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.UserStatus;
import pt.ipleiria.estg.dei.ei.dae.academics.dtos.UserDTO;

import java.util.Date;
import java.util.List;

@Stateless
public class UserBean {

    @PersistenceContext
    private EntityManager em;

    public List<User> getAll() {
        return em.createNamedQuery("getAllUsers", User.class).getResultList();
    }

    public User create(String username,
                       String email,
                       String password,
                       Role role) {

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(new Date());

        em.persist(user);
        em.flush();
        return user;
    }

    public User find(Long id) {
        return em.find(User.class, id);
    }

    public User find(String username) {
        try {
            return em.createNamedQuery("getUserByName", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean canLogin(String username, String password) {
        User user = find(username);
        return user != null &&
                pt.ipleiria.estg.dei.ei.dae.academics.security.Hasher.verify(password, user.getPassword());
    }

    public long countPublications(User user) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM Publication p WHERE p.uploadedBy = :u", Long.class)
                .setParameter("u", user)
                .getSingleResult();
    }

    public long countComments(User user) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM Comment c WHERE c.author = :u", Long.class)
                .setParameter("u", user)
                .getSingleResult();
    }

    public long countRatings(User user) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM Rating r WHERE r.user = :u", Long.class)
                .setParameter("u", user)
                .getSingleResult();
    }

    public long countSubscribedTags(User user) {
        return em.createQuery(
                        "SELECT COUNT(t) FROM Tag t JOIN t.subscribers s WHERE s = :u", Long.class)
                .setParameter("u", user)
                .getSingleResult();
    }

    public UserDTO toDetailedDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = UserDTO.from(user);

        dto.setPublicationCount((int) countPublications(user));
        dto.setCommentsCount((int) countComments(user));
        dto.setRatingsCount((int) countRatings(user));
        dto.setSubscribedTagsCount((int) countSubscribedTags(user));

        List<String> tags = em.createQuery(
                        "SELECT t.name FROM Tag t JOIN t.subscribers s WHERE s = :u", String.class)
                .setParameter("u", user)
                .getResultList();
        dto.setSubscribedTags(tags);

        return dto;
    }

    public User findByEmail(String email) {
        try {
            return em.createNamedQuery("User.findByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (Exception e) {
        }
        return null;
    }

    public User updateUser(Long id, String newEmail, Role newRole, boolean allowRoleChange) {
        User user = find(id);
        if (user == null) {
            return null;
        }

        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail.trim());
        }

        if (allowRoleChange && newRole != null) {
            user.setRole(newRole);
        }

        // se tiveres campo lastModified em User, atualiza aqui:
        // user.setLastModified(new Date());

        return user;
    }
    // EP35 - alterar status
    public User changeStatus(Long id, String newStatus) {
        User user = find(id);
        if (user == null) return null;

        String normalized = java.text.Normalizer
                .normalize(newStatus, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();

        if (normalized.startsWith("SUSPENS")) {
            normalized = "SUSPENDED";
        } else if (normalized.startsWith("ATIV")) {
            normalized = "ACTIVE";
        }

        UserStatus status = UserStatus.valueOf(normalized);
        user.setStatus(status);
        return user;
    }





    // EP36 - alterar role
    public User changeRole(Long id, String newRole) {
        User user = find(id);
        if (user == null) return null;

        Role role = Role.valueOf(newRole.toUpperCase());
        user.setRole(role);
        return user;
    }

    // EP37 - soft delete
    // EP37 - hard delete (pedido user: eliminar permanentemente)
    // EP37 - hard delete (pedido user: eliminar permanentemente)
    // EP37 - hard delete full
    public boolean remove(Long id) {
        User user = find(id);
        if (user == null) return false;

        // 1. Apagar Publicações do user e TUDO o que depende delas
        List<pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication> userPubs = 
            new java.util.ArrayList<>(user.getPublications());
            
        for (pt.ipleiria.estg.dei.ei.dae.academics.entities.Publication p : userPubs) {
            // Limpar History da Publicação
            for (pt.ipleiria.estg.dei.ei.dae.academics.entities.PublicationHistory ph : new java.util.ArrayList<>(p.getHistory())) {
                em.remove(ph);
            }
            // Limpar Ratings da Publicação
            for (pt.ipleiria.estg.dei.ei.dae.academics.entities.Rating r : new java.util.ArrayList<>(p.getRatings())) {
                em.remove(r);
            }
            // Limpar Comentários da Publicação
            for (pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment c : new java.util.ArrayList<>(p.getComments())) {
                em.remove(c);
            }
            // Limpar Tags (associações)
            p.getTags().clear(); 
            // Agora sim, remove a Publicação
            em.remove(p);
        }
        
        // 2. Remover Comentários feitos pelo user (noutras publicações)
        for (pt.ipleiria.estg.dei.ei.dae.academics.entities.Comment c : new java.util.ArrayList<>(user.getComments())) {
             em.remove(c);
        }
        
        // 3. Remover Activities
        for (pt.ipleiria.estg.dei.ei.dae.academics.entities.Activity a : new java.util.ArrayList<>(user.getActivities())) {
             em.remove(a);
        }

        // Ratings, Notifications, Tags(Subscribed) devem ir via Cascade do User ou table join cleanup
        em.remove(user);
        return true;
    }

    public void update(User user) {
        em.merge(user);
    }

    public long countPublicationsSubmitted(User user) {
        return em.createQuery(
                "SELECT COUNT(p) FROM Publication p WHERE p.uploadedBy = :u",
                Long.class).setParameter("u", user).getSingleResult();
    }

    public long countCommentsPosted(User user) {
        return em.createQuery(
                "SELECT COUNT(c) FROM Comment c WHERE c.author = :u",
                Long.class).setParameter("u", user).getSingleResult();
    }

    public long countRatingsGiven(User user) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Rating r WHERE r.user = :u",
                Long.class).setParameter("u", user).getSingleResult();
    }

}
