import "server-only";
import { ApiError, apiRequest, pathSegment } from "./http";

export type PlannedTask = {
  id: string;
  vehiclePlate: string;
  type: string | null;
  note: string | null;
  // Opravljen delovni nalog; null, dokler planirani nalog ni opravljen.
  workOrderId: string | null;
};

// Časi so ISO nizi (UTC), kot jih pričakuje koledar.
export type PlanGroup = {
  id: string;
  clientId: string;
  clientName: string;
  startAt: string;
  endAt: string;
  note: string | null;
  contact: string | null;
  expectedInstaller: string | null;
  expectedInstallerOtherText: string | null;
  createdById: string;
  tasks: PlannedTask[];
};

export type PlannedTaskDetail = PlannedTask & { planGroupId: string; clientId: string; clientName: string };

// endAt je pri novem dogodku neobvezen (API privzame uro po začetku), pri urejanju obvezen.
export type PlanGroupInput = {
  clientId: string;
  startAt: Date;
  endAt: Date | null;
  note: string | null;
  contact: string | null;
  expectedInstaller: string | null;
  expectedInstallerOtherText: string | null;
};

export type PlannedTaskInput = { vehiclePlate: string; type: string | null; note: string | null };

const GROUPS = "/api/v1/plan-groups";
const TASKS = "/api/v1/planned-tasks";

// Dogodki, ki se vsaj delno prekrivajo z [from, to), po začetku; planirani nalogi po času vnosa.
export function listPlanGroups(from: Date, to: Date): Promise<PlanGroup[]> {
  const params = new URLSearchParams({ from: from.toISOString(), to: to.toISOString() });
  return apiRequest<PlanGroup[]>(`${GROUPS}?${params}`);
}

export async function findPlannedTask(id: string): Promise<PlannedTaskDetail | null> {
  try {
    return await apiRequest<PlannedTaskDetail>(`${TASKS}/${pathSegment(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

// API prekrivajoče se dogodke istega monterja zamakne za konec tega dogodka.
export function createPlanGroup(
  input: PlanGroupInput & { createdById: string; tasks: PlannedTaskInput[] }
): Promise<{ id: string }> {
  return apiRequest<{ id: string }>(GROUPS, { method: "POST", body: input });
}

export function updatePlanGroup(id: string, input: PlanGroupInput): Promise<void> {
  return apiRequest<void>(`${GROUPS}/${pathSegment(id)}`, { method: "PUT", body: input });
}

export function movePlanGroup(id: string, startAt: Date, endAt: Date): Promise<void> {
  return apiRequest<void>(`${GROUPS}/${pathSegment(id)}/window`, { method: "PUT", body: { startAt, endAt } });
}

export function deletePlanGroup(id: string): Promise<void> {
  return apiRequest<void>(`${GROUPS}/${pathSegment(id)}`, { method: "DELETE" });
}

export function addPlannedTask(planGroupId: string, task: PlannedTaskInput): Promise<{ id: string }> {
  return apiRequest<{ id: string }>(`${GROUPS}/${pathSegment(planGroupId)}/tasks`, { method: "POST", body: task });
}

export function deletePlannedTask(id: string): Promise<void> {
  return apiRequest<void>(`${TASKS}/${pathSegment(id)}`, { method: "DELETE" });
}
