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
@Table(name="\"Sites_ClosureDays\"")
public class SiteClosureDays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tr")
    private Integer closureId;

    @Column(name = "site_id", nullable = false)
    private Integer siteId;

    @Column(name = "closure_date", nullable = false)
    private LocalDate closureDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "for_all", nullable = true) //default BIT 0
    private Boolean forAll;

    // ManyToOne relationship to Site (owning side)
    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private Site site;
}