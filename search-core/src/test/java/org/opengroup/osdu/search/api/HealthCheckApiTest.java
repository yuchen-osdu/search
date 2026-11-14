package org.opengroup.osdu.search.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class HealthCheckApiTest {

  @InjectMocks private HealthCheckApi healthCheckApi;

  @Test
  public void test_livenessCheck() {
    ResponseEntity<Void> response = healthCheckApi.livenessCheck();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  public void test_readinessCheck() {
    ResponseEntity<Void> response = healthCheckApi.readinessCheck();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody());
  }
}
