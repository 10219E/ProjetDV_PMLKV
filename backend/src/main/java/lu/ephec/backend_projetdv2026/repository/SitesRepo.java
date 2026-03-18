package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPASitesRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service //BEAN
public class SitesRepo {

    private final JPASitesRepo jpaSitesRepo;

    // InjDep Interface Sites
    public SitesRepo(JPASitesRepo jpaSitesRepo) {
        this.jpaSitesRepo = jpaSitesRepo;
    }

    //SET Site
    public Site newSite(Site site) { return jpaSitesRepo.save(site); }

    //GET Site by ID
    public Optional<Site> fetchById(Integer siteId) { return jpaSitesRepo.findById(siteId);}

    //GET Site by Name
    public Optional<Site> fetchByName(String siteName) { return jpaSitesRepo.findByName(siteName); }

    //GET Site by Address
    public Optional<Site> fetchByAddress(String siteAddress) { return jpaSitesRepo.findByAddress(siteAddress);}

    //GET ALL Sites
    public List<Site> fetchAll() { return jpaSitesRepo.findAll(); }

    //DELETE Site
    public void deleteSite(Integer siteId) { jpaSitesRepo.deleteById(siteId); }

    //UPDATE Site
    public Optional<Site> updSite(Integer siteId, Site updateData) {
        return jpaSitesRepo.findById(siteId).map(site -> {
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

            return jpaSitesRepo.save(site);
        });
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
}
