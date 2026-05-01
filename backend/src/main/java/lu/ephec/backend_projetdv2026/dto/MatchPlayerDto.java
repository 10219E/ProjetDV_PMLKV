package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.MatchPlayers;

public class MatchPlayerDto {
    private Integer matchPlayerId;
    private MatchDto match;
    private String userMatricule;
    private String status;
    private String playerRole;

    public MatchPlayerDto() {}

    public MatchPlayerDto(Integer matchPlayerId, MatchDto match, String userMatricule, String status, String playerRole) {
        this.matchPlayerId = matchPlayerId;
        this.match = match;
        this.userMatricule = userMatricule;
        this.status = status;
        this.playerRole = playerRole;
    }

    public static MatchPlayerDto fromEntity(MatchPlayers matchPlayers) {
        return new MatchPlayerDto(
            matchPlayers.getMatchPlayerId(),
            MatchDto.from(matchPlayers.getMatch()),
            matchPlayers.getUser().getMatricule(),
            matchPlayers.getStatus(),
            matchPlayers.getPlayerRole()
        );
    }

    public Integer getMatchPlayerId() { return matchPlayerId; }
    public MatchDto getMatch() { return match; }
    public String getUserMatricule() { return userMatricule; }
    public String getStatus() { return status; }
    public String getPlayerRole() { return playerRole; }
}