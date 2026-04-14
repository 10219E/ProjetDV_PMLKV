package lu.ephec.backend_projetdv2026.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestLoginController {

    @GetMapping(value = "/secure/ping", produces = "application/json")
    public String ping() {
        return "pong";
    }
}
