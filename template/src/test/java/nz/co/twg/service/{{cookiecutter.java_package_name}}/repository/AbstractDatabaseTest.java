package nz.co.twg.service.{{cookiecutter.java_package_name}}.repository;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
* A base database test that initializes a containerized db instance and a spring context. All
* database related tests should extend off this class to share the same db instance.
*/
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = AbstractDatabaseTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
public abstract class AbstractDatabaseTest {

    private static final PostgreSQLContainer<?> postgreDBContainer =
            new PostgreSQLContainer<>("postgres:12-alpine")
                    .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));

    static {
        postgreDBContainer.start();
    }

    static class DockerPostgreDataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.jpa.hibernate.ddl-auto = create-drop",
                    "spring.datasource.url = " + postgreDBContainer.getJdbcUrl(),
                    "spring.datasource.username = " + postgreDBContainer.getUsername(),
                    "spring.datasource.password = " + postgreDBContainer.getPassword());
        }
    }

    @Autowired private EntityManager em;

    @Autowired private TransactionTemplate txnTemplate;

    /**
    * A wrapper so the wrapped code executes within a valid transaction boundary. Exposing the entity
    * manager directly to bypass the DAO under test so the testing of the DAO can be more isolated
    * when comes to setting up default data. <br>
    * Use this method if return value is of no interest. <br>
    * e.g. during data setup / teardown
    */
    protected void useTransaction(Consumer<EntityManager> entityManagerConsumer) {
        txnTemplate.execute(
                (status) -> {
                    entityManagerConsumer.accept(em);
                    return null;
                });
    }

    /**
    * A wrapper so the wrapped code executes within a valid transaction boundary. Exposing the entity
    * manager directly to bypass the DAO under test so the testing of the DAO can be more isolated
    * when comes to setting up default data. <br>
    * Use this method if a result must be returned. <br>
    * e.g. during the "when" query with result just before "then" assertions
    */
    protected <T> T withTransaction(Function<EntityManager, T> entityManagerConsumer) {
        return txnTemplate.execute((status) -> entityManagerConsumer.apply(em));
    }
}
