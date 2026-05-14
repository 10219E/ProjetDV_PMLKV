package lu.ephec.backend_projetdv2026.services.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ManualHashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

        System.out.println("A001 -> " + enc.encode("S@dminPML1!")); //pmlkv@ephec.be
        System.out.println("A002 -> " + enc.encode("S@dminRHA1!")); //rhardenne@ephec.be
        System.out.println("M001 -> " + enc.encode("M@dminFVZ1!")); //vfievez@ephec.be
        System.out.println("M002 -> " + enc.encode("M@dminOGG1!")); //ogues@ephec.be
        System.out.println("G0001 -> " + enc.encode("VIP@ccess1!")); //mchlo@ephec.be
        System.out.println("G0002 -> " + enc.encode("VIP@ccess2!")); //jdupont@ephec.be ///HAS DEBT
        System.out.println("G0003 -> " + enc.encode("VIP@ccess3!")); //gguy@ephec.be
        System.out.println("G0004 -> " + enc.encode("Norm@lS!te5")); //clambert@ephec.be //REGISTERED THROUGH FORM AND UPGRADED TO VIP
        System.out.println("S0001 -> " + enc.encode("Norm@lS!te1")); //cmartin@ephec.be
        System.out.println("S0002 -> " + enc.encode("Norm@lS!te2")); //adubois@ephec.be ///HAD DEBT
        System.out.println("S0003 -> " + enc.encode("Norm@lS!te3")); //lvandriesche@ephec.be
        System.out.println("S0004 -> " + enc.encode("Norm@lS!te4")); //hmoret@ephec.be //REGISTERED THROUGH FORM
        System.out.println("L0001 -> " + enc.encode("Invite@Usr1!")); //sbernard@ephec.be
        System.out.println("L0002 -> " + enc.encode("Invite@Usr2!")); //tmara@ephec.be
        System.out.println("L0003 -> " + enc.encode("Invite@Usr3!")); //amariane@ephec.be
    }
}