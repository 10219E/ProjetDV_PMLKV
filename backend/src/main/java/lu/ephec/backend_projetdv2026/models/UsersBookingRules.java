package lu.ephec.backend_projetdv2026.models;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "\"Users_Booking_Rules\"")
public class UsersBookingRules {

    @Id
    @Column(name = "role_id")
    private Short roleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "role_id")
    private UserRoles role;

    @Column(name = "allowed_duration", nullable = false)
    private Integer allowedDuration = 5;
}