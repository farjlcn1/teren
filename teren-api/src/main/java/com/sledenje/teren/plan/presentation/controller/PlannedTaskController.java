package com.sledenje.teren.plan.presentation.controller;

import com.sledenje.teren.plan.domain.service.PlanService;
import com.sledenje.teren.plan.infrastructure.util.PlanDtoConverter;
import com.sledenje.teren.plan.presentation.dto.PlannedTaskDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planned-tasks")
@Tag(name = "Plan", description = "Dogodki v planu in njihovi planirani nalogi")
public class PlannedTaskController {

    private final PlanService planService;

    public PlannedTaskController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/{plannedTaskId}")
    @Operation(summary = "Planiran nalog", description = "Z dogodkom in stranko (za obrazec novega naloga).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planiran nalog najden"),
            @ApiResponse(responseCode = "404", description = "Planiran nalog ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PlannedTaskDetailDto> getPlannedTask(@PathVariable String plannedTaskId) {
        return ResponseEntity.ok(PlanDtoConverter.toDto(planService.findPlannedTask(plannedTaskId)));
    }

    @DeleteMapping("/{plannedTaskId}")
    @Operation(summary = "Brisanje planiranega naloga")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Planiran nalog izbrisan"),
            @ApiResponse(responseCode = "404", description = "Planiran nalog ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Planiran nalog je že opravljen",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deletePlannedTask(@PathVariable String plannedTaskId) {
        planService.deletePlannedTask(plannedTaskId);
        return ResponseEntity.noContent().build();
    }

}
