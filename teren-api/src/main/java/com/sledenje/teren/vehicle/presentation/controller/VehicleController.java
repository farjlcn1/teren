package com.sledenje.teren.vehicle.presentation.controller;

import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.service.VehicleService;
import com.sledenje.teren.vehicle.infrastructure.util.VehicleDtoConverter;
import com.sledenje.teren.vehicle.presentation.dto.ImportVehiclesRequest;
import com.sledenje.teren.vehicle.presentation.dto.ImportVehiclesResultDto;
import com.sledenje.teren.vehicle.presentation.dto.VehicleDto;
import com.sledenje.teren.vehicle.presentation.dto.VehiclePlateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vozila", description = "Pregled, vnos, urejanje in uvoz registrskih številk")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    @Operation(summary = "Seznam vozil", description = "Vrne vsa vozila, urejena po registrski številki.")
    public ResponseEntity<List<VehicleDto>> getAllVehicles() {
        List<VehicleDto> vehicleDtos = vehicleService.findAllVehicles().stream()
                .map(VehicleDtoConverter::toDto)
                .toList();
        return ResponseEntity.ok(vehicleDtos);
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Podrobnosti vozila")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vozilo najdeno"),
            @ApiResponse(responseCode = "404", description = "Vozilo s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable String vehicleId) {
        return ResponseEntity.ok(VehicleDtoConverter.toDto(vehicleService.findVehicleById(vehicleId)));
    }

    @PostMapping
    @Operation(summary = "Vnos vozila", description = "Doda vozilo z izvorom MANUAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vozilo dodano"),
            @ApiResponse(responseCode = "400", description = "Manjka registrska številka",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Registrska številka že obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody VehiclePlateRequest request) {
        Vehicle createdVehicle = vehicleService.createVehicle(request.plate());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{vehicleId}")
                .buildAndExpand(createdVehicle.id())
                .toUri();
        return ResponseEntity.created(location).body(VehicleDtoConverter.toDto(createdVehicle));
    }

    @PostMapping("/import")
    @Operation(summary = "Uvoz vozil",
            description = "Doda vozila z izvorom IMPORT. Obstoječe in ponovljene registrske številke preskoči.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vozila uvožena"),
            @ApiResponse(responseCode = "400", description = "Prazna registrska številka v seznamu",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ImportVehiclesResultDto> importVehicles(@Valid @RequestBody ImportVehiclesRequest request) {
        return ResponseEntity.ok(VehicleDtoConverter.toDto(vehicleService.importVehicles(request.plates())));
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "Sprememba registrske številke")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vozilo posodobljeno"),
            @ApiResponse(responseCode = "400", description = "Manjka registrska številka",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Vozilo s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Registrsko številko ima že drugo vozilo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable String vehicleId,
            @Valid @RequestBody VehiclePlateRequest request) {
        Vehicle updatedVehicle = vehicleService.updateVehiclePlate(vehicleId, request.plate());
        return ResponseEntity.ok(VehicleDtoConverter.toDto(updatedVehicle));
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Brisanje vozila",
            description = "Delovni nalogi in plan hranijo registrsko številko kot besedilo, zato ostanejo nespremenjeni.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vozilo izbrisano"),
            @ApiResponse(responseCode = "404", description = "Vozilo s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteVehicle(@PathVariable String vehicleId) {
        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }

}
