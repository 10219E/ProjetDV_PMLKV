package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.SiteSessions;
import lu.ephec.backend_projetdv2026.repo.JPASiteSessionsRepo;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SiteSessionsService {

    private final JPASiteSessionsRepo jpaSiteSessionsRepo;

    // InjDep Interface SitesSessions
    public SiteSessionsService(JPASiteSessionsRepo jpaSiteSessionsRepo) {
        this.jpaSiteSessionsRepo = jpaSiteSessionsRepo;
    }

    //SET Session
    public SiteSessions newSite(SiteSessions session) { return jpaSiteSessionsRepo.save(session); }

    //GET Session by ID
    public Optional<SiteSessions> fetchById(Integer siteId) { return jpaSiteSessionsRepo.findById(siteId);}

    //ALL Sessions
    public List<SiteSessions> allSessions() { return jpaSiteSessionsRepo.findAll();}

    //DELETE Session
    public void deleteSession(Integer sessionId) { jpaSiteSessionsRepo.deleteById(sessionId); }

    //GET ALL Sessions by start time
    public List<SiteSessions> getSessionByStartTime(LocalDateTime startTime) {
        return jpaSiteSessionsRepo.findByStartTime(startTime);
    }

    //GET ALL Sessions by end time
    public List<SiteSessions> getSessionByEndTime(LocalDateTime endTime) {
        return jpaSiteSessionsRepo.findByEndTime(endTime);
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
