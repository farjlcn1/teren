import "server-only";
import type { CurrentUser } from "@/lib/auth/session";
import type { WorkOrderSearch } from "@/lib/api/work-orders";

export type WorkOrderFilters = {
  from?: string;
  to?: string;
  clientId?: string;
  vehiclePlate?: string;
  imei?: string;
  type?: string;
  difficulty?: string;
  installer?: string;
  option?: string;
  deviceModel?: string;
  culprit?: string;
  sort?: string;
  dir?: string;
};

// Datum iz filtra (YYYY-MM-DD) je dan po lokalnem času strežnika (TZ=Europe/Ljubljana); neveljaven datum ne omejuje.
function localDateTime(date: string | undefined, time: string): Date | undefined {
  if (!date) return undefined;
  const value = new Date(`${date}T${time}`);
  return Number.isNaN(value.getTime()) ? undefined : value;
}

// Filtri s strani /nalogi v poizvedbo za API. Uporabnik brez pregleda vseh nalogov vidi samo svoje.
export function buildWorkOrderSearch(filters: WorkOrderFilters, user: CurrentUser): WorkOrderSearch {
  return {
    from: localDateTime(filters.from, "00:00:00"),
    to: localDateTime(filters.to, "23:59:59"),
    clientId: filters.clientId,
    vehiclePlate: filters.vehiclePlate,
    imei: filters.imei,
    type: filters.type,
    difficulty: filters.difficulty,
    installer: filters.installer,
    option: filters.option,
    deviceModel: filters.deviceModel,
    culprit: filters.culprit,
    createdById: user.canViewAllOrders ? undefined : user.id,
    sort: filters.sort,
    dir: filters.dir,
  };
}
