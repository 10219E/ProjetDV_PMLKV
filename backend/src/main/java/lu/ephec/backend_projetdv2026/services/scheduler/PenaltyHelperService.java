package lu.ephec.backend_projetdv2026.services.scheduler;

import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repo.JPAUserPenaltiesRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PenaltyHelperService {

    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;

    public PenaltyHelperService(JPAUserPenaltiesRepo jpaUserPenaltiesRepo) {
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
    }

    /**
     * Create penalty in a new transaction to avoid marking the caller's transaction rollback-only
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserPenalties createPenaltyNewTransaction(UserPenalties penalty) {
        return jpaUserPenaltiesRepo.save(penalty);
    }
}

