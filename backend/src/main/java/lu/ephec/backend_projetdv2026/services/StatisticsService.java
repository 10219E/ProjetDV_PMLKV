package lu.ephec.backend_projetdv2026.services;

import lombok.RequiredArgsConstructor;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JPAMatchPaymentsRepo matchPaymentsRepo;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);


    public List<MatchPayments> getDetailedFinancialReport() {
        logger.info("[Service - Statistics] Getting detailed financial report.");
        return matchPaymentsRepo.findAllFinancialPaymentsWithDetails();
    }

    //Retrieves all 'clear' or 'pending' payments for a specific site with full details ordered by payment date descending.
    public List<MatchPayments> getDetailedFinancialReportBySite(Integer siteId) {
        logger.info("[Service - Statistics] Getting detailed financial report.");
        return matchPaymentsRepo.findAllFinancialPaymentsBySiteWithDetails(siteId);
    }
}
