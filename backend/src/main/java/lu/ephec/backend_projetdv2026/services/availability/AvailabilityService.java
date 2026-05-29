package lu.ephec.backend_projetdv2026.services.availability;

import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.SiteSessions;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPASiteSessionsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserBookingRepo;
import lu.ephec.backend_projetdv2026.services.validation.SiteSessionsJsonHandler;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {

    private final JPASiteSessionsRepo jpaSiteSessionsRepo;
    private final JPAFieldRepo jpaFieldRepo;
    private final JPAMatchRepo jpaMatchRepo;
    private final JPAUserBookingRepo jpaUserBookingRepo;
    private final SiteSessionsJsonHandler siteSessionsJsonHandler;
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    public AvailabilityService(JPASiteSessionsRepo jpaSiteSessionsRepo, JPAFieldRepo jpaFieldRepo,
                               JPAMatchRepo jpaMatchRepo, JPAUserBookingRepo jpaUserBookingRepo,
                               SiteSessionsJsonHandler siteSessionsJsonHandler) {
        this.jpaSiteSessionsRepo = jpaSiteSessionsRepo;
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaUserBookingRepo = jpaUserBookingRepo;
        this.siteSessionsJsonHandler = siteSessionsJsonHandler;
    }

    //GETS AVAILABLE SESSIONS FOR A FIELD ON A SPECIFIC DATE
    public List<Session> getAvailableSessions(Integer siteId, Integer fieldId, LocalDate date) {
        // Validate inputs
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyNotNull(fieldId, "Field ID");
        ValidationBoiler.verifyNotNull(date, "Date");
        ValidationBoiler.verifyDatesValid(LocalDate.now(), date, "Date must be in the future");
        ValidationBoiler.verifyExists(jpaSiteSessionsRepo.existsById(siteId), "Site", siteId);
        ValidationBoiler.verifyExists(jpaFieldRepo.existsById(fieldId), "Field", fieldId);

        // Get all sessions for the site
        Optional<SiteSessions> siteSessionsOpt = jpaSiteSessionsRepo.findBySite_SiteId(siteId);
        if (siteSessionsOpt.isEmpty()) {
            logger.error("[Service - Availability] No sessions found for site: " + siteId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No sessions found for site: " + siteId);
        }

        SiteSessions siteSessions = siteSessionsOpt.get();
        List<Session> allSessions = siteSessionsJsonHandler.parseSessionsJson(siteSessions.getMatchSessionsJson());

        // Verify field exists and belongs to the site
        Field field = jpaFieldRepo.findById(fieldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Field not found with id: " + fieldId));

        if (!field.getSite().getSiteId().equals(siteId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Field " + fieldId + " does not belong to site " + siteId);
        }

        // Get all matches on the specified date for the field
        List<Match> matchesOnDate = jpaMatchRepo.findByField_FieldId(fieldId);

        // Filter out sessions that are already booked
        List<Session> availableSessions = new ArrayList<>();
        for (Session session : allSessions) {
            boolean isBooked = matchesOnDate.stream().anyMatch(match ->
                match.getMatchDate().equals(date) &&
                match.getStartTime().equals(session.getStartTime()) &&
                match.getEndTime().equals(session.getEndTime()) &&
                match.getField().getFieldId().equals(fieldId)
            );

            if (!isBooked) {
                availableSessions.add(session);
            }
        }
        logger.info("[Service - Availability] Available sessions for field " + fieldId + " on " + date + "returned");

        return availableSessions;
    }

    //Class to represent a session
    public static class Session {
        private LocalTime startTime;
        private LocalTime endTime;

        // Getters and setters
        public LocalTime getStartTime() {return startTime;}

        public void setStartTime(LocalTime startTime) {this.startTime = startTime;}

        public LocalTime getEndTime() {return endTime;}

        public void setEndTime(LocalTime endTime) {this.endTime = endTime;}

    }
    
    ///BOOKING-RESERVATION LOGIC

    public LocalDate getBookingEndDate(Short roleId, LocalDate startDate) {
        int range = jpaUserBookingRepo.findAllowedDurationByRoleId(roleId)
                .orElse(5); // Default to 5 if not found

        // We subtract 1 to account for binary logic - counting from 0 (today + range - 1)
        return startDate.plusDays(range - 1);
    }
}
