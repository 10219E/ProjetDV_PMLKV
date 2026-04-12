package lu.ephec.backend_projetdv2026.dto;


public class UserRegistrationDto {
	private String fname;
	private String lname;
	private String email;
	private String password;
	private String bdate; // yyyy-MM-dd
	private String lvl;
	private Integer siteId;
	private Short roleId;

	public String getFname() { return fname; }
	public void setFname(String fname) { this.fname = fname; }
	public String getLname() { return lname; }
	public void setLname(String lname) { this.lname = lname; }
	public Integer getSiteId() { return siteId; }
	public void setSiteId(Integer siteId) { this.siteId = siteId; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public String getBdate() { return bdate; }
	public void setBdate(String bdate) { this.bdate = bdate; }
	public String getLvl() { return lvl; }
	public void setLvl(String lvl) { this.lvl = lvl; }
	public Short getRoleId() { return roleId; }
	public void setRoleId(Short roleId) { this.roleId = roleId; }
}
