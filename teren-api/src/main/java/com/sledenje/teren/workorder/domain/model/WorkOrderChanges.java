package com.sledenje.teren.workorder.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Spremembe pri urejanju naloga, kot jih zapiše in prikaže zgodovina urejanj v aplikaciji Teren:
 * ključ je slovenska oznaka polja, monterji in opcije so primerjani kot urejen seznam "IME:besedilo".
 */
public final class WorkOrderChanges {

    private WorkOrderChanges() {
    }

    /**
     * @return spremenjena polja v vrstnem redu obrazca; prazno, če se ni nič spremenilo
     */
    public static Map<String, ValueChange> between(WorkOrder before, WorkOrderDetails after) {
        Map<String, ValueChange> changes = new LinkedHashMap<>();
        addIfChanged(changes, "Tip", before.type().name(), after.type().name());
        addIfChanged(changes, "Zahtevnost", before.difficulty().name(), after.difficulty().name());
        addIfChanged(changes, "Stranka", before.client().id(), after.clientId());
        addIfChanged(changes, "Registrska št.", before.vehiclePlate(), after.vehiclePlate());
        addIfChanged(changes, "Znamka", before.vehicleBrand(), after.vehicleBrand());
        addIfChanged(changes, "Model", before.vehicleModel(), after.vehicleModel());
        addIfChanged(changes, "Letnik", before.vehicleYear(), after.vehicleYear());
        addIfChanged(changes, "IMEI", before.imei(), after.imei());
        addIfChanged(changes, "IMEI prej", before.imeiPrev(), after.imeiPrev());
        addIfChanged(changes, "Krivec", enumName(before.culprit()), enumName(after.culprit()));
        addIfChanged(changes, "Komentar", before.comment(), after.comment());

        String beforeInstallers = joinSorted(before.installers().stream()
                .map(installer -> installer.name().name() + ":" + Objects.toString(installer.otherText(), ""))
                .toList());
        String afterInstallers = joinSorted(after.installers().stream()
                .map(installer -> installer.name().name() + ":" + Objects.toString(installer.otherText(), ""))
                .toList());
        if (!beforeInstallers.equals(afterInstallers)) {
            changes.put("Monterji", new ValueChange(beforeInstallers, afterInstallers));
        }

        String beforeOptions = joinSorted(before.options().stream()
                .map(option -> option.optionType().name() + ":" + Objects.toString(option.comment(), ""))
                .toList());
        String afterOptions = joinSorted(after.options().stream()
                .map(option -> option.optionType().name() + ":" + Objects.toString(option.comment(), ""))
                .toList());
        if (!beforeOptions.equals(afterOptions)) {
            changes.put("DIN/ANI/CAN", new ValueChange(beforeOptions, afterOptions));
        }
        return changes;
    }

    private static void addIfChanged(Map<String, ValueChange> changes, String label, Object from, Object to) {
        // Prazno in manjkajoče sta enako, kot v aplikaciji (String(v ?? "")).
        if (!Objects.toString(from, "").equals(Objects.toString(to, ""))) {
            changes.put(label, new ValueChange(from, to));
        }
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String joinSorted(List<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }

}
