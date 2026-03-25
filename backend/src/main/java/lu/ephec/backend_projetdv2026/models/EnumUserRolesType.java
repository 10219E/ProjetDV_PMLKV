package lu.ephec.backend_projetdv2026.models;

public enum EnumUserRolesType {
    INVITE(0, "invite", "L"),
    ONE_SITE_SUBSCRIBER(1, "subscribed", "S"),
    ALL_SITE_ACCESS(2, "all_site", "G"),
    SITE_ADMIN(7, "site_admin", "M"),
    ADMIN(9, "as_admin", "A");

    private final Short id;
    private final String displayName;
    private final String prefix;

    EnumUserRolesType(int id, String displayName, String prefix) {
        this.id = (short) id;
        this.displayName = displayName;
        this.prefix = prefix;
    }

    //GET ROLE ID
    public Short getId() {
        return id;
    }

    //GET NAME
    public String getDisplayName() {
        return displayName;
    }

    //GET MATRICULE PREFIX
    public String getPrefix() {
        return prefix;
    }

    //GET ROLE BY ID
    public static EnumUserRolesType fromId(Short id) {
        for (EnumUserRolesType role : values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        return null;
    }

    //CHECK ADMIN
    public boolean isAdmin() {
        return this == SITE_ADMIN || this == ADMIN;
    }

    //CHECK NORMAL
    public boolean isNormalUser() {
        return !isAdmin();
    }
}