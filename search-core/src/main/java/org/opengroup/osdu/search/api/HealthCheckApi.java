package org.opengroup.osdu.search.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Tag(name = "health-check-api", description = "Health Check API")
public class HealthCheckApi {

  @Operation(
      summary = "${healthChecksApi.livenessCheck.summary}",
      description = "${healthChecksApi.livenessCheck.description}",
      tags = {"health-check-api"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
            responseCode = "502",
            description = "Bad Gateway",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "503",
            description = "Service Unavailable",
            content = {@Content(schema = @Schema(implementation = AppError.class))})
      })
  // Public endpoint: no authentication required (see @PermitAll). Empty @SecurityRequirements
  // overrides the global bearer security so the generated spec advertises `security: []`.
  @SecurityRequirements
  @PermitAll
  @GetMapping(value = "/liveness_check", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> livenessCheck() {
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "${healthChecksApi.readinessCheck.summary}",
      description = "${healthChecksApi.readinessCheck.description}",
      tags = {"health-check-api"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
            responseCode = "502",
            description = "Bad Gateway",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "503",
            description = "Service Unavailable",
            content = {@Content(schema = @Schema(implementation = AppError.class))})
      })
  // Public endpoint: no authentication required (see @PermitAll). Empty @SecurityRequirements
  // overrides the global bearer security so the generated spec advertises `security: []`.
  @SecurityRequirements
  @PermitAll
  @GetMapping(value = "/readiness_check", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> readinessCheck() {
    return ResponseEntity.ok().build();
  }
}
