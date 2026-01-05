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
    
    @jakarta.ejb.EJB
    private UserBean userBean;

    public String authenticate(String username, String password) {
        User user = findUserByUsername(username);
        if (user == null) {
            return null;
        }
        if (!password.equals(user.getPassword())) {
            return null;
        }
        // EP99 - Bloquear login se não estiver ativo
        if (user.getStatus() != pt.ipleiria.estg.dei.ei.dae.academics.enums.UserStatus.ACTIVE) {
            return null; 
        }
        
        // Single Session: Gerar nova versão de autenticação
        String newVersion = java.util.UUID.randomUUID().toString();
        System.out.println("DEBUG_AUTH: Generating new version for " + username + ": " + newVersion);
        user.setAuthVersion(newVersion);
        
        // CRITICAL: Force update to DB immediately
        // We need to inject UserBean first.
        userBean.update(user);
        
        return TokenIssuer.issue(username, newVersion);
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
