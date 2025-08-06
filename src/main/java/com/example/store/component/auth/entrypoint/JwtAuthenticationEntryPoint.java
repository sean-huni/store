package com.example.store.component.auth.entrypoint;

import com.example.store.dto.error.ErrorDTO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final Gson gson;

    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException) throws IOException, ServletException {
        
        log.error("Unauthorized error: {}", authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

       final ErrorDTO errorDTO = new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                "Full authentication is required to access this resource",
                null,
                ZonedDateTime.now()
        );

        response.getWriter().write(gson.toJson(errorDTO));
    }
}
