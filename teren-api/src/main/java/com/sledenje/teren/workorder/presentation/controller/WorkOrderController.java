package com.sledenje.teren.workorder.presentation.controller;

import com.sledenje.teren.workorder.domain.model.CreatedWorkOrder;
import com.sledenje.teren.workorder.domain.service.WorkOrderService;
import com.sledenje.teren.workorder.infrastructure.util.WorkOrderDtoConverter;
import com.sledenje.teren.workorder.presentation.dto.CreateWorkOrderRequest;
import com.sledenje.teren.workorder.presentation.dto.CreatedWorkOrderDto;
import com.sledenje.teren.workorder.presentation.dto.UpdateWorkOrderRequest;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderAttachmentsRequest;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderDetailDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderDto;
import com.sledenje.teren.workorder.presentation.dto.WorkOrderSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/work-orders")
@Tag(name = "Delovni nalogi", description = "Pregled, vnos in urejanje delovnih nalogov")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    @Operation(summary = "Seznam nalogov",
            description = "Filtri se seštevajo (IN). Kdo sme videti katere naloge, preveri aplikacija "
                    + "(uporabnikom brez pregleda vseh nalogov pošlje createdById).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam nalogov"),
            @ApiResponse(responseCode = "400", description = "Neveljaven parameter (npr. neznana vrsta ali datum)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<WorkOrderDto>> getWorkOrders(
            @ParameterObject @Valid @ModelAttribute WorkOrderSearchRequest request) {
        List<WorkOrderDto> workOrderDtos = workOrderService.findWorkOrders(WorkOrderDtoConverter.toSearch(request))
                .stream()
                .map(WorkOrderDtoConverter::toDto)
                .toList();
        return ResponseEntity.ok(workOrderDtos);
    }

    @GetMapping("/imeis")
    @Operation(summary = "Vsi IMEI-ji", description = "Različni IMEI-ji vseh nalogov, urejeni naraščajoče (za filter).")
    public ResponseEntity<List<String>> getDistinctImeis() {
        return ResponseEntity.ok(workOrderService.findDistinctImeis());
    }

    @GetMapping("/{workOrderId}")
    @Operation(summary = "Podrobnosti naloga", description = "Nalog s fotografijami in zgodovino urejanj.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nalog najden"),
            @ApiResponse(responseCode = "404", description = "Nalog s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<WorkOrderDetailDto> getWorkOrder(@PathVariable String workOrderId) {
        return ResponseEntity.ok(WorkOrderDtoConverter.toDto(workOrderService.findWorkOrderDetail(workOrderId)));
    }

    @PostMapping
    @Operation(summary = "Vnos naloga",
            description = "V eni transakciji dodeli številko naloga, shrani nalog, doda vozilo, če ga še ni, in nalog "
                    + "zabeleži v planu (označi planirani nalog ali doda/ustvari dogodek).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nalog ustvarjen"),
            @ApiResponse(responseCode = "400", description = "Manjka obvezno polje",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Vsebina ne ustreza pravilom ali stranka ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreatedWorkOrderDto> createWorkOrder(@Valid @RequestBody CreateWorkOrderRequest request) {
        CreatedWorkOrder createdWorkOrder = workOrderService.createWorkOrder(WorkOrderDtoConverter.toNewWorkOrder(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{workOrderId}")
                .buildAndExpand(createdWorkOrder.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CreatedWorkOrderDto(createdWorkOrder.id(), createdWorkOrder.ident()));
    }

    @PutMapping("/{workOrderId}")
    @Operation(summary = "Urejanje naloga", description = "Spremembe se zapišejo v zgodovino urejanj.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nalog shranjen"),
            @ApiResponse(responseCode = "404", description = "Nalog s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Vsebina ne ustreza pravilom ali stranka ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> updateWorkOrder(@PathVariable String workOrderId,
            @Valid @RequestBody UpdateWorkOrderRequest request) {
        workOrderService.updateWorkOrder(workOrderId, WorkOrderDtoConverter.toDetails(request.details()),
                request.editedById());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workOrderId}/attachments")
    @Operation(summary = "Fotografije in podpis",
            description = "Aplikacija datoteke shrani na disk in tu sporoči njihove poti. Podpis nalog zaklene.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Priponke shranjene"),
            @ApiResponse(responseCode = "404", description = "Nalog s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> addAttachments(@PathVariable String workOrderId,
            @Valid @RequestBody WorkOrderAttachmentsRequest request) {
        workOrderService.addAttachments(workOrderId, WorkOrderDtoConverter.toAttachments(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workOrderId}/sent")
    @Operation(summary = "Nalog poslan po e-pošti", description = "Nastavi čas zadnjega pošiljanja na zdaj.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Zabeleženo"),
            @ApiResponse(responseCode = "404", description = "Nalog s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> markSent(@PathVariable String workOrderId) {
        workOrderService.markSent(workOrderId);
        return ResponseEntity.noContent().build();
    }

}
