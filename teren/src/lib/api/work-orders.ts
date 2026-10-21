import "server-only";
import { ApiError, apiRequest } from "./http";

// API vrača čase kot ISO nize (UTC); tu se pretvorijo v Date, da strani in PDF ostanejo nespremenjeni.

type WorkOrderJson = {
  id: string;
  ident: string;
  orderDate: string;
  type: string;
  difficulty: string;
  status: string;
  client: { id: string; name: string; address: string | null; contactInfo: string | null };
  vehiclePlate: string;
  vehicleBrand: string;
  vehicleModel: string;
  vehicleYear: number;
  imei: string;
  imeiPrev: string | null;
  culprit: string | null;
  comment: string | null;
  signatureUrl: string | null;
  signedAt: string | null;
  lockedAt: string | null;
  sentAt: string | null;
  createdBy: { id: string; fullName: string };
  createdAt: string;
  updatedAt: string;
  installers: { id: string; name: string; otherText: string | null }[];
  options: { id: string; optionType: string; comment: string | null }[];
  deviceModels: { id: string; deviceModel: string; comment: string | null }[];
};

type WorkOrderDetailJson = WorkOrderJson & {
  photos: { id: string; filePath: string; takenAt: string | null; uploadedAt: string }[];
  edits: { id: string; editedAt: string; editedBy: { id: string; fullName: string }; changesJson: string }[];
};

export type WorkOrder = Omit<WorkOrderJson, "orderDate" | "signedAt" | "lockedAt" | "sentAt" | "createdAt" | "updatedAt"> & {
  clientId: string;
  createdById: string;
  orderDate: Date;
  signedAt: Date | null;
  lockedAt: Date | null;
  sentAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
};

export type WorkOrderDetail = WorkOrder & {
  photos: { id: string; filePath: string; takenAt: Date | null; uploadedAt: Date }[];
  edits: { id: string; editedAt: Date; editedBy: { id: string; fullName: string }; changesJson: string }[];
};

// Prazna polja ne omejujejo. Kdo sme videti katere naloge, določi klicatelj (createdById).
export type WorkOrderSearch = {
  from?: Date;
  to?: Date;
  clientId?: string;
  vehiclePlate?: string;
  imei?: string;
  type?: string;
  difficulty?: string;
  installer?: string;
  option?: string;
  deviceModel?: string;
  culprit?: string;
  createdById?: string;
  sort?: string;
  dir?: string;
  limit?: number;
};

const BASE = "/api/v1/work-orders";

function toDate(value: string): Date;
function toDate(value: string | null): Date | null;
function toDate(value: string | null): Date | null {
  return value === null ? null : new Date(value);
}

function toWorkOrder(json: WorkOrderJson): WorkOrder {
  return {
    ...json,
    clientId: json.client.id,
    createdById: json.createdBy.id,
    orderDate: toDate(json.orderDate),
    signedAt: toDate(json.signedAt),
    lockedAt: toDate(json.lockedAt),
    sentAt: toDate(json.sentAt),
    createdAt: toDate(json.createdAt),
    updatedAt: toDate(json.updatedAt),
  };
}

function toWorkOrderDetail(json: WorkOrderDetailJson): WorkOrderDetail {
  return {
    ...toWorkOrder(json),
    photos: json.photos.map((p) => ({ ...p, takenAt: toDate(p.takenAt), uploadedAt: toDate(p.uploadedAt) })),
    edits: json.edits.map((e) => ({ ...e, editedAt: toDate(e.editedAt) })),
  };
}

function searchParams(search: WorkOrderSearch): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(search)) {
    if (value === undefined || value === null || value === "") continue;
    params.set(key, value instanceof Date ? value.toISOString() : String(value));
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function listWorkOrders(search: WorkOrderSearch): Promise<WorkOrder[]> {
  const orders = await apiRequest<WorkOrderJson[]>(`${BASE}${searchParams(search)}`);
  return orders.map(toWorkOrder);
}

// S fotografijami (po času nalaganja) in zgodovino urejanj (najnovejše prvo).
export async function findWorkOrder(id: string): Promise<WorkOrderDetail | null> {
  try {
    return toWorkOrderDetail(await apiRequest<WorkOrderDetailJson>(`${BASE}/${encodeURIComponent(id)}`));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

export function listWorkOrderImeis(): Promise<string[]> {
  return apiRequest<string[]>(`${BASE}/imeis`);
}
