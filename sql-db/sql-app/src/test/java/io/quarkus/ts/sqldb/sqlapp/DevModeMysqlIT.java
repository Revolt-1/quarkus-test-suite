package io.quarkus.ts.sqldb.sqlapp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

@Tag("QUARKUS-959")
@QuarkusScenario
@Tag("podman-incompatible") //todo https://github.com/quarkusio/quarkus/issues/33985
public class DevModeMysqlIT extends AbstractSqlDatabaseIT {

    @DevModeQuarkusApplication
    static RestService app = new RestService().withProperties("mysql.properties");

    @Test
    public void mysqlContainerShouldBeStarted() {
        app.logs().assertContains("Creating container for image: docker.io/mysql");
    }
}
