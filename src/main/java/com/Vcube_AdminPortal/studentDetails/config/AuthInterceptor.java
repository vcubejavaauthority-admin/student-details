package com.Vcube_AdminPortal.studentDetails.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Allow public pages, static resources, and auth endpoints
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/auth/login") || uri.equals("/auth/logout")
                || uri.startsWith("/css") || uri.startsWith("/js")
                || uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png")
                || uri.endsWith(".jpg") || uri.endsWith(".ico")
                || uri.endsWith(".woff") || uri.endsWith(".woff2")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
