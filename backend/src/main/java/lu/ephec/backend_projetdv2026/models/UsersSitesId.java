package lu.ephec.backend_projetdv2026.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

//NECESSARY TO SUPPORT DUAL PRIMARY KEY IN USERS_SITES
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsersSitesId implements Serializable {
    private String user;   // matches field name in UsersSites entity
    private Integer site;  // matches field name in UsersSites entity
}