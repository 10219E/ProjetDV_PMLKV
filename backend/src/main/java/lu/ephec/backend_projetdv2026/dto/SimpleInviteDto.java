package lu.ephec.backend_projetdv2026.dto;

public class SimpleInviteDto {
    private String email;
    private String matricule;
    private Short roleId;
    private boolean hasActivePenalties;

    // Constructors
    public SimpleInviteDto() {
    }

    public SimpleInviteDto(String email, String matricule, Short roleId, boolean hasActivePenalties) {
        this.email = email;
        this.matricule = matricule;
        this.roleId = roleId;
        this.hasActivePenalties = hasActivePenalties;
    }

    // Getters and Setters
    public String getEmail() {return email;}

    public void setEmail(String email) {this.email = email;}

    public String getMatricule() {return matricule;}

    public void setMatricule(String matricule) {this.matricule = matricule;}

    public Short getRoleId() {return roleId;}

    public void setRoleId(Short roleId) {this.roleId = roleId;}

    public boolean isHasActivePenalties() {return hasActivePenalties;}

    public void setHasActivePenalties(boolean hasActivePenalties) {this.hasActivePenalties = hasActivePenalties;}
}