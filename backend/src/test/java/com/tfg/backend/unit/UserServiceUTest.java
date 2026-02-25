package com.tfg.backend.unit;

import com.tfg.backend.dto.UserLoginDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Sustituimos Principal y Request por los componentes de Spring Security
    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    // Limpiamos el contexto después de cada test para no afectar a los siguientes
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_ShouldReturnList() {
        List<User> users = Arrays.asList(new User(), new User());
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void save_ShouldReturnSavedUser() {
        User user = new User();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void delete_ShouldCallRepository() {
        User user = new User();
        userService.delete(user);
        verify(userRepository).delete(user);
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        Long id = 1L;
        User user = new User();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(id);

        assertTrue(result.isPresent());
        verify(userRepository).findById(id);
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        String username = "user";
        User user = new User();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername(username);

        assertTrue(result.isPresent());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        String email = "test@test.com";
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        verify(userRepository).findByEmail(email);
    }

    // --- TESTS MODIFICADOS PARA USAR SECURITYCONTEXTHOLDER ---

    @Test
    void getLoginInfo_ShouldReturnEmpty_WhenNotAuthenticated() {
        // Configuramos el contexto para devolver null en la autenticación
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<UserLoginDTO> result = userService.getLoginInfo();

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void getLoginInfo_ShouldReturnDTO_WhenAuthenticated() {
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRoles(Set.of("USER"));

        // Configuramos el mock de autenticación en el contexto estático
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Optional<UserLoginDTO> result = userService.getLoginInfo();

        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
    }

    @Test
    void getLoggedUser_ShouldReturnUser_WhenAuthenticated() {
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRoles(Set.of("USER"));

        // Configuramos el mock de autenticación
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        Optional<User> result = userService.getLoggedUser();

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getLoggedUser_ShouldReturnEmpty_WhenNotAuthenticated() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<User> result = userService.getLoggedUser();

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsername(any());
    }

    // --- FIN TESTS MODIFICADOS ---

    @Test
    void registerUser_ShouldEncodePasswordAndSave() {
        UserSignupDTO dto = new UserSignupDTO();
        dto.setName("Name");
        dto.setUsername("user");
        dto.setEmail("mail@test.com");
        dto.setPassword("rawPass");

        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("encodedPass", result.getEncodedPassword());
        assertEquals("USER", result.getRoles().stream().toList().getFirst());
        verify(passwordEncoder).encode("rawPass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void booleanChecks_ShouldDelegateToRepository() {
        String username = "user";
        String email = "mail";

        when(userRepository.existsByUsername(username)).thenReturn(true);
        assertTrue(userService.existsByUsername(username));
        assertTrue(userService.isUsernameTaken(username));

        when(userRepository.existsByEmail(email)).thenReturn(true);
        assertTrue(userService.existsByEmail(email));
        assertTrue(userService.isEmailTaken(email));

        when(userRepository.existsByUsernameAndIsBannedTrue(username)).thenReturn(true);
        assertTrue(userService.isBannedByUsername(username));

        when(userRepository.existsByUsernameAndIsDeletedTrue(username)).thenReturn(true);
        assertTrue(userService.isDeletedByUsername(username));

        when(userRepository.existsByEmailAndIsBannedTrue(email)).thenReturn(true);
        assertTrue(userService.isBannedByEmail(email));

        when(userRepository.existsByEmailAndIsDeletedTrue(email)).thenReturn(true);
        assertTrue(userService.isDeletedByEmail(email));
    }
}