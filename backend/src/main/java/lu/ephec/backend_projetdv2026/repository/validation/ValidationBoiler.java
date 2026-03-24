package lu.ephec.backend_projetdv2026.repository.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ValidationBoiler {

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
}