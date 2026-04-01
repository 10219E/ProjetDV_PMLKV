package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="\"Matches\"")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Integer matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", referencedColumnName = "field_id")
    private Field field;

    @Column(name = "type", length = 10, nullable = false)
    private String type; // 'private' or 'public'

    @Column(name = "pub_status", length = 15, nullable = true)
    private String pubStatus; // 'closed', 'open', 'completed', 'cancelled'

    @Column(name = "priv_status", length = 15, nullable = true)
    private String privStatus; // 'awaiting', 'confirmed', 'completed', 'cancelled'

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "min_players", nullable = true)
    private Integer minPlayers = 4;

    @Column(name = "max_players", nullable = true)
    private Integer maxPlayers = 4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organiser_id", referencedColumnName = "user_id")
    private User organiser;

}