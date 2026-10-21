package com.sledenje.teren.plan.presentation.controller;

import com.sledenje.teren.plan.domain.service.PlanService;
import com.sledenje.teren.plan.infrastructure.util.PlanDtoConverter;
import com.sledenje.teren.plan.presentation.dto.CreatePlanGroupRequest;
import com.sledenje.teren.plan.presentation.dto.CreatedIdDto;
import com.sledenje.teren.plan.presentation.dto.MovePlanGroupRequest;
import com.sledenje.teren.plan.presentation.dto.PlanGroupDto;
import com.sledenje.teren.plan.presentation.dto.PlannedTaskRequest;
import com.sledenje.teren.plan.presentation.dto.UpdatePlanGroupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/plan-groups")
@Tag(name = "Plan", description = "Dogodki v planu in njihovi planirani nalogi")
public class PlanGroupController {

    private final PlanService planService;

    public PlanGroupController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Dogodki v obdobju", description = "Dogodki, ki se vsaj delno prekrivajo z [from, to), po začetku.")
    public ResponseEntity<List<PlanGroupDto>> getPlanGroups(
            @Parameter(description = "Začetek obdobja, ISO 8601") @RequestParam Instant from,
            @Parameter(description = "Konec obdobja (izključno), ISO 8601") @RequestParam Instant to) {
        return ResponseEntity.ok(planService.findPlanGroups(from, to).stream().map(PlanDtoConverter::toDto).toList());
    }

    @PostMapping
    @Operation(summary = "Nov dogodek")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dogodek ustvarjen"),
            @ApiResponse(responseCode = "400", description = "Manjka stranka, začetek ali registrska številka",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Konec ni po začetku, manjka ime monterja ali stranka ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreatedIdDto> createPlanGroup(@Valid @RequestBody CreatePlanGroupRequest request) {
        String planGroupId = planService.createPlanGroup(PlanDtoConverter.toChange(request),
                PlanDtoConverter.toNewPlannedTasks(request.tasks()), request.createdById());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{planGroupId}")
                .buildAndExpand(planGroupId)
                .toUri();
        return ResponseEntity.created(location).body(new CreatedIdDto(planGroupId));
    }

    @PutMapping("/{planGroupId}")
    @Operation(summary = "Urejanje dogodka")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dogodek shranjen"),
            @ApiResponse(responseCode = "404", description = "Dogodek ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Neveljavni podatki ali sprememba stranke z opravljenim nalogom",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> updatePlanGroup(@PathVariable String planGroupId,
            @Valid @RequestBody UpdatePlanGroupRequest request) {
        planService.updatePlanGroup(planGroupId, PlanDtoConverter.toChange(request));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planGroupId}/window")
    @Operation(summary = "Premik dogodka", description = "Spremeni samo čas (povleci in spusti v koledarju).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dogodek premaknjen"),
            @ApiResponse(responseCode = "404", description = "Dogodek ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Konec ni po začetku",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> movePlanGroup(@PathVariable String planGroupId,
            @Valid @RequestBody MovePlanGroupRequest request) {
        planService.movePlanGroup(planGroupId, request.startAt(), request.endAt());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{planGroupId}")
    @Operation(summary = "Brisanje dogodka")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dogodek izbrisan"),
            @ApiResponse(responseCode = "404", description = "Dogodek ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Dogodek vsebuje že opravljen nalog",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deletePlanGroup(@PathVariable String planGroupId) {
        planService.deletePlanGroup(planGroupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planGroupId}/tasks")
    @Operation(summary = "Nov planiran nalog v dogodku")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Planiran nalog dodan"),
            @ApiResponse(responseCode = "400", description = "Manjka registrska številka",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Dogodek ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreatedIdDto> addPlannedTask(@PathVariable String planGroupId,
            @Valid @RequestBody PlannedTaskRequest request) {
        String plannedTaskId = planService.addPlannedTask(planGroupId, PlanDtoConverter.toNewPlannedTask(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/planned-tasks/{plannedTaskId}")
                .buildAndExpand(plannedTaskId)
                .toUri();
        return ResponseEntity.created(location).body(new CreatedIdDto(plannedTaskId));
    }

}
