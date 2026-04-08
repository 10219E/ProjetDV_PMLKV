package lu.ephec.backend_projetdv2026;

import org.springframework.test.context.ActiveProfiles;

//Init profile "test" for all H2 tests to avoid conflicts with live DB tests and ensure they use the in-memory H2 database configuration
@ActiveProfiles("test")
public abstract class InitBaseH2Test {
}