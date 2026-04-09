package lu.ephec.backend_projetdv2026.dto;

import java.time.LocalDateTime;

public class UserAccountDto {
    private Double balance;
    private LocalDateTime lastUpdate;
    private String status;

    public UserAccountDto() {}

    public UserAccountDto(Double balance, LocalDateTime lastUpdate, String status) {
        this.balance = balance;
        this.lastUpdate = lastUpdate;
        this.status = status;
    }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

