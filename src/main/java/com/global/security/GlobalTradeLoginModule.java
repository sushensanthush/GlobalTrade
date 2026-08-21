package com.global.security;

import com.global.entity.User;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.*;
import java.util.HexFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalTradeLoginModule implements LoginModule {

    private static final Logger LOGGER = Logger.getLogger(GlobalTradeLoginModule.class.getName());


    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;


    private boolean loginSucceeded = false;
    private boolean commitSucceeded = false;


    private String authenticatedUsername;
    private String authenticatedRole;


    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;


    private Principal userPrincipal;
    private Principal rolePrincipal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;

        Object urlObj  = options.get("jdbcUrl");
        Object userObj = options.get("jdbcUser");
        Object pwObj   = options.get("jdbcPassword");

        this.jdbcUrl      = (urlObj  != null) ? urlObj.toString()  : "jdbc:mysql://localhost:3306/global_trade_db";
        this.jdbcUser     = (userObj != null) ? userObj.toString() : "root";
        this.jdbcPassword = (pwObj   != null) ? pwObj.toString()   : "";

        LOGGER.log(Level.FINE, "[JAAS] GlobalTradeLoginModule initialized for subject");
    }

    @Override
    public boolean login() throws LoginException {

        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[JAAS] Callback handling failed", e);
            throw new LoginException("Failed to retrieve credentials: " + e.getMessage());
        }

        String username = nameCallback.getName();
        char[] passwordChars = passwordCallback.getPassword();
        passwordCallback.clearPassword(); // Clear sensitive data immediately

        if (username == null || username.isBlank()) {
            throw new LoginException("Username is required");
        }
        if (passwordChars == null || passwordChars.length == 0) {
            throw new LoginException("Password is required");
        }

        String rawPassword = new String(passwordChars);
        String passwordHash = hashPassword(rawPassword);


        try {
            loginSucceeded = verifyCredentials(username, passwordHash);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[JAAS] Database error during authentication", e);
            throw new LoginException("Authentication system unavailable: " + e.getMessage());
        }

        if (!loginSucceeded) {
            LOGGER.log(Level.WARNING, "[JAAS] Authentication failed for user: {0}", username);
            throw new LoginException("Invalid credentials for user: " + username);
        }

        LOGGER.log(Level.INFO, "[JAAS] Authentication succeeded for user: {0} role: {1}",
                new Object[]{authenticatedUsername, authenticatedRole});
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }


        userPrincipal = new GlobalTradePrincipal(authenticatedUsername);
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }


        rolePrincipal = new GlobalTradePrincipal("ROLE_" + authenticatedRole);
        if (!subject.getPrincipals().contains(rolePrincipal)) {
            subject.getPrincipals().add(rolePrincipal);
        }

        commitSucceeded = true;
        LOGGER.log(Level.INFO, "[JAAS] Principals committed for user: {0} ({1})",
                new Object[]{authenticatedUsername, authenticatedRole});
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }
        if (!commitSucceeded) {

            loginSucceeded = false;
            authenticatedUsername = null;
            authenticatedRole = null;
            userPrincipal = null;
            rolePrincipal = null;
        } else {
            logout();
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().remove(rolePrincipal);
        loginSucceeded = false;
        commitSucceeded = false;
        authenticatedUsername = null;
        authenticatedRole = null;
        userPrincipal = null;
        rolePrincipal = null;
        LOGGER.log(Level.INFO, "[JAAS] Logout completed");
        return true;
    }


    private boolean verifyCredentials(String username, String passwordHash) throws SQLException {
        String sql = "SELECT username, role, active FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            stmt.setString(2, passwordHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean active = rs.getBoolean("active");
                    if (!active) {
                        LOGGER.log(Level.WARNING,
                                "[JAAS] Login attempt for deactivated account: {0}", username);
                        return false;
                    }
                    this.authenticatedUsername = rs.getString("username");
                    this.authenticatedRole = rs.getString("role");
                    return true;
                }
                return false;
            }
        }
    }


    private String hashPassword(String rawPassword) throws LoginException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new LoginException("SHA-256 not available: " + e.getMessage());
        }
    }


    private static class GlobalTradePrincipal implements Principal, java.io.Serializable {
        private final String name;

        GlobalTradePrincipal(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof GlobalTradePrincipal)) return false;
            return name.equals(((GlobalTradePrincipal) obj).name);
        }

        @Override public int hashCode() { return name.hashCode(); }
        @Override public String toString() { return "GlobalTradePrincipal(" + name + ")"; }
    }
}
