package com.global.session.auth;

import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.UnauthorizedAccessException;
import com.global.interceptor.Audited;
import com.global.interceptor.PerformanceMonitored;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Audited
@PerformanceMonitored
public class AuthenticationBean {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationBean.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User login(String username, String rawPassword) throws UnauthorizedAccessException {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new UnauthorizedAccessException("anonymous", "login with empty credentials");
        }

        try {
            User user = em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username.trim())
                    .getSingleResult();

            if (!user.isActive()) {
                LOGGER.log(Level.WARNING, "[AUTH] Login attempt for inactive account: {0}", username);
                throw new UnauthorizedAccessException(username, "login — account is deactivated");
            }

            if (user.getPasswordHash().equals(hashPassword(rawPassword))) {
                user.setLastLogin(LocalDateTime.now());
                em.merge(user);
                LOGGER.log(Level.INFO, "[AUTH] Successful login for user: {0} with role: {1}",
                        new Object[]{username, user.getRole()});
                return user;
            } else {
                LOGGER.log(Level.WARNING, "[AUTH] Invalid password for user: {0}", username);
                throw new UnauthorizedAccessException(username, "login — invalid credentials");
            }
        } catch (NoResultException e) {
            LOGGER.log(Level.WARNING, "[AUTH] Unknown username: {0}", username);
            throw new UnauthorizedAccessException(username, "login — user not found");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User register(String username, String email, String rawPassword,
                         String fullName, UserRole role) {
        String passwordHash = hashPassword(rawPassword);
        User user = new User(username, email, passwordHash, fullName, role);
        em.persist(user);
        LOGGER.log(Level.INFO, "[AUTH] New user registered: {0} with role: {1}",
                new Object[]{username, role});
        return user;
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findByUsername(String username) {
        try {
            return em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> findByRole(UserRole role) {
        return em.createNamedQuery("User.findByRole", User.class)
                .setParameter("role", role)
                .getResultList();
    }


    public void assertRole(User user, UserRole... permittedRoles) throws UnauthorizedAccessException {
        if (user == null) {
            throw new UnauthorizedAccessException("anonymous", "access to secured resource");
        }

        if (user.getRole() == UserRole.SYSTEM_ADMIN) {
            return;
        }

        for (UserRole role : permittedRoles) {
            if (user.getRole() == role) {
                return;
            }
        }

        LOGGER.log(Level.WARNING,
                "[AUTH] Access denied for user {0} with role {1}",
                new Object[]{user.getUsername(), user.getRole()});

        throw new UnauthorizedAccessException(
                user.getUsername(),
                "perform operation — insufficient role privileges"
        );
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deactivateUser(Long userId, User requestingUser) throws UnauthorizedAccessException {
        assertRole(requestingUser, UserRole.SYSTEM_ADMIN);
        User target = em.find(User.class, userId);
        if (target != null) {
            target.setActive(false);
            em.merge(target);
            LOGGER.log(Level.INFO, "[AUTH] User {0} deactivated by admin {1}",
                    new Object[]{target.getUsername(), requestingUser.getUsername()});
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available on this JVM", e);
        }
    }
}
