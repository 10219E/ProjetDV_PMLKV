package lu.ephec.backend_projetdv2026;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Disabled("Requires live DB - skipped by default")
public class JdbcLiveDbConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testConnection() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertEquals(1, result);
            System.out.println("✅ Test query returned ✅ " + result);
        } catch (Exception e) {
            System.out.println("❌ Test query failed ❌ ");
            throw e;
        }
    }
}