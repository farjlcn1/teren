package com.sledenje.teren.workorder.presentation.controller;

import com.sledenje.teren.workorder.domain.service.WorkOrderService;
import com.sledenje.teren.workorder.infrastructure.util.WorkOrderDtoConverter;
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
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-orders")
@Tag(name = "Delovni nalogi", description = "Pregled delovnih nalogov")
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

}
