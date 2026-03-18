package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.SiteSessions;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPASitesSessionsRepo;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SitesSessionsRepo {

    private final JPASitesSessionsRepo jpaSitesSessionsRepo;

    // InjDep Interface SitesSessions
    public SitesSessionsRepo(JPASitesSessionsRepo jpaSitesSessionsRepo) {
        this.jpaSitesSessionsRepo = jpaSitesSessionsRepo;
    }

    //SET Session
    public SiteSessions newSite(SiteSessions session) { return jpaSitesSessionsRepo.save(session); }

    //GET Session by ID
    public Optional<SiteSessions> fetchById(Integer siteId) { return jpaSitesSessionsRepo.findById(siteId);}

    //ALL Sessions
    public List<SiteSessions> allSessions() { return jpaSitesSessionsRepo.findAll();}

    //DELETE Session
    public void deleteSession(Integer sessionId) { jpaSitesSessionsRepo.deleteById(sessionId); }

    //GET ALL Sessions by start time
    public List<SiteSessions> getSessionByStartTime(LocalDateTime startTime) {
        return jpaSitesSessionsRepo.findByStartTime(startTime);
    }

    //GET ALL Sessions by end time
    public List<SiteSessions> getSessionByEndTime(LocalDateTime endTime) {
        return jpaSitesSessionsRepo.findByEndTime(endTime);
    }

    /*
    public SitesSessionsRepo updSession(Integer sessionId, SitesSessionsRepo dataObj) {
        Optional<SiteSessions> optionalSession = jpaSitesSessionsRepo.findById(sessionId);
        if (optionalSession.isPresent()) {
            SitesSessionsRepo session = optionalSession.get();
            // Update fields from dataObj
            session.setStartTime(dataObj.getStartTime());
            session.setEndTime(dataObj.getEndTime());
            session.setSiteId(dataObj.getSiteId());
            session.setMatchId(dataObj.getMatchId());
            // Update other fields as needed

            return jpaSitesSessionsRepo.save(session);
        }
        return null;
    }

    public List<SiteSessions> getSessionBySite(Integer siteId) {
        return jpaSitesSessionsRepo.findBySiteId(siteId);
    }

    public List<SiteSessions> getSessionByMatch(Integer matchId) {
        return jpaSitesSessionsRepo.findByMatchId(matchId);
    }*/
}
