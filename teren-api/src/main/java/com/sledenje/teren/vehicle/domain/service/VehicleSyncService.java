package com.sledenje.teren.vehicle.domain.service;

import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.exception.VehicleSyncFailedException;
import com.sledenje.teren.vehicle.domain.exception.VehicleSyncNotConfiguredException;
import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;

public interface VehicleSyncService {

    /**
     * @return trenutne nastavitve; {@link VehicleSyncConfig#NOT_CONFIGURED}, če še niso bile shranjene
     */
    VehicleSyncConfig findSyncConfig();

    /**
     * Shrani povezavo do zunanjega sistema. Prazen {@code apiToken} obdrži obstoječi žeton, če se URL ne spremeni
     * (ob novem URL-ju je brez žetona); prazen {@code apiUrl} izbriše povezavo skupaj z žetonom.
     */
    VehicleSyncConfig updateSyncConfig(String apiUrl, String apiToken);

    /**
     * Prenese vozila iz zunanjega sistema in izid zapiše v nastavitve. Shranijo se vsa vozila ali nobeno.
     *
     * @return število sinhroniziranih vozil
     * @throws VehicleSyncNotConfiguredException  če povezava ni nastavljena
     * @throws VehicleSyncFailedException         če zunanji sistem ni odgovoril pravilno
     * @throws VehiclePlateAlreadyExistsException če ima prenesena registrska številka že drugo vozilo
     */
    int syncVehicles();

}
