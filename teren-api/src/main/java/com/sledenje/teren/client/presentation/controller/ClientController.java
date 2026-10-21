package com.sledenje.teren.client.presentation.controller;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.service.ClientService;
import com.sledenje.teren.client.infrastructure.util.ClientDtoConverter;
import com.sledenje.teren.client.presentation.dto.ClientDto;
import com.sledenje.teren.client.presentation.dto.CreateClientRequest;
import com.sledenje.teren.client.presentation.dto.ImportClientsRequest;
import com.sledenje.teren.client.presentation.dto.ImportClientsResultDto;
import com.sledenje.teren.client.presentation.dto.UpdateClientRequest;
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
@RequestMapping("/api/v1/clients")
@Tag(name = "Stranke", description = "Pregled, vnos, urejanje in uvoz strank")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "Seznam strank", description = "Vrne vse stranke, urejene po imenu.")
    public ResponseEntity<List<ClientDto>> getAllClients() {
        List<ClientDto> clientDtos = clientService.findAllClients().stream()
                .map(ClientDtoConverter::toDto)
                .toList();
        return ResponseEntity.ok(clientDtos);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Podrobnosti stranke")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stranka najdena"),
            @ApiResponse(responseCode = "404", description = "Stranka s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClientDto> getClientById(@PathVariable String clientId) {
        return ResponseEntity.ok(ClientDtoConverter.toDto(clientService.findClientById(clientId)));
    }

    @PostMapping
    @Operation(summary = "Vnos nove stranke", description = "Doda stranko z izvorom MANUAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stranka dodana"),
            @ApiResponse(responseCode = "400", description = "Neveljavni podatki (npr. manjka ime)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClientDto> createClient(@Valid @RequestBody CreateClientRequest request) {
        Client createdClient = clientService.createClient(ClientDtoConverter.toNewClient(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{clientId}")
                .buildAndExpand(createdClient.id())
                .toUri();
        return ResponseEntity.created(location).body(ClientDtoConverter.toDto(createdClient));
    }

    @PostMapping("/import")
    @Operation(summary = "Uvoz strank", description = "Doda stranke z izvorom IMPORT. Shranijo se vse ali nobena.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stranke uvožene"),
            @ApiResponse(responseCode = "400", description = "Neveljavni podatki (npr. stranka brez imena)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ImportClientsResultDto> importClients(@Valid @RequestBody ImportClientsRequest request) {
        int createdCount = clientService.importClients(ClientDtoConverter.toNewClients(request));
        return ResponseEntity.ok(new ImportClientsResultDto(createdCount));
    }

    @PutMapping("/{clientId}")
    @Operation(summary = "Urejanje stranke", description = "Zamenja ime, naslov in kontakt. Izvor ostane enak.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stranka posodobljena"),
            @ApiResponse(responseCode = "400", description = "Neveljavni podatki (npr. manjka ime)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Stranka s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClientDto> updateClient(@PathVariable String clientId,
            @Valid @RequestBody UpdateClientRequest request) {
        Client updatedClient = clientService.updateClient(clientId, ClientDtoConverter.toClientUpdate(request));
        return ResponseEntity.ok(ClientDtoConverter.toDto(updatedClient));
    }

    @DeleteMapping("/{clientId}")
    @Operation(summary = "Brisanje stranke")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stranka izbrisana"),
            @ApiResponse(responseCode = "404", description = "Stranka s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Stranka ima delovne naloge ali dogodke v planu",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

}
