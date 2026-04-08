package lu.ephec.backend_projetdv2026.dto.security;

public class AuthLoginResponse {
    private String tokenType;
    private String accessToken;
    private long expiresIn;

    public AuthLoginResponse(String tokenType, String accessToken, long expiresIn) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}