package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.FieldDto;
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
    public ResponseEntity<List<FieldDto>> getAllFields() {
        List<FieldDto> responses = fieldService.fetchAll().stream()
                .map(FieldDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value ="/{id}", produces = "application/json")
    public ResponseEntity<FieldDto> getFieldById(@PathVariable Integer id) {
        Field field = fieldService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(FieldDto.from(field));
    }

    @GetMapping(value= "/site/{siteId}", produces = "application/json")
    public ResponseEntity<List<FieldDto>> getFieldsBySite(@PathVariable Integer siteId) {
        List<FieldDto> responses = fieldService.fetchBySite(siteId).stream()
                .map(FieldDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/site/{siteId}/active", produces = "application/json")
    public ResponseEntity<List<FieldDto>> getActiveFieldsBySite(@PathVariable Integer siteId) {
        List<FieldDto> responses = fieldService.fetchActiveFieldsByActiveSite(siteId).stream()
                .map(FieldDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value="/{id}/maintenance", produces = "application/json")
    public ResponseEntity<FieldDto> getFieldMaintenanceById(@PathVariable Integer id) {
        // Return maintenance date range for a specific field (from/to)
        Field field = fieldService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(FieldDto.from(field));
    }

}

