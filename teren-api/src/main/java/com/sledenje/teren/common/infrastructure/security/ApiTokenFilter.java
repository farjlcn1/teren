package com.sledenje.teren.common.infrastructure.security;

import com.sledenje.teren.common.infrastructure.config.TerenApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Vsi zahtevki na {@code /api/**} morajo imeti glavo {@code Authorization: Bearer <API_TOKEN>}.
 * <p>
 * Žeton pozna samo aplikacija Teren, ki kliče API v imenu prijavljenega uporabnika in sama preveri
 * njegove pravice. Health in Swagger nista pod {@code /api}, zato žetona ne potrebujeta.
 */
@Component
public class ApiTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String UNAUTHORIZED_BODY = """
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Manjka ali neveljaven API žeton."}""";

    private final byte[] expectedToken;

    public ApiTokenFilter(TerenApiProperties terenApiProperties) {
        this.expectedToken = terenApiProperties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (hasValidToken(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Zavrnjen zahtevek brez veljavnega API žetona: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(UNAUTHORIZED_BODY);
    }

    private boolean hasValidToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] providedToken = authorizationHeader.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        // Primerjava v konstantnem času, da se žetona ne da uganiti po času odziva.
        return MessageDigest.isEqual(expectedToken, providedToken);
    }

}
