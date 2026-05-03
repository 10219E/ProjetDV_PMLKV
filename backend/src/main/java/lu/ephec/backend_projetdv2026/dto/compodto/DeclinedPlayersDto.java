package lu.ephec.backend_projetdv2026.dto.compodto;

import java.io.Serializable;

/**
 * DTO for declined player information
 */
public class DeclinedPlayersDto implements Serializable {
    private String playerId;
    private String playerName;
    private String playerRole;
    private String playerStatus;

    /**
     * Default constructor
     */
    public DeclinedPlayersDto() {
    }

    /**
     * Constructor with all fields
     * @param playerId The ID of the player
     * @param playerName The full name of the player
     * @param playerRole The role of the player in the match
     * @param playerStatus The status of the player
     */
    public DeclinedPlayersDto(String playerId, String playerName, String playerRole, String playerStatus) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerRole = playerRole;
        this.playerStatus = playerStatus;
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerRole() {
        return playerRole;
    }

    public void setPlayerRole(String playerRole) {
        this.playerRole = playerRole;
    }

    public String getPlayerStatus() {
        return playerStatus;
    }

    public void setPlayerStatus(String playerStatus) {
        this.playerStatus = playerStatus;
    }

    @Override
    public String toString() {
        return "DeclinedPlayersDto{" +
                "playerId='" + playerId + '\'' +
                ", playerName='" + playerName + '\'' +
                ", playerRole='" + playerRole + '\'' +
                ", playerStatus='" + playerStatus + '\'' +
                '}';
    }
}