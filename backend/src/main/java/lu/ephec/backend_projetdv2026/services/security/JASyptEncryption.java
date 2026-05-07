package lu.ephec.backend_projetdv2026.services.security;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class JASyptEncryption {
    public static void main(String[] args) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("EPHEC2026"); //secret key
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        String encrypted = encryptor.encrypt(""); //enter pass here (you can find it in init-sql-server.sql)
        System.out.println("ENC(" + encrypted + ")");
        testJasypt(encrypted);
    }

    public static void testJasypt(String encrypted) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("EPHEC2026");
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        String decrypted = encryptor.decrypt(encrypted);
        System.out.println("Decrypted: " + decrypted);
    }
}
