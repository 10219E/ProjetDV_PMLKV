package lu.ephec.backend_projetdv2026.services.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ManualHashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

        System.out.println("A001 -> " + enc.encode("S@dminPML!"));
        System.out.println("G0001 -> " + enc.encode("VIP@ccess!"));
        System.out.println("L0001 -> " + enc.encode("Invite@Usr!"));
        System.out.println("S0001 -> " + enc.encode("Norm@lS!te"));
    }
}