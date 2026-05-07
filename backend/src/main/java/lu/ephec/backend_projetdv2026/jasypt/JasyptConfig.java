package lu.ephec.backend_projetdv2026.jasypt;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor encryptor() {

        PooledPBEStringEncryptor encryptor =
                new PooledPBEStringEncryptor();

        SimpleStringPBEConfig config =
                new SimpleStringPBEConfig();

        config.setPassword("EPHEC2026");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName(
                "org.jasypt.salt.RandomSaltGenerator");

        config.setStringOutputType("base64");

        encryptor.setConfig(config);

        return encryptor;
    }
}