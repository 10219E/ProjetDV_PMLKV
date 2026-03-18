package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="\"Sites\"") //DB is case sensitive, Hibernate for some reason converts to lowercase by default
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //makes the value managed by the DB, auto-incremented
    @Column(name = "site_id")
    private Integer siteId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
