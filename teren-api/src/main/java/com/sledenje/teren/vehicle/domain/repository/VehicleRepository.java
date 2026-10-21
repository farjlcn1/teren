package com.sledenje.teren.vehicle.domain.repository;

import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.model.SyncedVehicle;
import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleSource;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    List<Vehicle> findAllVehiclesOrderedByPlate();

    Optional<Vehicle> findVehicleById(String vehicleId);

    Optional<Vehicle> findVehicleByPlate(String plate);

    boolean existsVehicleById(String vehicleId);

    Vehicle saveNewVehicle(String plate, VehicleSource source);

    /**
     * @return posodobljeno vozilo ali prazno, če vozilo s tem ID-jem ne obstaja
     */
    Optional<Vehicle> updateVehiclePlate(String vehicleId, String plate);

    void deleteVehicleById(String vehicleId);

    /**
     * Doda vozilo, če registrske številke še ni. Ne vrže napake, če jo hkrati doda nekdo drug.
     */
    void insertVehicleIfMissing(String plate, VehicleSource source);

    /**
     * Vozila z znanim {@code externalId} posodobi, ostala doda z izvorom {@code SYNC}. Vse v eni transakciji.
     *
     * @throws VehiclePlateAlreadyExistsException če ima registrsko številko že drugo vozilo; nič se ne shrani
     */
    void upsertSyncedVehicles(List<SyncedVehicle> syncedVehicles);

}
