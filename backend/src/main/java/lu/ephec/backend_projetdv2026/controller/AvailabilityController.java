package lu.ephec.backend_projetdv2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    /**
     * Get available sessions for a field on a specific date
     *
     * @param siteId The ID of the site
     * @param fieldId The ID of the field
     * @param date The date to check availability (format: yyyy-MM-dd)
     * @return ResponseEntity containing the availability information
     */
    @Operation(summary = "Get available sessions for a field on a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved available sessions"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            @ApiResponse(responseCode = "404", description = "Site or field not found")
    })
    @GetMapping("/{siteId}/{fieldId}")
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

    /**
     * Get available dates for a field within a date range
     *
     * @param siteId The ID of the site
     * @param fieldId The ID of the field
     * @param startDate The start date of the range (format: yyyy-MM-dd)
     * @param endDate The end date of the range (format: yyyy-MM-dd)
     * @return ResponseEntity containing a list of available dates
     */
    @Operation(summary = "Get available dates for a field within a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved available dates"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            @ApiResponse(responseCode = "404", description = "Site or field not found")
    })
    @GetMapping("/dates/{siteId}/{fieldId}")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @Parameter(description = "ID of the site", required = true)
            @PathVariable Integer siteId,

            @Parameter(description = "ID of the field", required = true)
            @PathVariable Integer fieldId,

            @Parameter(description = "Start date of the range (format: yyyy-MM-dd)", required = true,
                    schema = @Schema(type = "string", format = "date", example = "2023-12-25"))
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date of the range (format: yyyy-MM-dd)", required = true,
                    schema = @Schema(type = "string", format = "date", example = "2023-12-31"))
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        logger.info("Starting getAvailableDates for siteId: {}, fieldId: {}, startDate: {}, endDate: {}",
                siteId, fieldId, startDate, endDate);

        try {
            // Validate date range
            if (startDate.isAfter(endDate)) {
                logger.warn("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
                return ResponseEntity.badRequest().build();
            }

            // Calculate the number of days in the range
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
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