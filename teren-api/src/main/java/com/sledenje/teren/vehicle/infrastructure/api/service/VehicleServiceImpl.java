package com.sledenje.teren.vehicle.infrastructure.api.service;

import com.sledenje.teren.vehicle.domain.exception.VehicleNotFoundException;
import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleImportResult;
import com.sledenje.teren.vehicle.domain.model.VehicleSource;
import com.sledenje.teren.vehicle.domain.repository.VehicleRepository;
import com.sledenje.teren.vehicle.domain.service.VehicleService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleServiceImpl.class);

    private final VehicleRepository vehicleRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAllVehicles() {
        return vehicleRepository.findAllVehiclesOrderedByPlate();
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle findVehicleById(String vehicleId) {
        return vehicleRepository.findVehicleById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    @Override
    @Transactional
    public Vehicle createVehicle(String plate) {
        String normalizedPlate = Vehicle.normalizePlate(plate);
        if (vehicleRepository.findVehicleByPlate(normalizedPlate).isPresent()) {
            throw new VehiclePlateAlreadyExistsException(normalizedPlate);
        }
        Vehicle createdVehicle = vehicleRepository.saveNewVehicle(normalizedPlate, VehicleSource.MANUAL);
        log.info("Dodano vozilo {} ({})", createdVehicle.id(), normalizedPlate);
        return createdVehicle;
    }

    @Override
    @Transactional
    public VehicleImportResult importVehicles(List<String> plates) {
        Set<String> platesInImport = new HashSet<>();
        int createdCount = 0;
        int skippedCount = 0;
        for (String plate : plates) {
            String normalizedPlate = Vehicle.normalizePlate(plate);
            boolean isNewPlate = platesInImport.add(normalizedPlate)
                    && vehicleRepository.findVehicleByPlate(normalizedPlate).isEmpty();
            if (isNewPlate) {
                vehicleRepository.saveNewVehicle(normalizedPlate, VehicleSource.IMPORT);
                createdCount++;
            } else {
                skippedCount++;
            }
        }
        log.info("Uvoženih {} vozil, preskočenih {}", createdCount, skippedCount);
        return new VehicleImportResult(createdCount, skippedCount);
    }

    @Override
    @Transactional
    public Vehicle updateVehiclePlate(String vehicleId, String plate) {
        String normalizedPlate = Vehicle.normalizePlate(plate);
        boolean plateTakenByOtherVehicle = vehicleRepository.findVehicleByPlate(normalizedPlate)
                .filter(vehicleWithPlate -> !vehicleWithPlate.id().equals(vehicleId))
                .isPresent();
        if (plateTakenByOtherVehicle) {
            throw new VehiclePlateAlreadyExistsException(normalizedPlate);
        }
        Vehicle updatedVehicle = vehicleRepository.updateVehiclePlate(vehicleId, normalizedPlate)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        log.info("Posodobljeno vozilo {} ({})", vehicleId, normalizedPlate);
        return updatedVehicle;
    }

    @Override
    @Transactional
    public void ensureVehicleExists(String plate) {
        String normalizedPlate = Vehicle.normalizePlate(plate);
        if (!normalizedPlate.isEmpty()) {
            vehicleRepository.insertVehicleIfMissing(normalizedPlate, VehicleSource.MANUAL);
        }
    }

    @Override
    @Transactional
    public void deleteVehicle(String vehicleId) {
        if (!vehicleRepository.existsVehicleById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
        vehicleRepository.deleteVehicleById(vehicleId);
        log.info("Izbrisano vozilo {}", vehicleId);
    }

}
