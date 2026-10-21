package com.sledenje.teren.workorder.domain.model;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.common.domain.model.WorkOrderType;
import com.sledenje.teren.workorder.domain.exception.InvalidWorkOrderException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pravila za vsebino naloga (enaka kot v obrazcih aplikacije Teren, z enakimi sporočili).
 */
public final class WorkOrderRules {

    private static final Pattern IMEI = Pattern.compile("\\d{10}");
    private static final int MIN_VEHICLE_YEAR = 1950;

    private WorkOrderRules() {
    }

    /**
     * @param currentYear tekoče leto; letnik vozila je lahko največ eno leto naprej
     * @throws InvalidWorkOrderException ob prvi kršitvi
     */
    public static void validate(WorkOrderDetails details, List<DeviceModelEntry> deviceModels, int currentYear) {
        if (details.vehicleYear() < MIN_VEHICLE_YEAR || details.vehicleYear() > currentYear + 1) {
            throw new InvalidWorkOrderException("Neveljavno leto.");
        }
        if (!IMEI.matcher(details.imei()).matches()
                || (details.imeiPrev() != null && !IMEI.matcher(details.imeiPrev()).matches())) {
            throw new InvalidWorkOrderException("IMEI mora vsebovati natanko zadnjih 10 številk.");
        }
        if (details.installers().isEmpty()) {
            throw new InvalidWorkOrderException("Izberi vsaj enega monterja.");
        }
        if (details.type() == WorkOrderType.INTERVENCIJA && details.imeiPrev() == null) {
            throw new InvalidWorkOrderException("Pri intervenciji je polje 'IMEI prej' obvezno.");
        }
        if (details.type() == WorkOrderType.INTERVENCIJA && details.culprit() == null) {
            throw new InvalidWorkOrderException("Pri intervenciji je polje 'Krivec' obvezno.");
        }
        for (InstallerEntry installer : details.installers()) {
            if (installer.name() == InstallerName.OSTALO && installer.otherText() == null) {
                throw new InvalidWorkOrderException("Vnesi ime monterja pri izbiri 'Ostalo'.");
            }
        }
        for (DeviceModelEntry deviceModel : deviceModels) {
            if (deviceModel.deviceModel() == DeviceModel.OSTALO && deviceModel.comment() == null) {
                throw new InvalidWorkOrderException("Vnesi napravo pri izbiri 'Drugo'.");
            }
        }
        // Vsak monter, opcija in model naprave je lahko na nalogu samo enkrat (tako kot v obrazcu).
        requireDistinct(details.installers().stream().map(InstallerEntry::name).toList(), "Monter");
        requireDistinct(details.options().stream().map(OptionEntry::optionType).toList(), "Opcija");
        requireDistinct(deviceModels.stream().map(DeviceModelEntry::deviceModel).toList(), "Model naprave");
    }

    private static void requireDistinct(List<? extends Enum<?>> values, String label) {
        Set<Enum<?>> seen = new HashSet<>();
        for (Enum<?> value : values) {
            if (!seen.add(value)) {
                throw new InvalidWorkOrderException(label + " " + value.name() + " je naveden večkrat.");
            }
        }
    }

}
