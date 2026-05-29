package lu.ephec.backend_projetdv2026.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lu.ephec.backend_projetdv2026.dto.FinancialRecordDto;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.services.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class StatisticsController {

    private final StatisticsService statisticsService;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @GetMapping(value = "/financial-report", produces = "application/json")
    public ResponseEntity<List<FinancialRecordDto>> getFinancialReport() {
        logger.info("[STATS CONTROLLER] Initiating financial report.");
        return ResponseEntity.ok(mapToDto(statisticsService.getDetailedFinancialReport()));
    }

    @GetMapping(value = "/financial-report/{siteId}", produces = "application/json")
    public ResponseEntity<List<FinancialRecordDto>> getFinancialReportBySite(
            @Parameter(description = "ID of the site", required = true)
            @PathVariable Integer siteId) {
        logger.info("[STATS CONTROLLER] Initiating financial report for site {}", siteId);
        return ResponseEntity.ok(mapToDto(statisticsService.getDetailedFinancialReportBySite(siteId)));
    }

    private List<FinancialRecordDto> mapToDto(List<MatchPayments> payments) {
        return payments.stream()
                .map(p -> new FinancialRecordDto(
                        p.getPaymentDate(),
                        p.getAmount(),
                        p.getUser().getFirstName() + " " + p.getUser().getLastName(),
                        p.getMatch().getField().getSite().getName(),
                        p.getPaymentMethod(),
                        p.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
