package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"Match_Payments\"")
public class MatchPayments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tr")
    private Integer tr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", referencedColumnName = "match_id", nullable = true)
    private Match match;

    @Column(name = "amount", columnDefinition = "DECIMAL(6,2)", nullable = false)
    private Double amount;

    @Column(name = "payment_date", nullable = true)
    private LocalDateTime paymentDate;

    @Column(name = "status", length = 15, nullable = false)
    private String status; // 'clear', 'pending', 'cancelled', 'failed', 'refunded'

    @Column(name = "payment_method", length = 10, nullable = false)
    private String paymentMethod; // 'COUNTER', 'CARD'
}
