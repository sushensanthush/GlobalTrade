package com.global.security;

import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.auth.AuthenticationBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@DisplayName("Security Penetration and Vulnerability Assessment")
class SecurityPenetrationTest {

    @Mock private EntityManager em;
    @InjectMocks private AuthenticationBean authBean;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        injectEntityManager();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }


    @Nested
    @DisplayName("1. Credential Input Vulnerability Tests")
    class CredentialInputTests {

        @Test
        @DisplayName("SQL injection in username field is rejected")
        void sqlInjectionInUsername_ShouldFail() {
            // Classic SQL injection payloads
            String[] injectionPayloads = {
                "' OR '1'='1",
                "admin'--",
                "' OR 1=1--",
                "'; DROP TABLE users;--",
                "admin' /*",
                "\" OR \"\"=\""
            };


            TypedQuery<User> mockQuery = mockUserQuery(null);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);

            for (String payload : injectionPayloads) {
                assertThatThrownBy(() -> authBean.login(payload, "anyPassword"))
                        .as("SQL injection payload '%s' should fail authentication", payload)
                        .isInstanceOf(Exception.class);
            }

        }

        @Test
        @DisplayName("Empty username is rejected before DB lookup")
        void emptyUsername_ShouldThrowBeforeDBLookup() {
            assertThatThrownBy(() -> authBean.login("", "password123"))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("Null password is rejected before DB lookup")
        void nullPassword_ShouldThrowBeforeDBLookup() {
            assertThatThrownBy(() -> authBean.login("admin", null))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(em);
        }

        @ParameterizedTest
        @DisplayName("Blank/whitespace credentials are rejected without DB lookup")
        @ValueSource(strings = {"   ", "\t", "\n", "  \t  "})
        void whitespaceCredentials_ShouldBeRejected(String blankInput) {
            assertThatThrownBy(() -> authBean.login(blankInput, "password"))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("Extremely long username (buffer overflow simulation) is handled safely")
        void oversizedUsername_ShouldBeHandledSafely() {
            String oversizedUsername = "A".repeat(10000);


            TypedQuery<User> mockQuery = mockUserQuery(null);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);

            assertThatThrownBy(() -> authBean.login(oversizedUsername, "password"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }



    @Nested
    @DisplayName("2. Role Escalation / Privilege Boundary Tests")
    class RoleEscalationTests {

        @Test
        @DisplayName("VENDOR_REPRESENTATIVE cannot assert SYSTEM_ADMIN role")
        void vendorRep_CannotEscalateTo_SystemAdmin() {
            User vendorUser = buildUser("vendor", UserRole.VENDOR_REPRESENTATIVE);

            // assertRole(user, permittedRoles...) — varargs
            assertThatThrownBy(() ->
                    authBean.assertRole(vendorUser, UserRole.SYSTEM_ADMIN))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("CUSTOMS_AGENT cannot assert LOGISTICS_COORDINATOR role")
        void customsAgent_CannotEscalateTo_LogisticsCoordinator() {
            User customsUser = buildUser("customs", UserRole.CUSTOMS_AGENT);

            assertThatThrownBy(() ->
                    authBean.assertRole(customsUser, UserRole.LOGISTICS_COORDINATOR))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("WAREHOUSE_MANAGER cannot assert CUSTOMS_AGENT role")
        void warehouseManager_CannotAssert_CustomsAgentRole() {
            User warehouseUser = buildUser("warehouse", UserRole.WAREHOUSE_MANAGER);

            assertThatThrownBy(() ->
                    authBean.assertRole(warehouseUser, UserRole.CUSTOMS_AGENT))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("SYSTEM_ADMIN can assert any role (superuser)")
        void systemAdmin_CanAssert_AnyRole() {
            User adminUser = buildUser("admin", UserRole.SYSTEM_ADMIN);


            assertThatCode(() -> authBean.assertRole(adminUser, UserRole.SYSTEM_ADMIN))
                    .doesNotThrowAnyException();
            assertThatCode(() -> authBean.assertRole(adminUser, UserRole.LOGISTICS_COORDINATOR))
                    .doesNotThrowAnyException();
            assertThatCode(() -> authBean.assertRole(adminUser, UserRole.CUSTOMS_AGENT))
                    .doesNotThrowAnyException();
            assertThatCode(() -> authBean.assertRole(adminUser, UserRole.WAREHOUSE_MANAGER))
                    .doesNotThrowAnyException();
            assertThatCode(() -> authBean.assertRole(adminUser, UserRole.VENDOR_REPRESENTATIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("LOGISTICS_COORDINATOR can assert own role but not SYSTEM_ADMIN")
        void logisticsCoordinator_CannotEscalate_ToSystemAdmin() {
            User logisticsUser = buildUser("logistics", UserRole.LOGISTICS_COORDINATOR);

            assertThatCode(() ->
                    authBean.assertRole(logisticsUser, UserRole.LOGISTICS_COORDINATOR))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() ->
                    authBean.assertRole(logisticsUser, UserRole.SYSTEM_ADMIN))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Null user is always rejected regardless of required role")
        void nullUser_AlwaysRejected() {
            assertThatThrownBy(() ->
                    authBean.assertRole(null, UserRole.VENDOR_REPRESENTATIVE))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }


    @Nested
    @DisplayName("3. Account State Security Tests")
    class AccountStateTests {

        @Test
        @DisplayName("Inactive account is rejected even with correct credentials")
        void inactiveAccount_ShouldBeRejectedDespiteCorrectCredentials() {
            String password = "password123";
            String hash = sha256(password);

            User inactiveUser = buildUser("inactive_user", UserRole.LOGISTICS_COORDINATOR);
            inactiveUser.setActive(false);
            inactiveUser.setPasswordHash(hash);

            TypedQuery<User> mockQuery = mockUserQuery(inactiveUser);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);


            assertThatThrownBy(() -> authBean.login("inactive_user", password))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("deactivated");
        }

        @Test
        @DisplayName("Non-existent user is rejected (UnauthorizedAccessException)")
        void nonExistentUser_ShouldBeRejected() {
            TypedQuery<User> mockQuery = mockUserQuery(null);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);

            assertThatThrownBy(() -> authBean.login("ghost_user", "anyPassword"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }


    @Nested
    @DisplayName("4. Password Security Tests")
    class PasswordSecurityTests {

        @Test
        @DisplayName("Wrong password with correct username is rejected")
        void wrongPassword_WithCorrectUsername_ShouldFail() {
            User existingUser = buildUser("admin", UserRole.SYSTEM_ADMIN);
            existingUser.setPasswordHash(sha256("correctPassword123"));

            TypedQuery<User> mockQuery = mockUserQuery(existingUser);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);

            assertThatThrownBy(() -> authBean.login("admin", "wrongPassword"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Correct credentials authenticate successfully")
        void correctCredentials_ShouldAuthenticateSuccessfully() {
            String password = "admin123";
            User adminUser = buildUser("admin", UserRole.SYSTEM_ADMIN);
            adminUser.setPasswordHash(sha256(password));

            TypedQuery<User> mockQuery = mockUserQuery(adminUser);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);

            when(em.merge(any(User.class))).thenReturn(adminUser);

            assertThatCode(() -> authBean.login("admin", password))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Plaintext hash as password is double-hashed and rejected")
        void plaintextHash_AsPassword_IsRejected() {
            String storedHash = sha256("securePass99");
            User user = buildUser("hashtest", UserRole.LOGISTICS_COORDINATOR);
            user.setPasswordHash(storedHash);

            TypedQuery<User> mockQuery = mockUserQuery(user);
            when(em.createNamedQuery(eq("User.findByUsername"), eq(User.class)))
                    .thenReturn(mockQuery);


            assertThatThrownBy(() -> authBean.login("hashtest", storedHash))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("SHA-256 produces consistent 64-char hex output")
        void sha256_ProducesConsistentOutput() {
            String hash1 = sha256("testPassword");
            String hash2 = sha256("testPassword");

            assertThat(hash1)
                    .isEqualTo(hash2)
                    .hasSize(64)
                    .matches("[a-f0-9]+");
        }

        @Test
        @DisplayName("Different passwords produce different hashes — no collision")
        void differentPasswords_ProduceDifferentHashes() {
            String hash1 = sha256("password1");
            String hash2 = sha256("password2");
            String hash3 = sha256("Password1");

            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(hash1).isNotEqualTo(hash3);
            assertThat(hash2).isNotEqualTo(hash3);
        }
    }


    private void injectEntityManager() throws Exception {
        Field emField = AuthenticationBean.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(authBean, em);
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<User> mockUserQuery(User returnValue) {
        TypedQuery<User> mockQuery = mock(TypedQuery.class);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        if (returnValue == null) {
            when(mockQuery.getSingleResult())
                    .thenThrow(new NoResultException("No user found"));
        } else {
            when(mockQuery.getSingleResult()).thenReturn(returnValue);
        }
        return mockQuery;
    }

    private User buildUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setActive(true);
        user.setFullName("Test " + username);
        user.setEmail(username + "@test.com");
        user.setPasswordHash(sha256("password123"));
        return user;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
