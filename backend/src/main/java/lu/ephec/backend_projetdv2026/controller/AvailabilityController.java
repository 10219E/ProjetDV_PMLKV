package lu.ephec.backend_projetdv2026.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lu.ephec.backend_projetdv2026.dto.compodto.AvailabilityDto;
import lu.ephec.backend_projetdv2026.services.availability.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityController.class);

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

   //Get available sessions for a field on a specific date, it checks which sessions are already booked
    @GetMapping(value = "/{siteId}/{fieldId}", produces = "application/json")
    public ResponseEntity<AvailabilityDto> getAvailableSessions(
            @Parameter(description = "ID of the site", required = true)
            @PathVariable Integer siteId,

            @Parameter(description = "ID of the field", required = true)
            @PathVariable Integer fieldId,

            @Parameter(description = "Date to check availability (format: yyyy-MM-dd)", required = true,
                    schema = @Schema(type = "string", format = "date", example = "2023-12-25"))
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        logger.info("Starting getAvailableSessions for siteId: {}, fieldId: {}, date: {}", siteId, fieldId, date);

        try {
            // Get available sessions from the service
            logger.debug("Calling availabilityService.getAvailableSessions");
            List<AvailabilityService.Session> availableSessions = availabilityService.getAvailableSessions(siteId, fieldId, date);
            logger.debug("Received {} available sessions from service", availableSessions.size());

            // Convert service sessions to DTO sessions
            logger.debug("Converting sessions to DTO format");
            List<AvailabilityDto.SessionDto> sessionDtos = availableSessions.stream()
                    .map(session -> new AvailabilityDto.SessionDto(
                            session.getStartTime(),
                            session.getEndTime()
                            ))
                    .collect(Collectors.toList());

            // Create and return the response DTO
            logger.debug("Creating response DTO");
            AvailabilityDto responseDto = new AvailabilityDto(
                    siteId,
                    fieldId,
                    date,
                    sessionDtos
            );

            logger.info("Successfully processed getAvailableSessions request");
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            logger.error("Error in getAvailableSessions: {}", e.getMessage(), e);
            throw e; // Re-throw to let Spring handle the error response
        }
    }

  //Get available dates based on role (to avoid over loading the system with too many requests for availability sessions when users want to book far in advance)
    @GetMapping(value = "/dates/{siteId}/{fieldId}", produces = "application/json")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @Parameter(description = "ID of the site", required = true)
            @PathVariable Integer siteId,

            @Parameter(description = "ID of the field", required = true)
            @PathVariable Integer fieldId,

            @Parameter(description = "Start date of the range (format: yyyy-MM-dd)", required = true,
                    schema = @Schema(type = "string", format = "date", example = "2023-12-25"))
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "User roleId to calculate reservation", required = true)
            @RequestParam Integer roledId
    ){

        try {
             if (roledId != null) {
                logger.info("Role ID has been provided.");
        }} catch (Exception e) {
            logger.error("Error validating role ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        startDate = startDate.plusDays(2); //we need to allow 2 days buffer to not mess with scheduler logic

        // 0 = invite (Membre externe invité) -> max 5 days
        // 1 = subscribed (Membre with subscription to at least one site) -> max 14 days
        // 2 = all_site (Membre VIP multi-sites) -> max 21 days
        // 7 = site_admin (site administrator) -> admin: 3 months (~90 days)
        // 9 = as_admin (super administrator) -> admin: 3 months (~90 days)

        int range = 0;
        if (roledId == 0) {range = 5;}
        if (roledId == 1) {range = 90;} //as populating the db setting to 90 instead of 14
        if (roledId == 2) {range = 90;} //as populating the db setting to 90 instead of 21
        if (roledId == 7 || roledId == 9) {range = 90;}

        range = range -1; //to account for binary logic - counting from 0
        LocalDate endDate = startDate.plusDays(range);

        try {
            // Validate date range
            if (startDate.isAfter(endDate)) {
                logger.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
                return ResponseEntity.badRequest().build();
            }

        logger.info("Starting getAvailableDates for siteId: {}, fieldId: {}, startDate: {}, endDate: {}",
                siteId, fieldId, startDate, endDate);


            // Calculate the number of days in the range
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include endDate
            logger.debug("Checking {} days in range", days);

            // Check each date in the range for availability
            logger.debug("Checking each date in range for availability");
            List<LocalDate> availableDates = startDate.datesUntil(endDate.plusDays(1))
                    .filter(date -> {
                        try {
                            List<AvailabilityService.Session> sessions = availabilityService.getAvailableSessions(siteId, fieldId, date);
                            boolean isAvailable = !sessions.isEmpty();
                            logger.debug("Date {} is {} for siteId: {}, fieldId: {}",
                                    date, isAvailable ? "available" : "not available", siteId, fieldId);
                            return isAvailable;
                        } catch (Exception e) {
                            logger.error("Error checking availability for date {}: {}", date, e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            logger.info("Found {} available dates in range", availableDates.size());
            return ResponseEntity.ok(availableDates);
        } catch (Exception e) {
            logger.error("Error in getAvailableDates: {}", e.getMessage(), e);
            throw e; // Re-throw to let Spring handle the error response
        }
    }
}