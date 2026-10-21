package com.sledenje.teren.plan.domain.repository;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PlanGroupRepository {

    /**
     * @return ID novega dogodka
     */
    String savePlanGroup(NewPlanGroup planGroup, List<NewPlannedTask> tasks);

    /**
     * @return ID novega planiranega naloga
     */
    String savePlannedTask(String planGroupId, NewPlannedTask task);

    /**
     * @return dogodki, ki se vsaj delno prekrivajo z [{@code from}, {@code to}), po začetku
     */
    List<PlanGroup> findPlanGroupsOverlapping(Instant from, Instant to);

    Optional<PlanGroup> findPlanGroup(String planGroupId);

    Optional<PlannedTaskDetail> findPlannedTask(String plannedTaskId);

    boolean existsClient(String clientId);

    /**
     * Prvi (po začetku) dogodek monterja, ki se prekriva z oknom [{@code startAt}, {@code endAt}).
     *
     * @param matchOtherText ali se primerja tudi ime monterja (pri "Ostalo"); {@code null} se ujema samo z {@code null}
     * @param excludedIds    dogodki, ki se ne upoštevajo (npr. pravkar premaknjeni)
     */
    Optional<PlanGroupWindow> findFirstOverlappingGroupOfInstaller(InstallerName installer, boolean matchOtherText,
            String installerOtherText, Instant startAt, Instant endAt, Set<String> excludedIds);

    void updatePlanGroup(String planGroupId, NewPlanGroup planGroup);

    void deletePlanGroup(String planGroupId);

    void deletePlannedTask(String plannedTaskId);

    void updatePlanGroupWindow(String planGroupId, Instant startAt, Instant endAt);

    /**
     * Zadnji dogodek iste stranke in istega monterja, ki se konča ob {@code notEndingBefore} ali kasneje.
     * Monter "Ostalo" se primerja tudi po imenu ({@code installerOtherText}, lahko {@code null}).
     */
    Optional<PlanGroupWindow> findLatestGroupOfInstallerEndingAfter(String clientId, InstallerName installer,
            String installerOtherText, Instant notEndingBefore);

    /**
     * Planiranemu nalogu nastavi opravljen delovni nalog, če še ni opravljen in dogodek pripada stranki.
     *
     * @return {@code false}, če planirani nalog ne obstaja, je že opravljen ali pripada drugi stranki
     */
    boolean linkPlannedTaskToWorkOrder(String plannedTaskId, String workOrderId, String clientId);

}
