package lu.ephec.backend_projetdv2026.models;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data //Using Lombok data to add basic getter/setters without new JPA class - as table is just a reference
@NoArgsConstructor //Gives context to (constructor)
@Entity
@Table(name = "\"Users_Roles\"")
public class UserRoles {
    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;
}