package lu.ephec.backend_projetdv2026.services.sitefieldbymatch;

import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.repo.JPASiteRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SiteFieldsByMatchService {

    private final JPAFieldRepo jpaFieldRepo;
    private final JPASiteRepo jpaSiteRepo;
    private static final Logger logger = LoggerFactory.getLogger(SiteFieldsByMatchService.class);

    public SiteFieldsByMatchService(JPAFieldRepo jpaFieldRepo, JPASiteRepo jpaSiteRepo) {
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaSiteRepo = jpaSiteRepo;
    }

    /**
     * Finds all unique sites and their fields from a list of matches.
     *
     * @param matches List of matches to process
     * @return Map where key is Site and value is List of Fields for that site
     */
    public Map<Site, List<Field>> findSitesAndFieldsForMatches(List<Match> matches) {
        // Validate input
        if (matches == null || matches.isEmpty()) {
            logger.warn("[Service - SiteFieldsByMatch]Empty or null matches list provided");
            return Collections.emptyMap();
        }

        // Create a map to store sites and their fields
        Map<Site, List<Field>> siteFieldMap = new LinkedHashMap<>();

        // Process each match
        for (Match match : matches) {
            if (match == null || match.getField() == null) {
                logger.debug("[Service - SiteFieldsByMatch] Skipping null match or match without field");
                continue; // Skip null matches or matches without fields
            }

            Field field = match.getField();

            // Ensure we have the complete field and site information
            Field completeField = jpaFieldRepo.findById(field.getFieldId())
                    .orElse(null);

            if (completeField == null || completeField.getSite() == null) {
                logger.debug("[Service - SiteFieldsByMatch] Skipping field without complete site information: {}", field.getFieldId());
                continue; // Skip fields without complete site information
            }

            Site site = completeField.getSite();

            // If site is not already in the map, add it with an empty list
            siteFieldMap.computeIfAbsent(site, k -> new ArrayList<>());

            // Add the field to the site's list if it's not already there
            List<Field> fields = siteFieldMap.get(site);
            if (!fields.contains(completeField)) {
                fields.add(completeField);
            }
        }

        logger.info("[Service - SiteFieldsByMatch] Found {} unique sites with fields for {} matches", siteFieldMap.size(), matches.size());
        return siteFieldMap;
    }
}