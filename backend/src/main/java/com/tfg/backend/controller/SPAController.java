package com.tfg.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * SPAController (Single Page Application Controller)
 * * This controller is essential for the "Monolithic Deployment" strategy where Spring Boot
 * serves the Angular frontend. It becomes critical in Docker and production environments,
 * where there is no independent Angular server (Vite). In this context, Spring Boot takes
 * responsibility for the frontend by serving the compiled files from the
 * src/main/resources/static folder, effectively handling the "HTML5 History Mode" routing.
 */
@Controller
public class SPAController {

    /**
     * Forwards all non-static file requests to the Angular index.html.
     * * LOGIC:
     * 1. Angular handles routing on the client side (e.g., /login, /dashboard).
     * 2. If the user refreshes the page (F5), the browser asks the server for that path.
     * 3. Since that path doesn't exist in Spring Boot, this controller catches it
     * and returns the index.html, allowing Angular to reload and handle the route.
     * * REGEX EXPLANATION:
     * - "/**": Matches any path hierarchy.
     * - "{path:[^\\.]*}": Matches a path variable ONLY if it does NOT contain a dot (.).
     * * WHY EXCLUDE DOTS?
     * - The objective is to catch routes like "/dashboard" or "/users/list".
     * - Not to catch static files like "styles.css", "image.png", or "main.js".
     * Static files must be served directly by the server, not forwarded to index.html.
     */
    @RequestMapping(value = {
            "/",
            "/{path:[^\\.]*}",          // Catches top-level routes (e.g., /login)
            "/**/{path:[^\\.]*}"        // Catches nested routes (e.g., /users/profile/123)
    }, method = RequestMethod.GET)
    public String forward() {
        // "forward:" keeps the browser URL unchanged (it stays as /dashboard)
        // but instructs the server to render the content of static/index.html
        return "forward:/index.html";
    }
}
