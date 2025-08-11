package com.example.store.service.auth;

import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        // Setup MessageSource with lenient stubbing for common messages
        lenient().when(messageSource.getMessage(eq("auth.400.010"), any(), any(), any(Locale.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArgument(1);
                    return "User not found with email: " + args[0];
                });
    }

    @Test
    @DisplayName("Should load user by username when user exists")
    void shouldLoadUserByUsernameWhenUserExists() {
        // Given
        String email = "user@example.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        
        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // Then
        assertEquals(email, userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }
    
    @Test
    @DisplayName("Should throw UsernameNotFoundException when user doesn't exist")
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesntExist() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());
        
        // When/Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
        );

        // Verify the exception message matches what would be returned by the MessageSource
        String expectedMessage = messageSource.getMessage("auth.400.010", new Object[]{email}, "User not found with email: " + email, Locale.getDefault());
        assertEquals(expectedMessage, exception.getMessage());
    }
}