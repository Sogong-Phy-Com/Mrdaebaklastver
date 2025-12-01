package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
        System.out.println("[JWT Filter] JwtAuthenticationFilter initialized");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();
        
        System.out.println("========== [JWT Filter] Request Processing Start ==========");
        System.out.println("[JWT Filter] Request Path: " + requestPath);
        System.out.println("[JWT Filter] Request Method: " + request.getMethod());
        System.out.println("[JWT Filter] Authorization Header: " + (authHeader != null ? "EXISTS (length: " + authHeader.length() + ")" : "MISSING"));
        if (authHeader != null) {
            System.out.println("[JWT Filter] Authorization Header Prefix: " + (authHeader.length() > 30 ? authHeader.substring(0, 30) + "..." : authHeader));
        }
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JWT Filter] WARNING: Authorization header is missing or not in Bearer format");
            System.out.println("[JWT Filter] Request Path: " + requestPath);
            
            // Check if this is a protected path
            if (requestPath.startsWith("/api/") && 
                !requestPath.startsWith("/api/auth/") && 
                !requestPath.startsWith("/api/health") && 
                !requestPath.startsWith("/api/menu/")) {
                System.out.println("[JWT Filter] WARNING: Protected path but no token provided");
            }
            
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            System.out.println("[JWT Filter] Token extracted (length: " + jwt.length() + ")");
            System.out.println("[JWT Filter] Token prefix: " + (jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt));
            
            // Extract user info first (before validation to see what's in the token)
            String userId = null;
            String role = null;
            try {
                userId = jwtService.extractUserId(jwt);
                role = jwtService.extractRole(jwt);
                System.out.println("[JWT Filter] Extracted user ID: " + userId);
                System.out.println("[JWT Filter] Extracted role: " + role);
            } catch (Exception e) {
                System.out.println("[JWT Filter] ERROR: Failed to extract user info from token");
                System.out.println("[JWT Filter] Exception type: " + e.getClass().getName());
                System.out.println("[JWT Filter] Exception message: " + e.getMessage());
                e.printStackTrace();
                filterChain.doFilter(request, response);
                return;
            }

            if (userId == null || userId.isEmpty()) {
                System.out.println("[JWT Filter] ERROR: User ID is null or empty");
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate token after extraction
            boolean isValid = false;
            try {
                isValid = jwtService.isTokenValid(jwt);
                System.out.println("[JWT Filter] Token validation result: " + isValid);
            } catch (Exception e) {
                System.out.println("[JWT Filter] ERROR: Token validation failed");
                System.out.println("[JWT Filter] Exception type: " + e.getClass().getName());
                System.out.println("[JWT Filter] Exception message: " + e.getMessage());
                e.printStackTrace();
                filterChain.doFilter(request, response);
                return;
            }
            
            if (!isValid) {
                System.out.println("[JWT Filter] WARNING: Token is invalid (expired or signature invalid)");
                System.out.println("[JWT Filter] User ID from token: " + userId);
                System.out.println("[JWT Filter] Role from token: " + role);
                filterChain.doFilter(request, response);
                return;
            }

            // Set authentication in SecurityContext
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Set default role if null or empty
                String userRole = (role != null && !role.isEmpty()) ? role.toUpperCase() : "CUSTOMER";
                String authority = "ROLE_" + userRole;
                
                System.out.println("[JWT Filter] Setting authority: " + authority);
                
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("[JWT Filter] SUCCESS: Authentication set in SecurityContext");
                System.out.println("[JWT Filter] Authenticated User ID: " + userId);
                System.out.println("[JWT Filter] Authenticated Authority: " + authority);
            } else {
                System.out.println("[JWT Filter] INFO: Already authenticated");
                Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("[JWT Filter] Existing Auth User: " + existingAuth.getName());
                System.out.println("[JWT Filter] Existing Auth Authorities: " + existingAuth.getAuthorities());
            }
        } catch (Exception e) {
            System.out.println("[JWT Filter] ERROR: Exception during token processing");
            System.out.println("[JWT Filter] Request Path: " + request.getRequestURI());
            System.out.println("[JWT Filter] Exception Type: " + e.getClass().getName());
            System.out.println("[JWT Filter] Exception Message: " + e.getMessage());
            System.out.println("[JWT Filter] Exception Stack Trace:");
            e.printStackTrace();
            
            // Check if this is a protected path
            String path = request.getRequestURI();
            if (path.startsWith("/api/") && 
                !path.startsWith("/api/auth/") && 
                !path.startsWith("/api/health") && 
                !path.startsWith("/api/menu/")) {
                System.out.println("[JWT Filter] WARNING: Protected path but token processing failed");
                System.out.println("[JWT Filter] WARNING: This request may return 401 Unauthorized");
            }
            // Token is invalid, continue without authentication
        }

        System.out.println("[JWT Filter] Before FilterChain.doFilter");
        System.out.println("[JWT Filter] SecurityContext Auth Status: " + 
            (SecurityContextHolder.getContext().getAuthentication() != null ? "AUTHENTICATED" : "NOT AUTHENTICATED"));
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("[JWT Filter] SecurityContext User: " + auth.getName());
            System.out.println("[JWT Filter] SecurityContext Authorities: " + auth.getAuthorities());
        }
        
        filterChain.doFilter(request, response);
        
        System.out.println("[JWT Filter] After FilterChain.doFilter");
        System.out.println("========== [JWT Filter] Request Processing Complete ==========");
    }
}

