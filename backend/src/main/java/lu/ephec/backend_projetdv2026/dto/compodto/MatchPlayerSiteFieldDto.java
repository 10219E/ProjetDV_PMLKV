package lu.ephec.backend_projetdv2026.dto.compodto;

import lu.ephec.backend_projetdv2026.dto.FieldDto;
import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.dto.MatchPlayerDto;
import lu.ephec.backend_projetdv2026.dto.SiteDto;

public class MatchPlayerSiteFieldDto {
    private MatchPlayerDto player;
    private MatchDto match;
    private FieldDto field;
    private SiteDto site;

    public MatchPlayerDto getPlayer() { return player; }
    public void setPlayer(MatchPlayerDto player) { this.player = player; }
    public MatchDto getMatch() { return match; }
    public void setMatch(MatchDto match) { this.match = match; }
    public FieldDto getField() { return field; }
    public void setField(FieldDto field) { this.field = field; }
    public SiteDto getSite() { return site; }
    public void setSite(SiteDto site) { this.site = site; }
}