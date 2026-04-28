package lu.ephec.backend_projetdv2026.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.User;

import java.time.LocalDateTime;

/**
 * DTO used by the payments controller. Keeps a simple, flat shape for frontend clients
 * while providing helpers to convert to/from the JPA entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchPaymentDto {
    private Integer tr;
    private String userMatricule; // maps to MatchPayments.user.matricule
    private Integer matchId;      // maps to MatchPayments.match.matchId
    private Double amount;
    private LocalDateTime paymentDate;
    private String status;
    private String paymentMethod;

    /**
     * Build a shallow MatchPayments entity from this DTO. This method creates User and Match
     * placeholders with only identifiers set. The service layer should validate and replace
     * them by fully-loaded entities when required.
     */
    public MatchPayments toEntity() {
        MatchPayments p = new MatchPayments();
        p.setTr(this.tr);

        if (this.userMatricule != null) {
            User u = new User();
            // set only identifier; service will validate existence
            try {
                u.setMatricule(this.userMatricule);
            } catch (Exception ignored) {
                // in case User uses different field name or no setter, leave as null and service will fail
            }
            p.setUser(u);
        }

        if (this.matchId != null) {
            Match m = new Match();
            try {
                m.setMatchId(this.matchId);
            } catch (Exception ignored) {
            }
            p.setMatch(m);
        }

        p.setAmount(this.amount);
        p.setPaymentDate(this.paymentDate);
        p.setStatus(this.status);
        p.setPaymentMethod(this.paymentMethod);
        return p;
    }

    /**
     * Create a DTO from an entity. Null-safe.
     */
    public static MatchPaymentDto fromEntity(MatchPayments p) {
        if (p == null) return null;
        MatchPaymentDto dto = new MatchPaymentDto();
        dto.setTr(p.getTr());
        if (p.getUser() != null) dto.setUserMatricule(p.getUser().getMatricule());
        if (p.getMatch() != null) dto.setMatchId(p.getMatch().getMatchId());
        dto.setAmount(p.getAmount());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setStatus(p.getStatus());
        dto.setPaymentMethod(p.getPaymentMethod());
        return dto;
    }
}

