package com.sledenje.teren.vehicle.domain.service;

import com.sledenje.teren.vehicle.domain.exception.VehicleNotFoundException;
import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleImportResult;
import java.util.List;

/**
 * Registrske številke se pred shranjevanjem normalizirajo ({@link Vehicle#normalizePlate(String)}).
 */
public interface VehicleService {

    /**
     * @return vsa vozila, urejena po registrski številki
     */
    List<Vehicle> findAllVehicles();

    /**
     * @throws VehicleNotFoundException če vozilo s tem ID-jem ne obstaja
     */
    Vehicle findVehicleById(String vehicleId);

    /**
     * Ročno doda vozilo (izvor {@code MANUAL}).
     *
     * @throws VehiclePlateAlreadyExistsException če registrska številka že obstaja
     */
    Vehicle createVehicle(String plate);

    /**
     * Doda vozila iz uvožene datoteke (izvor {@code IMPORT}). Obstoječe in ponovljene registrske številke preskoči.
     */
    VehicleImportResult importVehicles(List<String> plates);

    /**
     * @throws VehicleNotFoundException           če vozilo s tem ID-jem ne obstaja
     * @throws VehiclePlateAlreadyExistsException če ima registrsko številko že drugo vozilo
     */
    Vehicle updateVehiclePlate(String vehicleId, String plate);

    /**
     * Delovni nalogi in plan hranijo registrsko številko kot besedilo, zato brisanje nanje ne vpliva.
     *
     * @throws VehicleNotFoundException če vozilo s tem ID-jem ne obstaja
     */
    void deleteVehicle(String vehicleId);

}
