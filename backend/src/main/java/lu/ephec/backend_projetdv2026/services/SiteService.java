package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.SiteClosureDays;
import lu.ephec.backend_projetdv2026.repo.JPASiteClosureDaysRepo;
import lu.ephec.backend_projetdv2026.repo.JPASiteRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service //BEAN
public class SiteService {

    private final JPASiteRepo jpaSiteRepo;
    private final JPASiteClosureDaysRepo jpaClosureDaysRepo;

    // InjDep Interface Sites
    public SiteService(JPASiteRepo jpaSiteRepo, JPASiteClosureDaysRepo jpaClosureDaysRepo) {

        this.jpaSiteRepo = jpaSiteRepo;
        this.jpaClosureDaysRepo = jpaClosureDaysRepo;
    }

    ////SITES OPERATIONS////

    //CHECK EXISTS
    public boolean siteExists(Integer siteId) {
        return jpaSiteRepo.existsById(siteId);
    }

    //SET Site
    public Site newSite(Site site) {
        ValidationBoiler.verifyNotEmpty(site.getName(), "Site name");
        ValidationBoiler.verifyNotEmpty(site.getAddress(), "Site address");
        ValidationBoiler.verifyNotNull(site.getOpeningTime(), "Site opening time");
        ValidationBoiler.verifyNotNull(site.getClosingTime(), "Site closing time");
        return jpaSiteRepo.save(site);
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

    //DELETE Site
    public void deleteSite(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        List<SiteClosureDays> closures = jpaClosureDaysRepo.findBySiteId(siteId);
        jpaClosureDaysRepo.deleteAll(closures); //CLEAN DELETE EVEN IF CASCADE
        jpaSiteRepo.deleteById(siteId);
    }

    //UPDATE Site
    public Optional<Site> updateSite(Integer siteId, Site updateData) {

        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);

        return jpaSiteRepo.findById(siteId).map(site -> {
            if (updateData.getName() != null) {
                site.setName(updateData.getName());
            }
            if (updateData.getAddress() != null) {
                site.setAddress(updateData.getAddress());
            }
            if (updateData.getOpeningTime() != null) {
                site.setOpeningTime(updateData.getOpeningTime());
            }
            if (updateData.getClosingTime() != null) {
                site.setClosingTime(updateData.getClosingTime());
            }
            if (updateData.getIsActive() != null) {
                site.setIsActive(updateData.getIsActive());
            }

            return jpaSiteRepo.save(site);
        });
    }

    ////CLOSURE DAYS////

    // SET MULTIPLE DATES TO ONE SITE
    public List<SiteClosureDays> newClosuresOneSite(Integer siteId, List<LocalDate> closureDates, String reason) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        ValidationBoiler.verifyListNotEmpty(closureDates, "Closure dates");

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
    public List<SiteClosureDays> newClosureMultiSite(List<Integer> siteIds, List<LocalDate> closureDates, String reason) {
        ValidationBoiler.verifyListNotEmpty(siteIds, "Site IDs");
        ValidationBoiler.verifyListNotEmpty(closureDates, "Closure dates");


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
    public void deleteAllClosuresForSite(Integer siteId) {
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        List<SiteClosureDays> closures = jpaClosureDaysRepo.findBySiteId(siteId);
        if (!closures.isEmpty()) {
            jpaClosureDaysRepo.deleteAll(closures);
        }
    }

    // DELETE ALL CLOSURE DAYS for a specific Date (ALL SITES)
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
    /*

    // Les méthodes ci-dessous supposent que Site possède getClosures() et getSubscriptions()
    // et que Closure / Subscription sont des classes du package models.
    public List<?> getClosure(Integer siteId) {
        return jpaSitesRepo.findById(siteId)
                .map(site -> site.getClosures() != null ? site.getClosures() : Collections.emptyList())
                .orElse(Collections.emptyList());
    }

    public void setClosure(Integer siteId, Object closure) {
        jpaSitesRepo.findById(siteId).ifPresent(site -> {
            if (site.getClosures() == null) {
                site.setClosures(new java.util.ArrayList<>());
            }
            site.getClosures().add(closure);
            jpaSitesRepo.save(site);
        });
    }

    public void deleteClosure(Integer closureId) {
        // Si vous avez un ClosureRepository, utilisez-le ici pour supprimer par id.
    }

    public List<?> getSubBySite(Integer siteId) {
        return jpaSitesRepo.findById(siteId)
                .map(site -> site.getSubscriptions() != null ? site.getSubscriptions() : Collections.emptyList())
                .orElse(Collections.emptyList());
    }

    */

