package com.example.store.component.auth.entrypoint;

import com.example.store.dto.error.ErrorDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final Gson gson;
    private final MessageSource messageSource;

    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage(), authException);
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final String errorMessage = messageSource.getMessage("auth.400.012", null, "Full authentication is required to access this resource", Locale.getDefault());
       
       final ErrorDTO errorDTO = new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
               errorMessage,
                null,
                ZonedDateTime.now()
        );

        response.getWriter().write(gson.toJson(errorDTO));
    }
}
