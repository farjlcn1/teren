package com.sledenje.teren.plan.infrastructure.util;

import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlanGroupChange;
import com.sledenje.teren.plan.domain.model.PlannedTask;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import com.sledenje.teren.plan.presentation.dto.CreatePlanGroupRequest;
import com.sledenje.teren.plan.presentation.dto.PlanGroupDto;
import com.sledenje.teren.plan.presentation.dto.PlannedTaskDetailDto;
import com.sledenje.teren.plan.presentation.dto.PlannedTaskDto;
import com.sledenje.teren.plan.presentation.dto.PlannedTaskRequest;
import com.sledenje.teren.plan.presentation.dto.UpdatePlanGroupRequest;
import java.util.List;

/**
 * Pretvorbe med DTO-ji presentation nivoja in domenskim modelom plana.
 */
public final class PlanDtoConverter {

    private PlanDtoConverter() {
    }

    public static PlanGroupDto toDto(PlanGroup planGroup) {
        return new PlanGroupDto(
                planGroup.id(),
                planGroup.clientId(),
                planGroup.clientName(),
                planGroup.startAt(),
                planGroup.endAt(),
                planGroup.note(),
                planGroup.contact(),
                planGroup.expectedInstaller(),
                planGroup.expectedInstallerOtherText(),
                planGroup.createdById(),
                planGroup.tasks().stream().map(PlanDtoConverter::toDto).toList());
    }

    public static PlannedTaskDto toDto(PlannedTask task) {
        return new PlannedTaskDto(task.id(), task.vehiclePlate(), task.type(), task.note(), task.workOrderId());
    }

    public static PlannedTaskDetailDto toDto(PlannedTaskDetail detail) {
        PlannedTask task = detail.task();
        return new PlannedTaskDetailDto(task.id(), task.vehiclePlate(), task.type(), task.note(), task.workOrderId(),
                detail.planGroupId(), detail.clientId(), detail.clientName());
    }

    public static PlanGroupChange toChange(CreatePlanGroupRequest request) {
        return new PlanGroupChange(request.clientId(), request.startAt(), request.endAt(), request.note(),
                request.contact(), request.expectedInstaller(), request.expectedInstallerOtherText());
    }

    public static PlanGroupChange toChange(UpdatePlanGroupRequest request) {
        return new PlanGroupChange(request.clientId(), request.startAt(), request.endAt(), request.note(),
                request.contact(), request.expectedInstaller(), request.expectedInstallerOtherText());
    }

    public static List<NewPlannedTask> toNewPlannedTasks(List<PlannedTaskRequest> requests) {
        return requests.stream().map(PlanDtoConverter::toNewPlannedTask).toList();
    }

    /**
     * Prazna opomba je {@code null}; nov planirani nalog še ni opravljen.
     */
    public static NewPlannedTask toNewPlannedTask(PlannedTaskRequest request) {
        String note = request.note() == null || request.note().isBlank() ? null : request.note();
        return new NewPlannedTask(request.vehiclePlate(), request.type(), note, null);
    }

}
