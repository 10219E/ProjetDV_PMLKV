package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JPAMatchPlayersRepo extends JpaRepository<MatchPlayers, Integer> {

    // Find all players in a specific match
    List<MatchPlayers> findByMatch_MatchId(Integer matchId);

    // Find all matches where a user is playing
    List<MatchPlayers> findByUser_Matricule(String userId);

    // Find a specific player in a match
    Optional<MatchPlayers> findByMatch_MatchIdAndUser_Matricule(Integer matchId, String userId);

    // Find player by match and role
    Optional<MatchPlayers> findByMatch_MatchIdAndPlayerRole(Integer matchId, String playerRole);

    // Check if a user is already in a match
    boolean existsByMatch_MatchIdAndUser_Matricule(Integer matchId, String userId);

    // Check if a role is already assigned in a match
    boolean existsByMatch_MatchIdAndPlayerRole(Integer matchId, String playerRole);

    // Count players by status in a match
    long countByMatch_MatchIdAndStatus(Integer matchId, String status);

    // Get all players with specific status in a match
    List<MatchPlayers> findByMatch_MatchIdAndStatus(Integer matchId, String status);

    // Delete all players from a match (cascade)
    void deleteByMatch_MatchId(Integer matchId);
}
