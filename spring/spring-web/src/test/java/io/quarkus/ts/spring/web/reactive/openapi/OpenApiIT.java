package io.quarkus.ts.spring.web.reactive.openapi;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import io.quarkus.test.bootstrap.MariaDbService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.spring.web.reactive.AbstractDbIT;
import io.quarkus.ts.spring.web.reactive.MariaDBUtils;
import io.restassured.response.Response;

@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenApiIT extends AbstractDbIT {
    private static Response response;

    @Container(image = "${mariadb.10.image}", port = MariaDBUtils.PORT, expectedLog = MariaDBUtils.START_LOG)
    static final MariaDbService database = new MariaDbService();

    @QuarkusApplication
    private static final RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @Override
    public RestService getApp() {
        return app;
    }

    private void callOpenApiEndpoint() {
        response = app.given().get("/q/openapi?format=JSON");
    }

    @Order(1)
    @Test
    public void testResponseOk() {
        callOpenApiEndpoint();
        Assertions.assertEquals(response.statusCode(), HttpStatus.SC_OK);
    }

    @Order(2)
    @ParameterizedTest
    @CsvFileSource(resources = "/request-types.csv")
    public void testRequestType(String path, String method, String type) {
        checkType(Stream.of("paths", path, method, "requestBody", "content"), type);
    }

    @Order(3)
    @ParameterizedTest
    @CsvFileSource(resources = "/response-types.csv")
    public void testResponseType(String path, String method, String type) {
        checkType(Stream.of("paths", path, method, "responses", "200", "content"), type);
    }

    private void checkType(Stream<String> jsonPathStream, String type) {
        final String jsonPath = jsonPathStream
                .map(pathChunk -> "'" + pathChunk + "'")
                .collect(Collectors.joining("."));
        final Map<String, Object> objectMap = response.jsonPath().getMap(jsonPath);
        Assertions.assertNotNull(objectMap);
        Assertions.assertEquals(1, objectMap.keySet().size());
        Assertions.assertEquals(type, objectMap.keySet().iterator().next());
    }
}
