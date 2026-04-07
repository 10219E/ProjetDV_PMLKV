package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.validation.SiteSessionsJsonHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service //BEAN
public class SiteService {

    private final JPASiteRepo jpaSiteRepo;
    private final JPASiteClosureDaysRepo jpaClosureDaysRepo;
    private final SiteSessionsJsonHandler siteSessionsJsonHandler;
    private final JPASiteSessionsRepo jpaSiteSessionsRepo;
    private final JPAFieldRepo jpaFieldRepo;
    private final JPAMatchRepo jpaMatchRepo;

    // InjDep Interface Sites
    public SiteService(JPASiteRepo jpaSiteRepo, JPASiteClosureDaysRepo jpaClosureDaysRepo, SiteSessionsJsonHandler siteSessionsJsonHandler, JPASiteSessionsRepo jpaSiteSessionsRepo, FieldService fieldService, JPAFieldRepo jpaFieldRepo, JPAMatchRepo jpaMatchRepo) {

        this.jpaSiteRepo = jpaSiteRepo;
        this.jpaClosureDaysRepo = jpaClosureDaysRepo;
        this.siteSessionsJsonHandler = siteSessionsJsonHandler;
        this.jpaSiteSessionsRepo = jpaSiteSessionsRepo;
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaMatchRepo = jpaMatchRepo;
    }

    ////SITES OPERATIONS////

    //CHECK EXISTS
    public boolean siteExists(Integer siteId) {
        return jpaSiteRepo.existsById(siteId);
    }

    // SET Site
    @Transactional
    public Site newSite(Site site) {
        // Validate inputs
        ValidationBoiler.verifyNotEmpty(site.getName(), "Site name");
        ValidationBoiler.verifyNotEmpty(site.getAddress(), "Site address");
        ValidationBoiler.verifyNotNull(site.getOpeningTime(), "Site opening time");
        ValidationBoiler.verifyNotNull(site.getClosingTime(), "Site closing time");

        // Validate site hours are sufficient for at least one session
        ValidationBoiler.verifyEnoughSiteHours(site.getOpeningTime(), site.getClosingTime());


        Site savedSite = jpaSiteRepo.save(site);

        //Check if no hours exist for this site yet
        ValidationBoiler.verifyNotExists(jpaSiteSessionsRepo.existsBySite_SiteId(savedSite.getSiteId()),
                "Site Sessions", savedSite.getSiteId());


        String sessionsJson = siteSessionsJsonHandler.generateSessionsJson(
                savedSite.getOpeningTime(),
                savedSite.getClosingTime()
        );

        SiteSessions siteSessions = new SiteSessions(null, savedSite, sessionsJson);
        jpaSiteSessionsRepo.save(siteSessions);

        return savedSite;
    }

    //GET Site by ID
    public Optional<Site> fetchById(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaSiteRepo.findById(siteId);}

    //GET Site by Name
    public Optional<Site> fetchByName(String siteName) {
        ValidationBoiler.verifyNotEmpty(siteName, "Site name");
        return jpaSiteRepo.findByName(siteName);
    }

    //GET Site by Address
    public Optional<Site> fetchByAddress(String siteAddress) {
        ValidationBoiler.verifyNotEmpty(siteAddress, "Site address");
        return jpaSiteRepo.findByAddress(siteAddress);
    }

    //GET ALL Sites
    public List<Site> fetchAll() { return jpaSiteRepo.findAll(); }

    //GET ALL ACTIVE Sites
    public List<Site> fetchAllActive() {
        return jpaSiteRepo.findAllByIsActiveTrue();
    }

    //GET ALL INACTIVE Sites
    public List<Site> fetchAllInactive() {
        return jpaSiteRepo.findAllByIsActiveFalse();
    }

    //GET ALL By Opening TIME
    public List<Site> fetchByOpeningTime(LocalTime openingTime) {
        ValidationBoiler.verifyNotNull(openingTime, "Site opening time");
        return jpaSiteRepo.findByOpeningTime(openingTime);
    }

    //GET ALL By Closing TIME
    public List<Site> fetchByClosingTime(LocalTime closingTime) {
        ValidationBoiler.verifyNotNull(closingTime, "Site closing time");
        return jpaSiteRepo.findByClosingTime(closingTime);
    }

    //DELETE Site -- ONLY SUPER ADMIN
    @Transactional
    public void deleteSite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);

        // DELETE CLOSURES
        List<SiteClosureDays> closures = jpaClosureDaysRepo.findBySiteId(siteId);
        jpaClosureDaysRepo.deleteAll(closures);

        // DELETE SESSIONS
        Optional<SiteSessions> siteSessions = jpaSiteSessionsRepo.findBySite_SiteId(siteId);
        if (siteSessions.isPresent()) {
            jpaSiteSessionsRepo.delete(siteSessions.get());
        }

        // DELETE FIELDS
        List<Field> fields = jpaFieldRepo.findBySite_SiteId(siteId);
        if (!fields.isEmpty()) {
            jpaFieldRepo.deleteAll(fields);
        }

        // SET ALL MATCHES ON THIS SITE AS "Cancelled"
        List<Match> matchesOnSite = jpaMatchRepo.findBySiteId(siteId);
        matchesOnSite.forEach(match -> {
            if (match.getType().equals("public")) {
                match.setPubStatus("cancelled");
            } else if (match.getType().equals("private")) {
                match.setPrivStatus("cancelled");
            }
        });
        jpaMatchRepo.saveAll(matchesOnSite);

        jpaSiteRepo.deleteById(siteId);
    }

    //UPDATE Site + SITE Sessions if hours changed
    @Transactional
    public Optional<Site> updateSite(Integer siteId, Site updateData) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyNotNull(updateData, "Update data");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        AtomicBoolean hoursChanged = new AtomicBoolean(false);
        AtomicBoolean siteDeactivated = new AtomicBoolean(false);

        return jpaSiteRepo.findById(siteId).map(site -> {
            if (updateData.getName() != null) {
                ValidationBoiler.verifyNotEmpty(updateData.getName(), "Site name");
                site.setName(updateData.getName());
            }
            if (updateData.getAddress() != null) {
                ValidationBoiler.verifyNotEmpty(updateData.getAddress(), "Site address");
                site.setAddress(updateData.getAddress());
            }
            if (updateData.getOpeningTime() != null && !updateData.getOpeningTime().equals(site.getOpeningTime())) {
                site.setOpeningTime(updateData.getOpeningTime());
                hoursChanged.set(true);
            }
            if (updateData.getClosingTime() != null && !updateData.getClosingTime().equals(site.getClosingTime())) {
                site.setClosingTime(updateData.getClosingTime());
                hoursChanged.set(true);
            }

            if (updateData.getIsActive() != null && !updateData.getIsActive() && site.getIsActive()) {
                siteDeactivated.set(true);
            }

            if (updateData.getIsActive() != null) {
                site.setIsActive(updateData.getIsActive());
            }

            Site updatedSite = jpaSiteRepo.save(site);

            //IF SITE IS SET TO INACTIVE
            if (siteDeactivated.get()) {
                //DEACTIVATING FIELDS
                List<Field> fields = jpaFieldRepo.findBySite_SiteId(updatedSite.getSiteId());
                fields.forEach(field -> {
                    field.setIsActive(false);
                });
                jpaFieldRepo.saveAll(fields);

                // SET ALL MATCHES ON THIS SITE AS "Cancelled"
                List<Match> matchesOnSite = jpaMatchRepo.findBySiteId(siteId);
                matchesOnSite.forEach(match -> {
                    if (match.getType().equals("public")) {
                        match.setPubStatus("cancelled");
                    } else if (match.getType().equals("private")) {
                        match.setPrivStatus("cancelled");
                    }
                });
                jpaMatchRepo.saveAll(matchesOnSite);
            }



            //IF HOURS CHANGED, WE MUST REGENERATE SESSIONS
            if (hoursChanged.get()) {
                // Validate new hours are sufficient
                ValidationBoiler.verifyEnoughSiteHours(updatedSite.getOpeningTime(), updatedSite.getClosingTime());
                //ValidationBoiler.verifyExists(jpaSiteSessionsRepo.existsBySiteId(updatedSite.getSiteId()),
                //       "Site Sessions", updatedSite.getSiteId()); -- Better to handle with the ELSE and create the sessions if NULL

                // Generate new sessions JSON
                String newSessionsJson = siteSessionsJsonHandler.generateSessionsJson(
                        updatedSite.getOpeningTime(),
                        updatedSite.getClosingTime()
                );

                // Update or create SiteSessions
                Optional<SiteSessions> existingSessions = jpaSiteSessionsRepo.findBySite_SiteId(updatedSite.getSiteId());

                if (existingSessions.isPresent()) {
                    // Update existing sessions
                    SiteSessions sessions = existingSessions.get();
                    sessions.setMatchSessionsJson(newSessionsJson);
                    jpaSiteSessionsRepo.save(sessions);
                } else {
                    // Create new sessions (JUST IN CASE IF NEW SITE FAILED ?)
                    SiteSessions siteSessions = new SiteSessions(null, updatedSite, newSessionsJson);
                    jpaSiteSessionsRepo.save(siteSessions);
                }
            }

            return updatedSite;
        });
    }
    ////SESSION TIMES////

    // GET SESSION TIMES FOR A SITE (parsed from JSON)
    public List<?> fetchSessionTimesForSite(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);

        Optional<SiteSessions> siteSessions = jpaSiteSessionsRepo.findBySite_SiteId(siteId);

        if (siteSessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No sessions found for site: " + siteId);
        }

        try {
            String jsonString = siteSessions.get().getMatchSessionsJson();
            com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(jsonString);

            com.fasterxml.jackson.databind.JsonNode sessionsArray = rootNode.get("sessions");

            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .convertValue(sessionsArray, new com.fasterxml.jackson.core.type.TypeReference<>() {});

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error parsing session JSON: " + e.getMessage());
        }
    }

    ////CLOSURE DAYS////

    // SET MULTIPLE DATES TO ONE SITE
    @Transactional
    public List<SiteClosureDays> newClosuresOneSite(Integer siteId, List<LocalDate> closureDates, String reason) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        ValidationBoiler.verifyListNotEmpty(closureDates, "Closure dates");
        ValidationBoiler.verifyNotEmpty(reason, "Closure reason");

        return closureDates.stream()
                .peek(date -> {
                    ValidationBoiler.verifyNotNull(date, "Closure date item");
                    ValidationBoiler.verifyDatesValid(LocalDate.now(), date, "Closure date must be in the future");
                })
                .filter(date -> !jpaClosureDaysRepo.existsBySiteIdAndClosureDate(siteId, date))
                .map(date -> new SiteClosureDays(null, siteId, date, reason, null))
                .map(jpaClosureDaysRepo::save)
                .toList();
    }

    // SET MULTIPLE DATES TO MULTIPLE SITES
    @Transactional
    public List<SiteClosureDays> newClosureMultiSite(List<Integer> siteIds, List<LocalDate> closureDates, String reason) {
        ValidationBoiler.verifyListNotEmpty(siteIds, "Site IDs");
        ValidationBoiler.verifyListNotEmpty(closureDates, "Closure dates");
        ValidationBoiler.verifyNotEmpty(reason, "Closure reason");


        closureDates.forEach(date -> {
            ValidationBoiler.verifyDatesValid(LocalDate.now(), date, "Closure date must be in the future");
        });

        List<SiteClosureDays> result = new java.util.ArrayList<>();
        for (Integer siteId : siteIds) {
            if (jpaSiteRepo.existsById(siteId)) {
                closureDates.stream()
                        .filter(date -> !jpaClosureDaysRepo.existsBySiteIdAndClosureDate(siteId, date))
                        .map(date -> new SiteClosureDays(null, siteId, date, reason, null))
                        .map(jpaClosureDaysRepo::save)
                        .forEach(result::add);
            }
        }
        return result;
    }

    //GET ALL
    public List <SiteClosureDays> fetchAllClosures(){
        return jpaClosureDaysRepo.findAll();
    }

    //FETCH BY SPECIFIC DATE RANGE
    public List<SiteClosureDays> fetchClosuresByDateRange(LocalDate startDate, LocalDate endDate) {
        ValidationBoiler.verifyNotNull(startDate, "Start date");
        ValidationBoiler.verifyNotNull(endDate, "End date");
        ValidationBoiler.verifyDatesValid(startDate, endDate, "Date range");

        return jpaClosureDaysRepo.findByClosureDateBetween(startDate, endDate);
    }

    //FETCH BY SITE
    public List<SiteClosureDays> fetchClosureForSite(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaClosureDaysRepo.findBySiteId(siteId);
    }

    // DELETE CLOSURE DAY for specific Site and Date
    @Transactional
    public void deleteClosureDayForSite(Integer siteId, LocalDate closureDate) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        ValidationBoiler.verifyNotNull(closureDate, "Closure date");

        Optional<SiteClosureDays> closure = jpaClosureDaysRepo.findBySiteIdAndClosureDate(siteId, closureDate);
        if (closure.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No closure day found for site " + siteId + " on " + closureDate);
        }
        jpaClosureDaysRepo.delete(closure.get());
    }

    // DELETE ALL CLOSURES FOR A SITE
    @Transactional
    public void deleteAllClosuresForSite(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        List<SiteClosureDays> closures = jpaClosureDaysRepo.findBySiteId(siteId);
        if (!closures.isEmpty()) {
            jpaClosureDaysRepo.deleteAll(closures);
        }
    }

    // DELETE ALL CLOSURE DAYS for a specific Date (ALL SITES)
    @Transactional
    public void deleteClosureDayForAllSites(LocalDate closureDate) {
        ValidationBoiler.verifyNotNull(closureDate, "Closure date");

        List<SiteClosureDays> closures = jpaClosureDaysRepo.findByClosureDate(closureDate);

        if (closures.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No closure days found for date " + closureDate);
        }

        jpaClosureDaysRepo.deleteAll(closures);
    }
}


