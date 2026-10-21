package com.sledenje.teren.vehicle.domain.repository;

import com.sledenje.teren.vehicle.domain.exception.VehicleSyncFailedException;
import com.sledenje.teren.vehicle.domain.model.SyncedVehicle;
import java.util.List;

/**
 * Vozila iz zunanjega sistema.
 */
public interface ExternalVehicleRepository {

    /**
     * @param apiToken Bearer žeton ali {@code null}, če ga zunanji sistem ne zahteva
     * @throws VehicleSyncFailedException če zunanji sistem ni dosegljiv ali vrne neveljaven odgovor
     */
    List<SyncedVehicle> fetchVehicles(String apiUrl, String apiToken);

}
