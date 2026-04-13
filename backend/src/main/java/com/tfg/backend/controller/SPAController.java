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

    @RequestMapping(value = { "/{path:^(?!api).*$}/**/{path:[^\\.]*}", "/{path:^(?!api).*$}" })
    public String forward() {
        // "forward:" keeps the browser URL unchanged (it stays as /dashboard)
        // but instructs the server to render the content of static/index.html
        return "forward:/index.html";
    }
}
