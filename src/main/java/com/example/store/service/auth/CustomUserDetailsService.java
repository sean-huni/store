package com.example.store.service.auth;

import com.example.store.persistence.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return userRepo.findByEmail(username)
                .orElseThrow(() -> {
                    final String errorMessage = messageSource.getMessage("auth.400.010", new Object[]{username}, "User not found with email: %s".formatted(username), Locale.getDefault());
                    return new UsernameNotFoundException(errorMessage);
                });
    }
}