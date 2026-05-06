package lu.ephec.backend_projetdv2026.dto.compodto;

public class UserPenaltyUpdateDto {
    private Integer penaltyId;
    private Double amount;

    public UserPenaltyUpdateDto() {
    }

    public UserPenaltyUpdateDto(Integer penaltyId, Double amount) {
        this.penaltyId = penaltyId;
        this.amount = amount;
    }

    public Integer getPenaltyId() {
        return penaltyId;
    }

    public void setPenaltyId(Integer penaltyId) {
        this.penaltyId = penaltyId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

