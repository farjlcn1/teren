package com.sledenje.teren.plan.domain.service;

import com.sledenje.teren.plan.domain.exception.InvalidPlanGroupException;
import com.sledenje.teren.plan.domain.exception.PlanGroupCompletedException;
import com.sledenje.teren.plan.domain.exception.PlanGroupNotFoundException;
import com.sledenje.teren.plan.domain.exception.PlannedTaskCompletedException;
import com.sledenje.teren.plan.domain.exception.PlannedTaskNotFoundException;
import com.sledenje.teren.plan.domain.model.CompletedWorkOrder;
import com.sledenje.teren.plan.domain.model.NewPlannedTask;
import com.sledenje.teren.plan.domain.model.PlanGroup;
import com.sledenje.teren.plan.domain.model.PlanGroupChange;
import com.sledenje.teren.plan.domain.model.PlannedTaskDetail;
import java.time.Instant;
import java.util.List;

/**
 * Plan: dogodki (obiski strank v časovnem oknu) s planiranimi nalogi.
 * <p>
 * Isti monter (pri "Ostalo" po imenu) nima dveh prekrivajočih se dogodkov: ob vnosu, urejanju ali premiku
 * dogodka se prekrivajoči dogodki tega monterja zamaknejo tik za njegov konec, nato po potrebi še naslednji.
 */
public interface PlanService {

    /**
     * @return dogodki, ki se vsaj delno prekrivajo z [{@code from}, {@code to}), po začetku
     */
    List<PlanGroup> findPlanGroups(Instant from, Instant to);

    /**
     * @throws PlannedTaskNotFoundException če planirani nalog ne obstaja
     */
    PlannedTaskDetail findPlannedTask(String plannedTaskId);

    /**
     * @param tasks planirani nalogi brez opravljenega naloga
     * @return ID novega dogodka
     * @throws InvalidPlanGroupException če stranka ne obstaja, konec ni po začetku ali manjka ime monterja "Ostalo"
     */
    String createPlanGroup(PlanGroupChange planGroup, List<NewPlannedTask> tasks, String createdById);

    /**
     * @throws PlanGroupNotFoundException če dogodek ne obstaja
     * @throws InvalidPlanGroupException  kot pri vnosu, ali manjka konec, ali se spreminja stranka dogodka z že
     *                                    opravljenim nalogom
     */
    void updatePlanGroup(String planGroupId, PlanGroupChange planGroup);

    /**
     * Premik dogodka po koledarju (samo čas).
     *
     * @throws PlanGroupNotFoundException če dogodek ne obstaja
     * @throws InvalidPlanGroupException  če konec ni po začetku
     */
    void movePlanGroup(String planGroupId, Instant startAt, Instant endAt);

    /**
     * @throws PlanGroupNotFoundException  če dogodek ne obstaja
     * @throws PlanGroupCompletedException če vsebuje že opravljen nalog
     */
    void deletePlanGroup(String planGroupId);

    /**
     * @return ID novega planiranega naloga
     * @throws PlanGroupNotFoundException če dogodek ne obstaja
     * @throws InvalidPlanGroupException  če manjka registrska številka
     */
    String addPlannedTask(String planGroupId, NewPlannedTask task);

    /**
     * @throws PlannedTaskNotFoundException  če planirani nalog ne obstaja
     * @throws PlannedTaskCompletedException če je že opravljen
     */
    void deletePlannedTask(String plannedTaskId);

    /**
     * Zabeleži opravljen nalog v planu, da je vse delo vidno na zavihku Plan.
     * <ul>
     *     <li>Nalog iz planiranega naloga: planirani nalog se označi kot opravljen (če je že opravljen ali pripada
     *     drugi stranki, se ne zgodi nič; nalog ostane veljaven).</li>
     *     <li>Nalog brez plana: doda se k zadnjemu dogodku iste stranke in istega monterja, ki še traja, in ga po
     *     potrebi podaljša na uro po nalogu; sicer nastane nov enourni dogodek z že opravljenim nalogom.</li>
     * </ul>
     * Kliče se v transakciji vnosa naloga.
     */
    void recordCompletedWorkOrder(CompletedWorkOrder completedWorkOrder);

}
