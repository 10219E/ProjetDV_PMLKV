package lu.ephec.backend_projetdv2026.dto.compodto;

import lu.ephec.backend_projetdv2026.dto.*;

import java.util.List;

public class InvitesDto {
    private MatchPaymentDto payment;
    private MatchDto match;
    private FieldDto field;
    private SiteDto site;
    private UserProfileDto organiser;
    private List<UserProfileDto> participants;
    private Integer occupancy;
    private Integer remainingSlots;

    // Constructors
    public InvitesDto() {
    }

    public InvitesDto(MatchPaymentDto payment, MatchDto match, FieldDto field,
                                SiteDto site, UserProfileDto organiser,
                                List<UserProfileDto> participants, Integer occupancy,
                                Integer remainingSlots) {
        this.payment = payment;
        this.match = match;
        this.field = field;
        this.site = site;
        this.organiser = organiser;
        this.participants = participants;
        this.occupancy = occupancy;
        this.remainingSlots = remainingSlots;
    }

    // Getters and setters
    public MatchPaymentDto getPayment() { return payment; }
    public void setPayment(MatchPaymentDto payment) { this.payment = payment; }
    public MatchDto getMatch() { return match; }
    public void setMatch(MatchDto match) { this.match = match; }
    public FieldDto getField() { return field; }
    public void setField(FieldDto field) { this.field = field; }
    public SiteDto getSite() { return site; }
    public void setSite(SiteDto site) { this.site = site; }
    public UserProfileDto getOrganiser() { return organiser; }
    public void setOrganiser(UserProfileDto organiser) { this.organiser = organiser; }
    public List<UserProfileDto> getParticipants() { return participants; }
    public void setParticipants(List<UserProfileDto> participants) { this.participants = participants; }
    public Integer getOccupancy() { return occupancy; }
    public void setOccupancy(Integer occupancy) { this.occupancy = occupancy; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
}