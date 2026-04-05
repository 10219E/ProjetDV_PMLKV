package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"Sites_Match_Players\"")
public class MatchPlayers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tr")
    private Integer matchPlayerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", referencedColumnName = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Column(name = "status", length = 15, nullable = false)
    private String status; // approved, pending, declined

    @Column(name = "p_role", length = 2, nullable = false)
    private String playerRole; // p1, p2, p3, p4
}