import "server-only";
import { ApiError, apiRequest, pathSegment } from "./http";

export type ClientSource = "MANUAL" | "IMPORT" | "SYNC";

export type Client = {
  id: string;
  name: string;
  address: string | null;
  contactInfo: string | null;
  source: ClientSource;
  externalId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ClientOption = { id: string; name: string };

// Prazna neobvezna polja API shrani kot null (pri urejanju jih tudi izbriše).
export type ClientInput = {
  name: string;
  address?: string;
  contactInfo?: string;
};

export type ClientSyncConfig = {
  apiUrl: string | null;
  hasToken: boolean;
  lastSyncAt: string | null;
  lastSyncStatus: string | null;
};

const BASE = "/api/v1/clients";

export function listClients(): Promise<Client[]> {
  return apiRequest<Client[]>(BASE);
}

// Za izbirnike strank: v brskalnik gre samo ID in ime.
export async function listClientOptions(): Promise<ClientOption[]> {
  const clients = await listClients();
  return clients.map(({ id, name }) => ({ id, name }));
}

export async function findClient(id: string): Promise<Client | null> {
  try {
    return await apiRequest<Client>(`${BASE}/${pathSegment(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

export function createClient(input: ClientInput): Promise<Client> {
  return apiRequest<Client>(BASE, { method: "POST", body: input });
}

export function updateClient(id: string, input: ClientInput): Promise<Client> {
  return apiRequest<Client>(`${BASE}/${pathSegment(id)}`, { method: "PUT", body: input });
}

export function deleteClient(id: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${pathSegment(id)}`, { method: "DELETE" });
}

// Shranijo se vse stranke ali nobena.
export function importClients(clients: ClientInput[]): Promise<{ createdCount: number }> {
  return apiRequest<{ createdCount: number }>(`${BASE}/import`, { method: "POST", body: { clients } });
}

export function getClientSyncConfig(): Promise<ClientSyncConfig> {
  return apiRequest<ClientSyncConfig>(`${BASE}/sync-config`);
}

// Prazen apiToken obdrži obstoječi žeton, prazen apiUrl izbriše povezavo skupaj z žetonom.
export function saveClientSyncConfig(config: { apiUrl: string; apiToken?: string }): Promise<ClientSyncConfig> {
  return apiRequest<ClientSyncConfig>(`${BASE}/sync-config`, { method: "PUT", body: config });
}

export function syncClients(): Promise<{ syncedCount: number }> {
  return apiRequest<{ syncedCount: number }>(`${BASE}/sync`, { method: "POST" });
}
