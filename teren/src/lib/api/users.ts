import "server-only";
import { ApiError, apiRequest, pathSegment } from "./http";

export type UserPermissions = {
  canManageUsers: boolean;
  canViewAllOrders: boolean;
  canExportData: boolean;
  canSendEmail: boolean;
  canEditOrders: boolean;
  canManageClients: boolean;
  canManageVehicles: boolean;
  canManagePlan: boolean;
};

// Zgoščeno geslo nikoli ne zapusti API-ja; gesla preverja in zgošči API.
export type User = UserPermissions & {
  id: string;
  email: string;
  fullName: string;
  isActive: boolean;
  // Seje, izdane pred tem časom, ne veljajo (nastavi se ob spremembi gesla).
  sessionsValidFrom: string | null;
  createdAt: string;
  updatedAt: string;
};

const BASE = "/api/v1/users";

export function listUsers(): Promise<User[]> {
  return apiRequest<User[]>(BASE);
}

export async function findUser(id: string): Promise<User | null> {
  try {
    return await apiRequest<User>(`${BASE}/${pathSegment(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

// Nov uporabnik je aktiven in nima pravic. API preveri pravila gesla in unikatnost emaila.
export function createUser(input: { email: string; fullName: string; password: string }): Promise<User> {
  return apiRequest<User>(BASE, { method: "POST", body: input });
}

// Vse pravice se pošljejo hkrati; manjkajoča pravica bi jo odvzela, zato jih API zahteva vse.
export function updateUserAccess(id: string, access: UserPermissions & { isActive: boolean }): Promise<User> {
  return apiRequest<User>(`${BASE}/${pathSegment(id)}/access`, { method: "PUT", body: access });
}

export function changeUserPassword(id: string, password: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${pathSegment(id)}/password`, { method: "PUT", body: { password } });
}

export function deleteUser(id: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${pathSegment(id)}`, { method: "DELETE" });
}

// null pri napačnem emailu ali geslu in pri onemogočenem uporabniku (API vrne 401 za vse tri primere).
export async function authenticate(email: string, password: string): Promise<User | null> {
  try {
    return await apiRequest<User>("/api/v1/auth/login", { method: "POST", body: { email, password } });
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) return null;
    throw e;
  }
}
