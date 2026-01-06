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

    @jakarta.inject.Inject
    private pt.ipleiria.estg.dei.ei.dae.academics.security.TokenIssuer tokenIssuer;

    public String authenticate(String username, String password) {
        User user = em.createNamedQuery("getUserByName", User.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst().orElse(null);

        if (user != null && user.getPassword().equals(password)) {
            // EP99 - Bloquear login se não estiver ativo
            if (user.getStatus() != pt.ipleiria.estg.dei.ei.dae.academics.enums.UserStatus.ACTIVE) {
                return null; 
            }

            // CHECK: Se utilizador já tem sessão válida (não expirada)
            if (user.getLastLogin() != null) {
                long expirationTime = user.getLastLogin().getTime() + (pt.ipleiria.estg.dei.ei.dae.academics.security.TokenIssuer.EXPIRY_MINS * 60 * 1000);
                if (System.currentTimeMillis() < expirationTime) {
                    // Sessão ainda é válida -> Bloqueia novo login
                   throw new jakarta.ws.rs.ClientErrorException("Esta conta ja tem uma sessao ativa. Aguarde o fim da sessao ou faça logout no outro dispositivo.", jakarta.ws.rs.core.Response.Status.CONFLICT);
                }
            }

            // 1. GERAR NOVA VERSÃO DE AUTH
            String newAuthVersion = java.util.UUID.randomUUID().toString();
            user.setAuthVersion(newAuthVersion); 
            user.setLastLogin(new java.util.Date()); // Atualiza Last Login
            em.merge(user); 
            
            // 2. PASSAR A VERSÃO PARA O TOKEN
            return tokenIssuer.issue(user.getUsername(), newAuthVersion);
        }
        return null;
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

    public void logout(String username) {
        User user = findUserByUsername(username);
        if (user != null) {
            user.setLastLogin(null); // Limpa a sessão
            em.merge(user);
        }
    }

    public User findUserByEmail(String email) {
        try {
            return em.createNamedQuery("getUserByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public User findUserByResetToken(String token) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.resetToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public String generateResetToken(String email) {
        User user = findUserByEmail(email);
        if (user != null) {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            // 1 hora de validade
            user.setResetTokenExpiry(new java.util.Date(System.currentTimeMillis() + 3600 * 1000));
            em.merge(user);
            return token;
        }
        return null;
    }

    public boolean updatePasswordWithToken(String token, String newPassword) {
        User user = findUserByResetToken(token);
        if (user != null) {
            if (user.getResetTokenExpiry().before(new java.util.Date())) {
                return false; // Token expirado
            }
            user.setPassword(newPassword); // Deveria ter hash aqui, mas mantemos simples como o resto
            user.setResetToken(null); 
            user.setResetTokenExpiry(null);
            em.merge(user);
            return true;
        }
        return false;
    }
}
