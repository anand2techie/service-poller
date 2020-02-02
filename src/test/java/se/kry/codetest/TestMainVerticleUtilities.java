package se.kry.codetest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMainVerticleUtilities {
    @Test
    @DisplayName("Validate an example of an valid URL")
    void validateValidURL() {
        MainVerticle verticle = new MainVerticle(0, "");
        assertTrue(verticle.validURL("http://kry.se"));
    }

    @Test
    @DisplayName("Validate an example of an invalid URL")
    void validateInvalidURL_InvalidProtocol() {
        MainVerticle verticle = new MainVerticle(0, "");
        assertFalse(verticle.validURL("ftp://kry.se"));
    }

    @Test
    @DisplayName("Test serialization and deserialization of stored services")
    void serializeDeserializeServices() throws IOException {
        File file = File.createTempFile("test", "cache");
        MainVerticle verticle = new MainVerticle(0, file.getAbsolutePath());
        verticle.storeNewService("test");

        MainVerticle newVerticle = new MainVerticle(0, file.getAbsolutePath());
        newVerticle.loadServices();
        assertEquals(verticle.getServices(), newVerticle.getServices());
    }
}
