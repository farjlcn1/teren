package com.sledenje.teren.plan.domain.service;

import com.sledenje.teren.plan.domain.model.CompletedWorkOrder;

public interface PlanService {

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
