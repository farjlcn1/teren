package com.sledenje.teren.client.presentation.controller;

import com.sledenje.teren.client.domain.service.ClientSyncService;
import com.sledenje.teren.client.infrastructure.util.ClientDtoConverter;
import com.sledenje.teren.client.presentation.dto.ClientSyncConfigDto;
import com.sledenje.teren.client.presentation.dto.ClientSyncResultDto;
import com.sledenje.teren.client.presentation.dto.UpdateClientSyncConfigRequest;
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
@RequestMapping("/api/v1/clients")
@Tag(name = "Sinhronizacija strank", description = "Prenos strank iz zunanjega sistema")
public class ClientSyncController {

    private final ClientSyncService clientSyncService;

    public ClientSyncController(ClientSyncService clientSyncService) {
        this.clientSyncService = clientSyncService;
    }

    @GetMapping("/sync-config")
    @Operation(summary = "Nastavitve sinhronizacije", description = "Vrne povezavo in izid zadnje sinhronizacije, brez žetona.")
    public ResponseEntity<ClientSyncConfigDto> getSyncConfig() {
        return ResponseEntity.ok(ClientDtoConverter.toDto(clientSyncService.findSyncConfig()));
    }

    @PutMapping("/sync-config")
    @Operation(summary = "Shranjevanje povezave",
            description = "Prazen žeton obdrži obstoječega, če se URL ne spremeni; prazen URL izbriše povezavo skupaj z žetonom.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nastavitve shranjene"),
            @ApiResponse(responseCode = "400", description = "Neveljaven URL",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClientSyncConfigDto> updateSyncConfig(
            @Valid @RequestBody UpdateClientSyncConfigRequest request) {
        return ResponseEntity.ok(ClientDtoConverter.toDto(
                clientSyncService.updateSyncConfig(request.apiUrl(), request.apiToken())));
    }

    @PostMapping("/sync")
    @Operation(summary = "Zagon sinhronizacije",
            description = "Prenese stranke iz zunanjega sistema: znane po externalId posodobi, nove doda z izvorom SYNC.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sinhronizacija uspela"),
            @ApiResponse(responseCode = "422", description = "Povezava ni nastavljena",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Zunanji sistem ni odgovoril pravilno",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClientSyncResultDto> syncClients() {
        return ResponseEntity.ok(new ClientSyncResultDto(clientSyncService.syncClients()));
    }

}
