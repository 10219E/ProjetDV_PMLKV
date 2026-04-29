package lu.ephec.backend_projetdv2026.services.validation;

import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repo.JPASiteClosureDaysRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

public class ValidationBoiler {

    // Check if passed object is not null
    public static void verifyNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required");
        }
    }

    // Check if user is active
    public static void verifyUserActive(Boolean isActive, String userId) {
        if (isActive == null || !isActive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + userId + " is not active. Cannot proceed with this operation.");
        }
    }

    // Check if user has outstanding debt or pending match payments
    public static void verifyNoOutstandingFinancialObligations(boolean hasDebt, List<MatchPayments> pendingPayments, String userId) {
        if (hasDebt) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User " + userId + " has outstanding debt. Cannot proceed with this operation.");
        }


        //WE'RE BLOCKING THIS CHECK AS IT DOESN'T MAKE SENSE - TO RESTRICTIVE - YOU CAN BE INVITED TO MULTIPLE MATCHES AT ONCE
        /*long pendingCount = pendingPayments.stream()
                .filter(payment -> "pending".equals(payment.getStatus()))
                .count();

        if (pendingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User " + userId + " has " + pendingCount + " pending match payments. Cannot proceed with this operation.");
        }*/
    }

    // Check if collection is not empty (for List, Set, etc.)
    public static void verifyListNotEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required");
        }
    }

    //Check if passed object (id String or Integer) exists
    public static void verifyExists(boolean exists, String resourceType, Object id) {
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    resourceType + " not found with id: " + id);
        }
    }

    //Check if passed object (id String or Integer) does NOT exist (prevent duplicates)
    public static void verifyNotExists(boolean exists, String resourceType, Object id) {
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    resourceType + " already exists with id: " + id);
        }
    }

    //Check if passed email exists
    public static void verifyEmailNotExists(boolean emailExists, String email) {
        if (emailExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already exists: " + email);
        }
    }

    //Check if toDate or toTime is superior
    public static <T extends Comparable<T>> void verifyDatesValid(T fromDate, T toDate, String fieldName) {
        if (fromDate != null && toDate != null && fromDate.compareTo(toDate) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + ": start date must be <= end date");
        }
    }

    //Check if string is not empty or null
    public static void verifyNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required");
        }
    }

    //Check if level is valid for User
    public static void verifyValidLevel(String level) {
        ValidationBoiler.verifyNotEmpty(level, "Level");

        String[] validLevels = {"débutant", "averti", "confirmé"};
        boolean isValid = false;
        for (String validLevel : validLevels) {
            if (validLevel.equalsIgnoreCase(level.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid level: " + level + ". Valid levels are: débutant, averti, confirmé");
        }
    }

    // Check if user has active penalties - block migration if yes
    public static void verifyNoActivePenalties(boolean hasActivePenalties, String userId) {
        if (hasActivePenalties) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User " + userId + " has active penalties. Cannot migrate user with active penalties.");
        }
    }

    //Check if penalty reason is valid
    public static void verifyValidPenaltyReason(String reason) {
        ValidationBoiler.verifyNotEmpty(reason, "Penalty reason");

        String[] validReasons = {"unpaid_balance", "no_show", "insufficient_players"};
        boolean isValid = false;
        for (String validReason : validReasons) {
            if (validReason.equalsIgnoreCase(reason.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid penalty reason: " + reason + ". Valid reasons are: unpaid_balance, no_show, insufficient_players");
        }
    }

    // Check if user is admin - admins cannot have penalties, matches, etc
    public static void verifyNotAdminUser(Short roleId, String userId) {
        EnumUserRolesType role = EnumUserRolesType.fromId(roleId);
        if (role != null && role.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin user " + userId + " (roles " + role.getDisplayName() + ") cannot be used here.");
        }
    }

    //Validate role
    public static void verifyValidRoleId(Short roleId) {
        if (roleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role ID is required");
        }

        EnumUserRolesType role = EnumUserRolesType.fromId(roleId);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role ID: " + roleId);
        }
    }

    //Verify valid migration -- prevent admin --> normal user and normal --> admin
    public static void verifyNotMigrationBetweenAdminNormal(Short oldRoleId, Short newRoleId) {
        EnumUserRolesType oldRole = EnumUserRolesType.fromId(oldRoleId);
        EnumUserRolesType newRole = EnumUserRolesType.fromId(newRoleId);

        if (oldRole != null && newRole != null) {
            if (oldRole.isAdmin() != newRole.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Migration between admin and non-admin roles not allowed. Attempted to migrate: " + oldRole.getDisplayName() + " to " + newRole.getDisplayName() + ".");
            }
        }
    }

    // Min pre-session: 15 min
    // Max pre-session: 30 min
    // Min post-session: 15 min
    // Max post-session: 30 min (avoids business hours being too long for no reason)
    // Check if site has enough hours to fit at least one session with valid pre/post durations
    public static void verifyEnoughSiteHours(LocalTime openingTime, LocalTime closingTime) {
        verifyNotNull(openingTime, "Opening time");
        verifyNotNull(closingTime, "Closing time");
        if (!closingTime.isAfter(openingTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Closing time must be after opening time");
        }

        final int MIN_PRE = 15, MAX_PRE = 30;
        final int MIN_POST = 15, MAX_POST = 30;
        final int SESSION = 90;
        final int BREAK = 15;

        long total = ChronoUnit.MINUTES.between(openingTime, closingTime); //Count all time

        boolean feasible = false;
        for (int pre = MIN_PRE; pre <= MAX_PRE; pre++) {
            long offset = pre; //Init - Minutes elapsed since opening
            int count = 0;  //Init - Number of sessions
            long lastEnd = -1; //Init - End of last session (total minutes) to calculate leftover after last session

            //Populate sessions
            while (offset + SESSION + MIN_POST <= total) { //Loop through sessions
                lastEnd = offset + SESSION;       //Add session time to offset to get session end time, store in lastEnd to calculate leftover after last session
                offset += SESSION + BREAK;        //Adding Sessions + break time to time elapsed
                count++;
            }

            if (count == 0) {
                continue; //No session could fit with this pre-session duration, try next pre-session duration
            }

            long leftover = total - lastEnd; //Left over after last session of the day
            if (leftover >= MIN_POST && leftover <= MAX_POST) {
                feasible = true; //Feasible schedule found with this pre-session duration, no need to check further
                break;
            }
        }

        if (!feasible) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid site hours: need pre/post between 15 and 30 minutes and at least one 90-minute session fitting the schedule");
        }
    }

    // Validate match type (private or public)
    public static void verifyValidMatchType(String type) {
        verifyNotEmpty(type, "Match type");
        if (!type.equals("private") && !type.equals("public")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Match type must be 'private' or 'public'. Received: " + type);
        }
    }

    // Private matches must have an organiser, public matches must not
    public static void verifyOrganizerConsistency(String matchType, User organiser) {
        verifyNotEmpty(matchType, "Match type");

        if (matchType.equals("private")) {
            if (organiser == null || organiser.getMatricule() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private match must have an organiser");
            }
        } else if (matchType.equals("public")) {
            if (organiser != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Public match must not have an organiser (must be NULL)");
            }
        }
    }

    // Validate status consistency with match type
    // Public matches: pubStatus must be valid, privStatus must be null
    // Private matches: privStatus must be valid, pubStatus must be null
    public static void verifyMatchStatusConsistency(String matchType, String pubStatus, String privStatus) {
        verifyNotEmpty(matchType, "Match type");

        if (matchType.equals("public")) {
            if (pubStatus == null || pubStatus.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Public match must have a public status");
            }
            if (privStatus != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Public match must not have a private status");
            }
            if (!pubStatus.equals("open") && !pubStatus.equals("closed") &&
                    !pubStatus.equals("completed") && !pubStatus.equals("cancelled")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Public status must be one of: 'open', 'closed', 'completed', 'cancelled'. Received: " + pubStatus);
            }
        } else if (matchType.equals("private")) {
            if (privStatus == null || privStatus.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private match must have a private status");
            }
            if (pubStatus != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private match must not have a public status");
            }
            if (!privStatus.equals("awaiting") && !privStatus.equals("confirmed") &&
                    !privStatus.equals("completed") && !privStatus.equals("cancelled")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private status must be one of: 'awaiting', 'confirmed', 'completed', 'cancelled'. Received: " + privStatus);
            }
        }
    }

    // Validate match date is not on a site closure day
    public static void verifyMatchDateNotOnClosureDay(LocalDate matchDate, Integer siteId,
                                                      JPASiteClosureDaysRepo jpaSiteClosureDaysRepo) {
        verifyNotNull(matchDate, "Match date");
        verifyNotNull(siteId, "Site ID");

        if (jpaSiteClosureDaysRepo.existsBySiteIdAndClosureDate(siteId, matchDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot create match on " + matchDate + ": site is closed on this date");
        }
    }

    // Validate match date is not during field maintenance period
    public static void verifyFieldNotUnderMaintenance(LocalDate matchDate, Field field) {
        verifyNotNull(matchDate, "Match date");
        verifyNotNull(field, "Field");

        LocalDate maintenanceFromDate = field.getMaintenanceFromDate();
        LocalDate maintenanceToDate = field.getMaintenanceToDate();

        // If both maintenance dates are set, check if match date falls within the range
        if (maintenanceFromDate != null && maintenanceToDate != null) {
            if (!matchDate.isBefore(maintenanceFromDate) && !matchDate.isAfter(maintenanceToDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot create match on " + matchDate + ": field is under maintenance from " + maintenanceFromDate + " to " + maintenanceToDate);
            }
        }
    }



}