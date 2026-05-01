package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.models.User;

public class MatchPlayerDto {
    private Integer matchPlayerId;
    private Match match;
    private User user;
    private String status;
    private String playerRole;

    public MatchPlayerDto() {}

    public MatchPlayerDto(Integer matchPlayerId, Match match, User user, String status, String playerRole) {
        this.matchPlayerId = matchPlayerId;
        this.match = match;
        this.user = user;
        this.status = status;
        this.playerRole = playerRole;
    }

    // Conversion methods
    public MatchPlayers toEntity() {
        MatchPlayers matchPlayers = new MatchPlayers();
        matchPlayers.setMatchPlayerId(this.matchPlayerId);
        matchPlayers.setMatch(this.match);
        matchPlayers.setUser(this.user);
        matchPlayers.setStatus(this.status);
        matchPlayers.setPlayerRole(this.playerRole);
        return matchPlayers;
    }

    public static MatchPlayerDto fromEntity(MatchPlayers matchPlayers) {
        return new MatchPlayerDto(
            matchPlayers.getMatchPlayerId(),
            matchPlayers.getMatch(),
            matchPlayers.getUser(),
            matchPlayers.getStatus(),
            matchPlayers.getPlayerRole()
        );
    }

    public Integer getMatchPlayerId() { return matchPlayerId; }
    public Match getMatch() { return match; }
    public User getUser() { return user; }
    public String getStatus() { return status; }
    public String getPlayerRole() { return playerRole; }
}