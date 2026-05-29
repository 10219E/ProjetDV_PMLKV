package lu.ephec.backend_projetdv2026.services;

import lombok.RequiredArgsConstructor;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JPAMatchPaymentsRepo matchPaymentsRepo;


    public List<MatchPayments> getAllSiteDetailedFinancialReport() {
        return matchPaymentsRepo.findAllClearedPaymentsWithDetails();
    }

    //Retrieves all 'clear' payments with full details (User, Match, Field, Site) ordered by payment date descending.
    public List<MatchPayments> getDetailedFinancialReport() {
        return matchPaymentsRepo.findAllClearedPaymentsWithDetails();
    }

    //Retrieves all 'clear' payments for a specific site with full details ordered by payment date descending.
    public List<MatchPayments> getDetailedFinancialReportBySite(Integer siteId) {
        return matchPaymentsRepo.findAllClearedPaymentsBySiteWithDetails(siteId);
    }
}
