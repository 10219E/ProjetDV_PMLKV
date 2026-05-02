package lu.ephec.backend_projetdv2026.dto.compodto;

import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.dto.MatchPlayerDto;

public class MatchAndPlayerDto {
    private MatchPlayerDto player;
    private MatchDto match;

    public MatchPlayerDto getPlayer() {return player;}

    public void setPlayer(MatchPlayerDto player) {this.player = player;}

    public MatchDto getMatch() {return match;}

    public void setMatch(MatchDto match) {this.match = match;}
}