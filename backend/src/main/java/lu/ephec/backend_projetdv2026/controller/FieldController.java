package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.FieldDto;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.services.FieldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @PostMapping(produces = "application/json", consumes = "application/json")
    public ResponseEntity<FieldDto> newField(@RequestBody FieldDto fieldDto) {
        Field field = new Field();
        field.setIsIndoor(fieldDto.getIsIndoor());
        field.setIsActive(fieldDto.getIsActive() != null ? fieldDto.getIsActive() : true);
        field.setMaintenanceFromDate(fieldDto.getMaintenanceFromDate());
        field.setMaintenanceToDate(fieldDto.getMaintenanceToDate());

        if (fieldDto.getSiteId() != null) {
            Site site = new Site();
            site.setSiteId(fieldDto.getSiteId());
            field.setSite(site);
        }

        Field saved = fieldService.newField(field);
        return ResponseEntity.ok(FieldDto.from(saved));
    }

    @PatchMapping(value = "/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<FieldDto> updateField(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        if (!fieldService.fieldExists(id)) {
            return ResponseEntity.notFound().build();
        }

        Field updateData = new Field();

        if (updates.containsKey("isIndoor")) {
            updateData.setIsIndoor((Boolean) updates.get("isIndoor"));
        }

        if (updates.containsKey("isActive")) {
            updateData.setIsActive((Boolean) updates.get("isActive"));
        }

        if (updates.containsKey("maintenanceFromDate")) {
            String date = (String) updates.get("maintenanceFromDate");
            if (date != null) {
                updateData.setMaintenanceFromDate(LocalDate.parse(date));
            }
        }

        if (updates.containsKey("maintenanceToDate")) {
            String date = (String) updates.get("maintenanceToDate");
            if (date != null) {
                updateData.setMaintenanceToDate(LocalDate.parse(date));
            }
        }

        return fieldService.updateField(id, updateData)
                .map(updated -> ResponseEntity.ok(FieldDto.from(updated)))
                .orElse(ResponseEntity.badRequest().build());
    }

}
