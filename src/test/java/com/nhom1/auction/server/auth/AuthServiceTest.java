package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UserAlreadyExistsException;
import com.nhom1.auction.server.infrastructure.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, notificationService);
    }

    @Test
    public void testLogin_CorrectCredentials_ReturnsUser() throws AuthenticationException {
        String identifier = "testuser";
        String password = "password";
        User user = new User("testuser", "test@example.com", "password", UserRole.USER);
        when(userRepository.findByIdentifier(identifier)).thenReturn(Optional.of(user));

        User result = authService.login(identifier, password);

        assertEquals(user, result);
    }

    @Test
    public void testLogin_WrongPassword_ThrowsAuthenticationException() {
        String identifier = "testuser";
        String password = "wrongpassword";
        User user = new User("testuser", "test@example.com", "password", UserRole.USER);
        when(userRepository.findByIdentifier(identifier)).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () -> authService.login(identifier, password));
    }

    @Test
    public void testLogin_UnknownIdentifier_ThrowsAuthenticationException() {
        String identifier = "unknown";
        String password = "password";
        when(userRepository.findByIdentifier(identifier)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authService.login(identifier, password));
    }

    @Test
    public void testLogin_RepositoryFailure_PropagatesRuntimeException() {
        String identifier = "testuser";
        String password = "password";
        RuntimeException dbFailure = new RuntimeException("Failed to find user by identifier");
        when(userRepository.findByIdentifier(identifier)).thenThrow(dbFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> authService.login(identifier, password));
        assertSame(dbFailure, thrown);
    }

    @Test
    public void testRegister_NewUser_SavesAndReturnsUser() throws UserAlreadyExistsException {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);

        User result = authService.register(username, email, password);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testRegister_DuplicateEmail_ThrowsUserAlreadyExistsException() {
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(username, email, password));
    }

    @Test
    public void testRegister_DuplicateUsername_ThrowsUserAlreadyExistsException() {
        String username = "existinguser";
        String email = "new@example.com";
        String password = "password";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(username, email, password));
    }

    @Test
    public void testRegister_EmailCheckRepositoryFailure_PropagatesRuntimeException() {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password";
        RuntimeException dbFailure = new RuntimeException("Failed to check email existence");
        when(userRepository.existsByEmail(email)).thenThrow(dbFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> authService.register(username, email, password));
        assertSame(dbFailure, thrown);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRegister_UsernameCheckRepositoryFailure_PropagatesRuntimeException() {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password";
        RuntimeException dbFailure = new RuntimeException("Failed to check username existence");
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenThrow(dbFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> authService.register(username, email, password));
        assertSame(dbFailure, thrown);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRegister_SaveRepositoryFailure_PropagatesRuntimeException() {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password";
        RuntimeException dbFailure = new RuntimeException("Failed to save user");
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        doThrow(dbFailure).when(userRepository).save(any(User.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> authService.register(username, email, password));
        assertSame(dbFailure, thrown);
    }
}
