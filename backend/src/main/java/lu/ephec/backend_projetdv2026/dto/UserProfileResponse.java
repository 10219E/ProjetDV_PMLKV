package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.models.UsersSites;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class UserProfileResponse {
    private String matricule;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private String level;
    private Boolean isActive;
    private Short roleId;

    private UserAccountDto account;
    private List<UserPenaltyDto> penalties;
    private List<UserSiteDto> sites;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String matricule, String firstName, String lastName, String email, LocalDate birthDate, String level, Boolean isActive, Short roleId, UserAccountDto account, List<UserPenaltyDto> penalties, List<UserSiteDto> sites) {
        this.matricule = matricule;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.level = level;
        this.isActive = isActive;
        this.roleId = roleId;
        this.account = account;
        this.penalties = penalties;
        this.sites = sites;
    }

    public static UserProfileResponse from(User user, UserAccounts accountModel, List<UsersSites> userSites) {
        Short rId = null;
        if (user.getRole() != null) {
            rId = user.getRole().getId();
        }

        UserAccountDto accDto = accountModel != null ? new UserAccountDto(
                accountModel.getBalance(),
                accountModel.getLastUpdate(),
                accountModel.getStatus()) : null;

        List<UserPenaltyDto> penDtoList = null;
        if (user.getPenalties() != null) {
            penDtoList = user.getPenalties().stream().map(p -> new UserPenaltyDto(
                    p.getTr(),
                    p.getReason(),
                    p.getStartDate(),
                    p.getEndDate(),
                    p.getIsActive(),
                    p.getDescription()
            )).collect(Collectors.toList());
        }

        List<UserSiteDto> sitDtoList = null;
        if (userSites != null) {
            sitDtoList = userSites.stream().map(s -> new UserSiteDto(
                    s.getSite().getSiteId(),
                    s.getSite().getName(),
                    s.getIsPrimary(),
                    s.getIsVip()
            )).collect(Collectors.toList());
        }

        return new UserProfileResponse(
                user.getMatricule(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getLevel(),
                user.getIsActive(),
                rId,
                accDto,
                penDtoList,
                sitDtoList
        );
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Short getRoleId() { return roleId; }
    public void setRoleId(Short roleId) { this.roleId = roleId; }
    public UserAccountDto getAccount() { return account; }
    public void setAccount(UserAccountDto account) { this.account = account; }
    public List<UserPenaltyDto> getPenalties() { return penalties; }
    public void setPenalties(List<UserPenaltyDto> penalties) { this.penalties = penalties; }
    public List<UserSiteDto> getSites() { return sites; }
    public void setSites(List<UserSiteDto> sites) { this.sites = sites; }
}
