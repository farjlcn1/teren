package com.sledenje.teren.vehicle.infrastructure.api.service;

import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.exception.VehicleSyncFailedException;
import com.sledenje.teren.vehicle.domain.exception.VehicleSyncNotConfiguredException;
import com.sledenje.teren.vehicle.domain.model.SyncedVehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;
import com.sledenje.teren.vehicle.domain.repository.ExternalVehicleRepository;
import com.sledenje.teren.vehicle.domain.repository.VehicleRepository;
import com.sledenje.teren.vehicle.domain.repository.VehicleSyncConfigRepository;
import com.sledenje.teren.vehicle.domain.service.VehicleSyncService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleSyncServiceImpl implements VehicleSyncService {

    private static final Logger log = LoggerFactory.getLogger(VehicleSyncServiceImpl.class);

    private final VehicleSyncConfigRepository vehicleSyncConfigRepository;
    private final ExternalVehicleRepository externalVehicleRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleSyncServiceImpl(VehicleSyncConfigRepository vehicleSyncConfigRepository,
            ExternalVehicleRepository externalVehicleRepository, VehicleRepository vehicleRepository) {
        this.vehicleSyncConfigRepository = vehicleSyncConfigRepository;
        this.externalVehicleRepository = externalVehicleRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleSyncConfig findSyncConfig() {
        return vehicleSyncConfigRepository.findSyncConfig().orElse(VehicleSyncConfig.NOT_CONFIGURED);
    }

    @Override
    @Transactional
    public VehicleSyncConfig updateSyncConfig(String apiUrl, String apiToken) {
        String newApiUrl = blankToNull(apiUrl);
        String newApiToken = blankToNull(apiToken);
        if (newApiUrl == null) {
            newApiToken = null;
        } else if (newApiToken == null && newApiUrl.equals(findSyncConfig().apiUrl())) {
            // Obrazec žetona ne prikaže; prazno polje obdrži obstoječega, a samo za isti URL. Ob novem URL-ju
            // bi sicer stari žeton odšel na drug strežnik (npr. napadalčev).
            newApiToken = findSyncConfig().apiToken();
        }
        VehicleSyncConfig savedConfig = vehicleSyncConfigRepository.saveConnection(newApiUrl, newApiToken);
        log.info("Shranjene nastavitve sinhronizacije vozil (povezava {})",
                savedConfig.isConfigured() ? "nastavljena" : "izbrisana");
        return savedConfig;
    }

    /**
     * Brez skupne transakcije: izid (tudi neuspeh) se mora zapisati, vozila pa se shranijo v svoji transakciji.
     */
    @Override
    public int syncVehicles() {
        VehicleSyncConfig syncConfig = findSyncConfig();
        if (!syncConfig.isConfigured()) {
            throw new VehicleSyncNotConfiguredException();
        }

        try {
            List<SyncedVehicle> syncedVehicles =
                    externalVehicleRepository.fetchVehicles(syncConfig.apiUrl(), syncConfig.apiToken());
            vehicleRepository.upsertSyncedVehicles(syncedVehicles);
            vehicleSyncConfigRepository.saveSyncResult(now(),
                    "V redu — " + syncedVehicles.size() + " registrskih št.");
            log.info("Sinhroniziranih {} vozil", syncedVehicles.size());
            return syncedVehicles.size();
        } catch (VehicleSyncFailedException exception) {
            vehicleSyncConfigRepository.saveSyncResult(now(), "Napaka: " + exception.getReason());
            log.warn("Sinhronizacija vozil ni uspela: {}", exception.getReason());
            throw exception;
        } catch (VehiclePlateAlreadyExistsException exception) {
            vehicleSyncConfigRepository.saveSyncResult(now(), "Napaka: " + exception.getMessage());
            log.warn("Sinhronizacija vozil ni uspela: {}", exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            vehicleSyncConfigRepository.saveSyncResult(now(), "Napaka: shranjevanje vozil ni uspelo");
            throw exception;
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
