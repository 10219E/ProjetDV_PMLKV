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
@Table(name = "\"Users_Accounts\"")
public class UserAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tr")
    private Integer tr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", columnDefinition = "DECIMAL(6,2)", nullable = false)
    private Double balance = 0.00;

    @Column(name = "last_upd", nullable = true)
    private LocalDateTime lastUpdate;

    @Column(name = "status", length = 10, nullable = false)
    private String status = "clear"; // 'clear' or 'debt'
}
