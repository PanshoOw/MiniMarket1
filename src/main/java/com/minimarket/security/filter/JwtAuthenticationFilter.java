package com.minimarket.security.filter;

import com.minimarket.security.service.CustomUserDetailsService;
import com.minimarket.security.util.JwtUtil;
import com.minimarket.service.AuditoriaService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USUARIO_ANONIMO = "ANONIMO";
    private static final String TIPO_TOKEN_INVALIDO = "TOKEN_INVALIDO";
    private static final String TIPO_AUTENTICACION_FALLIDA = "AUTENTICACION_FALLIDA";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuditoriaService auditoriaService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   CustomUserDetailsService customUserDetailsService,
                                   AuditoriaService auditoriaService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.auditoriaService = auditoriaService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            jwt = authorizationHeader.substring(BEARER_PREFIX.length());

            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (JwtException | IllegalArgumentException e) {
                logger.warn("Token JWT inválido o malformado");

                auditoriaService.registrarEvento(
                        USUARIO_ANONIMO,
                        TIPO_TOKEN_INVALIDO,
                        request.getRemoteAddr(),
                        "Intento de acceso con token JWT inválido en: " + request.getRequestURI()
                );
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (UsernameNotFoundException | JwtException | IllegalArgumentException e) {
                logger.warn("No se pudo autenticar con el token JWT proporcionado");

                auditoriaService.registrarEvento(
                        username,
                        TIPO_AUTENTICACION_FALLIDA,
                        request.getRemoteAddr(),
                        "Fallo de autenticación JWT en: " + request.getRequestURI()
                );
            }
        }

        filterChain.doFilter(request, response);
    }
}