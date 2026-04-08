package lu.ephec.backend_projetdv2026.dto.security;

public class AuthLoginRequest {
    private String login; // email or matricule
    private String password;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
