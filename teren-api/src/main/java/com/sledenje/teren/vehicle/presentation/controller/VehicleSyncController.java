package com.sledenje.teren.vehicle.presentation.controller;

import com.sledenje.teren.vehicle.domain.service.VehicleSyncService;
import com.sledenje.teren.vehicle.infrastructure.util.VehicleDtoConverter;
import com.sledenje.teren.vehicle.presentation.dto.UpdateVehicleSyncConfigRequest;
import com.sledenje.teren.vehicle.presentation.dto.VehicleSyncConfigDto;
import com.sledenje.teren.vehicle.presentation.dto.VehicleSyncResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Sinhronizacija vozil", description = "Prenos registrskih številk iz zunanjega sistema")
public class VehicleSyncController {

    private final VehicleSyncService vehicleSyncService;

    public VehicleSyncController(VehicleSyncService vehicleSyncService) {
        this.vehicleSyncService = vehicleSyncService;
    }

    @GetMapping("/sync-config")
    @Operation(summary = "Nastavitve sinhronizacije", description = "Vrne povezavo in izid zadnje sinhronizacije, brez žetona.")
    public ResponseEntity<VehicleSyncConfigDto> getSyncConfig() {
        return ResponseEntity.ok(VehicleDtoConverter.toDto(vehicleSyncService.findSyncConfig()));
    }

    @PutMapping("/sync-config")
    @Operation(summary = "Shranjevanje povezave",
            description = "Prazen žeton obdrži obstoječega, prazen URL izbriše povezavo skupaj z žetonom.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nastavitve shranjene"),
            @ApiResponse(responseCode = "400", description = "Neveljaven URL",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<VehicleSyncConfigDto> updateSyncConfig(
            @Valid @RequestBody UpdateVehicleSyncConfigRequest request) {
        return ResponseEntity.ok(VehicleDtoConverter.toDto(
                vehicleSyncService.updateSyncConfig(request.apiUrl(), request.apiToken())));
    }

    @PostMapping("/sync")
    @Operation(summary = "Zagon sinhronizacije",
            description = "Prenese vozila iz zunanjega sistema: znana po externalId posodobi, nova doda z izvorom SYNC. "
                    + "Shranijo se vsa ali nobeno.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sinhronizacija uspela"),
            @ApiResponse(responseCode = "409", description = "Prenesena registrska številka že obstaja pri drugem vozilu",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Povezava ni nastavljena",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Zunanji sistem ni odgovoril pravilno",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<VehicleSyncResultDto> syncVehicles() {
        return ResponseEntity.ok(new VehicleSyncResultDto(vehicleSyncService.syncVehicles()));
    }

}
