package com.sledenje.teren.plan.domain.repository;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.plan.domain.model.NewPlanGroup;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroupWindow;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlanGroupRepository {

    /**
     * @return ID novega dogodka
     */
    String savePlanGroup(NewPlanGroup planGroup, List<NewPlannedTask> tasks);

    void savePlannedTask(String planGroupId, NewPlannedTask task);

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
