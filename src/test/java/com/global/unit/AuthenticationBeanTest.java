package com.global.unit;

import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.auth.AuthenticationBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationBean Unit Tests")
class AuthenticationBeanTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private AuthenticationBean authBean;

    private User activeAdmin;
    private String rawPassword = "admin123";
    private String hashedPassword;

    @BeforeEach
    void setUp() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        hashedPassword = HexFormat.of().formatHex(hash);

        activeAdmin = new User("admin", "admin@globaltrade.com", hashedPassword,
                "System Administrator", UserRole.SYSTEM_ADMIN);
        activeAdmin.setActive(true);
    }


    @Test
    @DisplayName("login — correct credentials should return authenticated User")
    void login_correctCredentials_returnsUser() throws UnauthorizedAccessException {
        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("username"), eq("admin"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(activeAdmin);
        when(em.merge(any(User.class))).thenReturn(activeAdmin);

        User result = authBean.login("admin", rawPassword);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getRole()).isEqualTo(UserRole.SYSTEM_ADMIN);
        assertThat(result.getLastLogin()).isNotNull();
    }

    @Test
    @DisplayName("login — wrong password should throw UnauthorizedAccessException")
    void login_wrongPassword_throwsException() {
        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("username"), eq("admin"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(activeAdmin);

        assertThatThrownBy(() -> authBean.login("admin", "wrongpassword"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("invalid credentials");

        verify(em, never()).merge(any());
    }

    @Test
    @DisplayName("login — inactive account should throw UnauthorizedAccessException")
    void login_inactiveAccount_throwsException() {
        activeAdmin.setActive(false);

        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("username"), eq("admin"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(activeAdmin);

        assertThatThrownBy(() -> authBean.login("admin", rawPassword))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("login — unknown username should throw UnauthorizedAccessException")
    void login_unknownUsername_throwsException() {
        @SuppressWarnings("unchecked")
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("User.findByUsername", User.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("username"), eq("ghost"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenThrow(new NoResultException());

        assertThatThrownBy(() -> authBean.login("ghost", rawPassword))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("user not found");
    }

    @Test
    @DisplayName("login — blank username should throw UnauthorizedAccessException immediately")
    void login_blankCredentials_throwsWithoutDbCall() {
        assertThatThrownBy(() -> authBean.login("  ", rawPassword))
                .isInstanceOf(UnauthorizedAccessException.class);

        verifyNoInteractions(em);
    }


    @Test
    @DisplayName("assertRole — user with permitted role should pass without exception")
    void assertRole_permittedRole_passes() {
        User logisticsUser = new User("logistics", "log@gt.com", hashedPassword,
                "Logistics", UserRole.LOGISTICS_COORDINATOR);

        assertThatNoException().isThrownBy(() ->
                authBean.assertRole(logisticsUser,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN));
    }

    @Test
    @DisplayName("assertRole — user without permitted role should throw UnauthorizedAccessException")
    void assertRole_deniedRole_throwsException() {
        User vendorUser = new User("vendor", "v@gt.com", hashedPassword,
                "Vendor", UserRole.VENDOR_REPRESENTATIVE);

        assertThatThrownBy(() ->
                authBean.assertRole(vendorUser,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("vendor")
                .hasMessageContaining("insufficient role");
    }

    @Test
    @DisplayName("assertRole — null user should throw UnauthorizedAccessException")
    void assertRole_nullUser_throwsException() {
        assertThatThrownBy(() ->
                authBean.assertRole(null, UserRole.SYSTEM_ADMIN))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("anonymous");
    }

    @Test
    @DisplayName("assertRole — SYSTEM_ADMIN always passes for any secured operation")
    void assertRole_systemAdmin_alwaysPermitted() {
        assertThatNoException().isThrownBy(() ->
                authBean.assertRole(activeAdmin,
                        UserRole.SYSTEM_ADMIN, UserRole.CUSTOMS_AGENT,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.WAREHOUSE_MANAGER));
    }



    @Test
    @DisplayName("register — should persist new user with hashed password")
    void register_validInputs_persistsUser() {
        doNothing().when(em).persist(any(User.class));

        User result = authBean.register("newuser", "new@gt.com",
                "password123", "New User", UserRole.CUSTOMS_AGENT);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMS_AGENT);
        assertThat(result.getPasswordHash()).isNotEqualTo("password123");
        assertThat(result.getPasswordHash()).hasSize(64); // SHA-256 hex = 64 chars
        verify(em, times(1)).persist(any(User.class));
    }
}
