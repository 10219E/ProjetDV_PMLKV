package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "\"Users_Penalties\"")
public class UserPenalties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", referencedColumnName = "match_id")
    private Match match;*/

    @Column (name="match_id", nullable = true) //TRUE UNTIL IMPLEMENTED / MANY TO ONE FOR NOW INDEF. POSTPONED - SEEING HOW MUCH TIME REMAINING
    private Integer matchId;

    @Column(name = "reason", length = 25, nullable = false)
    private String reason;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "description", length = 255)
    private String description;
}