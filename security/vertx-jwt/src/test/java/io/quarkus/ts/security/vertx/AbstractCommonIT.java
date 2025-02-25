package io.quarkus.ts.security.vertx;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;

import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.security.vertx.model.BladeRunner;
import io.quarkus.ts.security.vertx.model.Replicant;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.mutiny.core.Vertx;

public abstract class AbstractCommonIT {

    static final int REDIS_PORT = 6379;
    private static final String JWT_ALGORITHM = "HS256";
    private static final String JWT_SECRET = "keepSecret";

    BladeRunner bladeRunner;
    Replicant replicant;
    Vertx vertx;

    @Container(image = "${redis.image}", port = REDIS_PORT, expectedLog = "Ready to accept connections")
    static DefaultService redis = new DefaultService().withProperty("ALLOW_EMPTY_PASSWORD", "YES");

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.redis.hosts",
                    () -> {
                        String redisHost = redis.getURI().withScheme("redis").getRestAssuredStyleUri();
                        return String.format("%s:%d", redisHost, redis.getURI().getPort());
                    });

    @BeforeEach
    public void setup() {
        this.vertx = Vertx.vertx();
        bladeRunner = defaultBladeRunner();
        app.given().accept(ContentType.JSON)
                .headers("Authorization", "Bearer " + JWT(Invalidity.EMPTY, "admin"))
                .body(bladeRunner.toJsonEncoded())
                .when()
                .post("/bladeRunner/")
                .then()
                .statusCode(200);

        replicant = defaultReplicant();
        app.given().accept(ContentType.JSON)
                .headers("Authorization", "Bearer " + JWT(Invalidity.EMPTY, "admin"))
                .body(replicant.toJsonEncoded())
                .when()
                .post("/replicant/")
                .then()
                .statusCode(200);
    }

    @AfterEach
    public void teardown() {
        app.given().accept(ContentType.JSON)
                .headers("Authorization", "Bearer " + JWT(Invalidity.EMPTY, "admin"))
                .when()
                .delete("/bladeRunner/" + bladeRunner.getId())
                .then()
                .statusCode(anyOf(is(204), is(404)));

        app.given().accept(ContentType.JSON)
                .headers("Authorization", "Bearer " + JWT(Invalidity.EMPTY, "admin"))
                .when()
                .delete("/replicant/" + replicant.getId())
                .then()
                .statusCode(anyOf(is(204), is(404)));
    }

    protected enum Invalidity {
        EMPTY,
        WRONG_ISSUER,
        WRONG_AUDIENCE,
        AHEAD_OF_TIME,
        EXPIRED,
        WRONG_KEY
    }

    protected String JWT(Invalidity invalidity, String... groups) {
        JsonObject authConfig = defaultAuthConfig();
        JsonObject claims = defaultClaims(groups);
        JWTAuth jwt = JWTAuth.create(vertx.getDelegate(), new JWTAuthOptions()
                .addPubSecKey(getPubSecKeyOptions(authConfig)));
        switch (invalidity) {
            case WRONG_ISSUER:
                claims.put("iss", "invalid");
                break;
            case WRONG_AUDIENCE:
                claims.put("aud", "invalid");
                break;
            case AHEAD_OF_TIME:
                claims.put("iat", currentTimePLusOneEpoch());
                break;
            case EXPIRED:
                claims.put("exp", currentTimeEpoch());
                break;
            case WRONG_KEY:
                authConfig.put("publicKey", "invalid");
                jwt = JWTAuth.create(vertx.getDelegate(), new JWTAuthOptions()
                        .addPubSecKey(getPubSecKeyOptions(authConfig)));
                break;
        }

        return jwt.generateToken(claims);
    }

    private JsonObject defaultAuthConfig() {
        return new JsonObject()
                .put("symmetric", true)
                .put("algorithm", JWT_ALGORITHM)
                .put("publicKey", JWT_SECRET);
    }

    private PubSecKeyOptions getPubSecKeyOptions(JsonObject authConfig) {
        return new PubSecKeyOptions(authConfig).setBuffer(authConfig.getBuffer("publicKey"));
    }

    private JsonObject defaultClaims(String... groups) {
        Long now = currentTimeEpoch();
        Long expiration = currentTimePLusOneEpoch();
        return new JsonObject()
                .put("name", "John Doe")
                .put("sub", "bff")
                .put("iss", "vertxJWT@redhat.com")
                .put("aud", "third_party")
                .put("groups", Arrays.asList(groups))
                .put("iat", now)
                .put("exp", expiration);
    }

    private Long currentTimeEpoch() {
        return currentTime().toInstant().toEpochMilli() / 1000L;
    }

    private Long currentTimePLusOneEpoch() {
        return currentTime().plusMinutes(1).toInstant().toEpochMilli() / 1000L;
    }

    private ZonedDateTime currentTime() {
        return ZonedDateTime.now();
    }

    protected BladeRunner defaultBladeRunner() {
        BladeRunner bladeRunner = new BladeRunner();
        bladeRunner.setDailyRate(2000.00);
        bladeRunner.setRetirements(103);
        bladeRunner.setVoightKampffTestAmount(1500);
        bladeRunner.setName("Rick");
        bladeRunner.setLastName("Deckard");
        bladeRunner.setIq(105);

        return bladeRunner;
    }

    protected Replicant defaultReplicant() {
        Replicant replicant = new Replicant();
        replicant.setFugitive(true);
        replicant.setLiveSpanYears(5);
        replicant.setModel("Nexus 5");
        replicant.setTelepathy(true);
        replicant.setName("Alan");
        replicant.setLastName("Greg");
        replicant.setIq(185);

        return replicant;
    }
}
