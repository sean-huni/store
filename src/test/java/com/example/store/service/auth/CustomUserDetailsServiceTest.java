package com.example.store.service.auth;

import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

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
        
        assertEquals("User not found with email: " + email, exception.getMessage());
    }
}