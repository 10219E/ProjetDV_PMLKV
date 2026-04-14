package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.FieldResponse;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.services.FieldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fields")
public class FieldController {

    private final FieldService fieldService;

    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<FieldResponse>> getAllFields() {
        List<FieldResponse> responses = fieldService.fetchAll().stream()
                .map(FieldResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value ="/{id}", produces = "application/json")
    public ResponseEntity<FieldResponse> getFieldById(@PathVariable Integer id) {
        Field field = fieldService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(FieldResponse.from(field));
    }

    @GetMapping(value= "/site/{siteId}", produces = "application/json")
    public ResponseEntity<List<FieldResponse>> getFieldsBySite(@PathVariable Integer siteId) {
        List<FieldResponse> responses = fieldService.fetchBySite(siteId).stream()
                .map(FieldResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/site/{siteId}/active", produces = "application/json")
    public ResponseEntity<List<FieldResponse>> getActiveFieldsBySite(@PathVariable Integer siteId) {
        List<FieldResponse> responses = fieldService.fetchActiveFieldsByActiveSite(siteId).stream()
                .map(FieldResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

