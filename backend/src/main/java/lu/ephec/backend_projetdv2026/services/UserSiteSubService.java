package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UsersSites;
import lu.ephec.backend_projetdv2026.repo.JPASiteRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserSiteRepo;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UserSiteSubService {

    private final JPAUserSiteRepo jpaUserSiteRepo;
    private final JPAUserRepo jpaUserRepo;
    private final JPASiteRepo jpaSiteRepo;

    public UserSiteSubService(JPAUserSiteRepo jpaUserSiteRepo, JPAUserRepo jpaUserRepo, JPASiteRepo jpaSiteRepo) {
        this.jpaUserSiteRepo = jpaUserSiteRepo;
        this.jpaUserRepo = jpaUserRepo;
        this.jpaSiteRepo = jpaSiteRepo;
    }

    // SET LINK USER TO SITE -- Admins are also linked the same way EXCEPT SA
    @Transactional
    public UsersSites newUserSite(String userId, Integer siteId, Boolean isPrimary, Boolean isVip) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        ValidationBoiler.verifyUserActive(jpaUserRepo.findById(userId).orElseThrow().getIsActive(), userId);
        //IMPLEMENT SITE IS ACTIVE

        if (jpaUserRepo.findById(userId).orElseThrow().getRole().getId() == 9) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Super Admin users cannot be linked to sites: " + userId);
        }

        if (jpaUserSiteRepo.existsByUser_MatriculeAndSite_SiteId(userId, siteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User-site link already exists for user " + userId + " and site " + siteId);
        }

        User user = jpaUserRepo.findById(userId).orElseThrow();
        Site site = jpaSiteRepo.findById(siteId).orElseThrow();

        UsersSites link = new UsersSites();
        if (isPrimary != null) {
            link.setIsPrimary(isPrimary);
        } else {
            link.setIsPrimary(true);
        }

        if (isVip != null) {
            link.setIsVip(isVip);
        } else {
            link.setIsVip(false);
        }
        link.setUser(user);
        link.setSite(site);

        return jpaUserSiteRepo.save(link);
    }

    // UPDATE -- CAN ONLY UPDATE VIP OR PRIMARY STATUS -- NEW HAS TO BE LINKED TO ANOTHER SITE
    @Transactional
    public Optional<UsersSites> updateUserSite(String userId, Integer siteId, Boolean isPrimary, Boolean isVip) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        if (isPrimary == null && isVip == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one field must be provided: isPrimary or isVip");
        }

        return jpaUserSiteRepo.findByUser_MatriculeAndSite_SiteId(userId, siteId).map(link -> {
            if (isPrimary != null) {
                link.setIsPrimary(isPrimary);
            }
            if (isVip != null) {
                link.setIsVip(isVip);
            }
            return jpaUserSiteRepo.save(link);
        });
    }

    // DELETE LINK
    @Transactional
    public void deleteUserSite(String userId, Integer siteId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(siteId, "Site ID");

        ValidationBoiler.verifyExists(
                jpaUserSiteRepo.existsByUser_MatriculeAndSite_SiteId(userId, siteId),
                "User-site link",
                userId + "/" + siteId
        );

        jpaUserSiteRepo.deleteByUserAndSite(userId, siteId);
    }

    // FETCH ALL USER LINKS BY SITE
    public List<UsersSites> fetchBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);

        List<UsersSites> links = jpaUserSiteRepo.findBySite_SiteId(siteId);
        if (links.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user-site links found for site: " + siteId);
        }
        return links;
    }

    //FETCH ALL LINKS FOR USER
    public List<UsersSites> fetchByUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "Site ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        List<UsersSites> links = jpaUserSiteRepo.findByUser_Matricule(userId);
        if (links.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user-site links found for user: " + userId);
        }
        return links;
    }

    // FETCH all links by VIP flag
    public List<UsersSites> fetchByVip(Boolean isVip) {
        ValidationBoiler.verifyNotNull(isVip, "VIP flag");

        List<UsersSites> links = jpaUserSiteRepo.findByIsVip(isVip);
        if (links.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user-site links found for isVip=" + isVip);
        }
        return links;
    }
}
