package lu.ephec.backend_projetdv2026.services.validation;

import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MatriculeHandler {

    //Generate matricule based on user role (Admin identifiable by 3 digits, users by 4)
    public static String generateMatricule(Short roleId, JPAUserRepo jpaUserRepo) {

        String prefix;
        int digitCount;

        switch(roleId) {
            case 0:  //invite
                prefix = "L";
                digitCount = 4;
                break;
            case 1:  //subscribed --one site
                prefix = "S";
                digitCount = 4;
                break;
            case 2:  //all_site --VIP
                prefix = "G";
                digitCount = 4;
                break;
            case 7:  //site_admin
                prefix = "M";
                digitCount = 3;
                break;
            case 9:  //as_admin
                prefix = "A";
                digitCount = 3;
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid role ID: " + roleId);
        }

        // Fetch all matricules starting with the prefix
        List<User> usersWithPrefix = jpaUserRepo.findAllWithMatriculePrefix(prefix);

        // Determine starting number
        int startingNumber = (int) Math.pow(10, digitCount - 1); // 100 for 3 digits, 1000 for 4 digits

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
        return prefix + String.format("%0" + digitCount + "d", maxNumber + 1);
    }

}
