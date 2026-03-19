package lu.ephec.backend_projetdv2026.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="\"Fields\"")
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //gen by DB
    @Column(name = "field_id")
    private Integer fieldId;

    @Column(name = "site_id")
    private Integer siteId;

    @Column(name = "is_indoor", nullable = false)
    private Boolean isIndoor = false; // DB default 0 -> false

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // DB default 1 -> true

    @Column(name = "maintenance_from_date")
    private LocalDate maintenanceFromDate;

    @Column(name = "maintenance_to_date")
    private LocalDate maintenanceToDate;

    /*
    // ManyToOne relation to Site (foreign key site_id). Keep nullable = false if a Field must belong to a Site.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false)
    private Site site;
     */
}