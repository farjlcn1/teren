import "server-only";
import { ApiError, apiRequest, pathSegment } from "./http";

export type VehicleSource = "MANUAL" | "IMPORT" | "SYNC";

// API registrsko številko normalizira (brez presledkov na robu, velike črke) in preveri, da je unikatna.
export type Vehicle = {
  id: string;
  plate: string;
  source: VehicleSource;
  externalId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type VehicleOption = { plate: string };

export type VehicleSyncConfig = {
  apiUrl: string | null;
  hasToken: boolean;
  lastSyncAt: string | null;
  lastSyncStatus: string | null;
};

const BASE = "/api/v1/vehicles";

export function listVehicles(): Promise<Vehicle[]> {
  return apiRequest<Vehicle[]>(BASE);
}

// Za izbirnike registrskih številk: v brskalnik gre samo registrska številka.
export async function listVehicleOptions(): Promise<VehicleOption[]> {
  const vehicles = await listVehicles();
  return vehicles.map(({ plate }) => ({ plate }));
}

export async function findVehicle(id: string): Promise<Vehicle | null> {
  try {
    return await apiRequest<Vehicle>(`${BASE}/${pathSegment(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

export function createVehicle(plate: string): Promise<Vehicle> {
  return apiRequest<Vehicle>(BASE, { method: "POST", body: { plate } });
}

export function updateVehicle(id: string, plate: string): Promise<Vehicle> {
  return apiRequest<Vehicle>(`${BASE}/${pathSegment(id)}`, { method: "PUT", body: { plate } });
}

export function deleteVehicle(id: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${pathSegment(id)}`, { method: "DELETE" });
}

// Obstoječe in v uvozu ponovljene registrske številke API preskoči.
export function importVehicles(plates: string[]): Promise<{ createdCount: number; skippedCount: number }> {
  return apiRequest<{ createdCount: number; skippedCount: number }>(`${BASE}/import`, {
    method: "POST",
    body: { plates },
  });
}

export function getVehicleSyncConfig(): Promise<VehicleSyncConfig> {
  return apiRequest<VehicleSyncConfig>(`${BASE}/sync-config`);
}

// Prazen apiToken obdrži obstoječi žeton, prazen apiUrl izbriše povezavo skupaj z žetonom.
export function saveVehicleSyncConfig(config: { apiUrl: string; apiToken?: string }): Promise<VehicleSyncConfig> {
  return apiRequest<VehicleSyncConfig>(`${BASE}/sync-config`, { method: "PUT", body: config });
}

export function syncVehicles(): Promise<{ syncedCount: number }> {
  return apiRequest<{ syncedCount: number }>(`${BASE}/sync`, { method: "POST" });
}
