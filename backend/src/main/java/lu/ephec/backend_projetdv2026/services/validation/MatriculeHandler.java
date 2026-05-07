package lu.ephec.backend_projetdv2026.services.validation;

import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.services.availability.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MatriculeHandler {

    private static final Logger logger = LoggerFactory.getLogger(MatriculeHandler.class);

    //Generate matricule based on user role (Admin identifiable by 3 digits, users by 4)
    public static String generateMatricule(Short roleId, JPAUserRepo jpaUserRepo) {

        EnumUserRolesType role = EnumUserRolesType.fromId(roleId);

        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role ID: " + roleId);
        }

        String prefix = role.getPrefix();  // "L", "S", "G", "M", "A"
        int digitCount = role.isAdmin() ? 3 : 4;  // Admin = 3 digits, users = 4 digits

        logger.info("[Service - Matricule Handler] Planning matricule generation for user: " + role.getDisplayName() + " with prefix: " + prefix + " and digit count: " + digitCount);

        // Fetch all matricules starting with the prefix
        List<User> usersWithPrefix = jpaUserRepo.findAllWithMatriculePrefix(prefix);

        // Determine starting number
        int startingNumber = (int) Math.pow(10, digitCount - 1); // 100 for 3 digits, 1000 for 4 digits

        logger.info("[Service - Matricule Handler] Fetching new user matricule starting number: " + startingNumber);

        // If no users with this prefix, start with minimum
        if (usersWithPrefix.isEmpty()) {
            return prefix + String.format("%0" + digitCount + "d", startingNumber); //prefix+JAVA "REGEX" to format number with leading zeros
        }

        // Find the highest number
        int maxNumber = usersWithPrefix.stream()
                .map(user -> {
                    String matricule = user.getMatricule();
                    try {
                        return Integer.parseInt(matricule.substring(1)); // Remove prefix and parse number
                    } catch (NumberFormatException e) {
                        return startingNumber - 1;
                    }
                })
                .max(Integer::compareTo)
                .orElse(startingNumber - 1);

        // Increment and return with proper formatting
        logger.info("[Service - Matricule Handler] Matricule for new user defined: " + prefix + String.format("%0" + digitCount + "d", maxNumber + 1));
        return prefix + String.format("%0" + digitCount + "d", maxNumber + 1);
    }

}
