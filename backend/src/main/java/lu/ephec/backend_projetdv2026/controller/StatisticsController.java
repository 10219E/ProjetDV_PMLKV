package lu.ephec.backend_projetdv2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lu.ephec.backend_projetdv2026.dto.FinancialRecordDto;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.services.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Endpoints for financial and administrative reporting")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping(value = "/financial-report", produces = "application/json")
    @Operation(summary = "Get detailed financial report of all cleared payments")
    public ResponseEntity<List<FinancialRecordDto>> getFinancialReport() {
        return ResponseEntity.ok(mapToDto(statisticsService.getDetailedFinancialReport()));
    }

    @GetMapping(value = "/financial-report/{siteId}", produces = "application/json")
    @Operation(summary = "Get detailed financial report of all cleared payments for a specific site")
    public ResponseEntity<List<FinancialRecordDto>> getFinancialReportBySite(
            @Parameter(description = "ID of the site", required = true)
            @PathVariable Integer siteId) {
        return ResponseEntity.ok(mapToDto(statisticsService.getDetailedFinancialReportBySite(siteId)));
    }

    private List<FinancialRecordDto> mapToDto(List<MatchPayments> payments) {
        return payments.stream()
                .map(p -> new FinancialRecordDto(
                        p.getPaymentDate(),
                        p.getAmount(),
                        p.getUser().getFirstName() + " " + p.getUser().getLastName(),
                        p.getMatch().getField().getSite().getName(),
                        p.getPaymentMethod()
                ))
                .collect(Collectors.toList());
    }
}
