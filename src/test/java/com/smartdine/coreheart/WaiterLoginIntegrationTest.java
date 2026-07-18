package com.smartdine.coreheart;

import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.service.ActivationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WaiterLoginIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ActivationService activationService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testWaiterActivationAndPinLogin() throws Exception {
        // 1. Activate the system using the local mock cloud gateway
        String activationCode = "SD-a0eebc99";
        String gatewayUrl = "http://localhost:" + port + "/api/mock-cloud";
        
        activationService.activateSystem(activationCode, gatewayUrl);

        // Verify the system configuration state
        assertTrue(activationService.isSystemActivated(), "System should be activated");

        // 2. Perform PIN login for the custom waiter (Ravi Kumar, PIN 1001)
        UUID restaurantId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        PinLoginRequest loginRequest = new PinLoginRequest();
        loginRequest.setPin("1001");
        loginRequest.setRestaurantId(restaurantId);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/auth/pin-login",
                loginRequest,
                AuthResponse.class
        );

        // Verify the response
        assertEquals(200, response.getStatusCode().value(), "PIN Login should succeed");
        AuthResponse authResponse = response.getBody();
        assertNotNull(authResponse, "Auth response should not be null");
        assertNotNull(authResponse.getToken(), "Auth token should not be null");
        assertEquals("WAITER", authResponse.getRole(), "User role should be WAITER");
        assertEquals("Ravi Kumar", authResponse.getFullName(), "Waiter full name should match");
    }
}
