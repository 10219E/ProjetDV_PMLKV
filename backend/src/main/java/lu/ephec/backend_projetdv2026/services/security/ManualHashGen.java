package lu.ephec.backend_projetdv2026.services.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ManualHashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

        System.out.println("A001 -> " + enc.encode("S@dminPML!"));
        System.out.println("G0001 -> " + enc.encode("VIP@ccess!")); //mchlo@ephec.be
        System.out.println("L0001 -> " + enc.encode("Invite@Usr!")); //sbernard@ephec.be
        System.out.println("S0001 -> " + enc.encode("Norm@lS!te")); //jmartin@ephec.be
        System.out.println("L0005 -> " + enc.encode("Invite@123!")); //invite@me.com
        System.out.println("L0006 -> " + enc.encode("S@dmin1!")); //sone@ephec.be
    }
}