package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleiria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleiria.estg.dei.ei.dae.academics.security.Hasher;
import pt.ipleiria.estg.dei.ei.dae.academics.security.TokenIssuer;

@Stateless
public class AuthService {

    @PersistenceContext
    private EntityManager em;

    public String authenticate(String username, String password) {
        User user = findUserByUsername(username);
        if (user == null) {
            return null;
        }
        if (!password.equals(user.getPassword())) {
            return null;
        }
        return TokenIssuer.issue(username);
    }

    public User findUserByUsername(String username) {
        try {
            return em.createNamedQuery("getUserByName", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public User findUserById(Long userId) {
        return em.find(User.class, userId);
    }

    public boolean isUserValid(String username) {
        return findUserByUsername(username) != null;
    }
}
