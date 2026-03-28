package lu.ephec.backend_projetdv2026.services.validation;

import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

public class ValidationBoiler {

    // Check if passed object is not null
    public static void verifyNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required");
        }
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

    //Check if toDate is superior
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
                    "Admin users (roles " + role.getDisplayName() + ") cannot have penalties. Penalties are only for regular users.");
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

    // Check if site has enough hours to fit at least one session
    // Min: 90 min session + 15 min post-session = 105 min
    // Max pre-session: 30 min, always 15 min post-session required
    public static void verifyEnoughSiteHours(java.time.LocalTime openingTime, java.time.LocalTime closingTime) {
        if (openingTime == null || closingTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Opening and closing times are required");
        }

        if (closingTime.isBefore(openingTime) || closingTime.equals(openingTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Closing time must be after opening time");
        }

        long minutesAvailable = java.time.temporal.ChronoUnit.MINUTES
                .between(openingTime, closingTime);

        // Min needed: 15 min (min pre-session) + 90 min (session) + 15 min (post-session) = 120 min
        long minutesNeeded = 120;

        if (minutesAvailable < minutesNeeded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Site hours too short. Need at least %d minutes (15 pre-session + 90 session + 15 post-session), got %d minutes",
                            minutesNeeded, minutesAvailable));
        }
    }
}