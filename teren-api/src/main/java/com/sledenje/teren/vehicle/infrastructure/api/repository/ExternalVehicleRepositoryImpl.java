package com.sledenje.teren.vehicle.infrastructure.api.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sledenje.teren.common.infrastructure.external.ExternalFetchException;
import com.sledenje.teren.common.infrastructure.external.ExternalListFetcher;
import com.sledenje.teren.vehicle.domain.exception.VehicleSyncFailedException;
import com.sledenje.teren.vehicle.domain.model.SyncedVehicle;
import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.repository.ExternalVehicleRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Generična povezava: GET na nastavljeni URL vrne JSON seznam objektov s polji {@code externalId} in {@code plate}.
 */
@Repository
public class ExternalVehicleRepositoryImpl implements ExternalVehicleRepository {

    private static final String INVALID_RESPONSE = "zunanja povezava je vrnila neveljaven seznam registrskih št.";

    private final ExternalListFetcher externalListFetcher;

    public ExternalVehicleRepositoryImpl(ExternalListFetcher externalListFetcher) {
        this.externalListFetcher = externalListFetcher;
    }

    @Override
    public List<SyncedVehicle> fetchVehicles(String apiUrl, String apiToken) {
        List<ExternalVehicle> externalVehicles;
        try {
            externalVehicles = externalListFetcher.fetchList(apiUrl, apiToken, ExternalVehicle[].class);
        } catch (ExternalFetchException exception) {
            throw new VehicleSyncFailedException(exception.getReason());
        }
        return externalVehicles.stream().map(ExternalVehicleRepositoryImpl::toSyncedVehicle).toList();
    }

    private static SyncedVehicle toSyncedVehicle(ExternalVehicle externalVehicle) {
        if (externalVehicle == null || isBlank(externalVehicle.externalId()) || isBlank(externalVehicle.plate())) {
            throw new VehicleSyncFailedException(INVALID_RESPONSE);
        }
        return new SyncedVehicle(externalVehicle.externalId().trim(), Vehicle.normalizePlate(externalVehicle.plate()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExternalVehicle(String externalId, String plate) {
    }

}
