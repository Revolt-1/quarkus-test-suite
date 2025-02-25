package io.quarkus.ts.sqldb.sqlapp;

import static io.quarkus.ts.sqldb.sqlapp.DbUtil.getImageName;
import static io.quarkus.ts.sqldb.sqlapp.DbUtil.getImageVersion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.dockerjava.api.model.Image;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.SocketUtils;

@Tag("QUARKUS-959")
@QuarkusScenario
@Tag("podman-incompatible") //todo https://github.com/quarkusio/quarkus/issues/33985
public class DevModePostgresqlDevServiceUserExperienceIT {

    // we use '-alpine' version as no other test is using it, which mitigates the fact that sometimes
    // io.quarkus.test.utils.DockerUtils.removeImage doesn't work as expected
    // TODO: drop suffix when https://github.com/quarkus-qe/quarkus-test-suite/issues/1227 is fixed
    private static final String POSTGRESQL_VERSION = getImageVersion("postgresql.latest.image") + "-alpine";
    private static final String POSTGRES_NAME = getImageName("postgresql.latest.image");

    @DevModeQuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.db-kind", "postgresql")
            .withProperty("quarkus.datasource.devservices.port", Integer.toString(SocketUtils.findAvailablePort()))
            .withProperty("quarkus.datasource.devservices.image-name", "${postgresql.latest.image}-alpine")
            .withProperty("quarkus.hibernate-orm.database.generation", "none")
            .onPreStart(s -> DockerUtils.removeImage(POSTGRES_NAME, POSTGRESQL_VERSION));

    @Test
    public void verifyIfUserIsInformedAboutPostgresqlDevServicePulling() {
        app.logs().assertContains(String.format("Pulling docker image: %s:%s", POSTGRES_NAME, POSTGRESQL_VERSION));
        app.logs().assertContains("Please be patient; this may take some time but only needs to be done once");
        app.logs().assertContains("Starting to pull image");
        app.logs().assertContains("Dev Services for PostgreSQL started");
    }

    @Test
    public void verifyPostgresqlImage() {
        Image postgresImg = DockerUtils.getImage(POSTGRES_NAME, POSTGRESQL_VERSION);
        Assertions.assertFalse(postgresImg.getId().isEmpty(), String.format("%s:%s not found. " +
                "Notice that user set his own custom image by 'quarkus.datasource.devservices.image-name' property",
                POSTGRES_NAME, POSTGRESQL_VERSION));
    }

    @AfterAll
    //TODO workaround for podman 4.4.1 on rhel. Without it, *next* test (eg MariaDBDatabaseIT) fails with "broken pipe"
    public static void clear() {
        DockerUtils.removeImage(POSTGRES_NAME, POSTGRESQL_VERSION);
    }
}
