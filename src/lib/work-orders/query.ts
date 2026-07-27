import "server-only";
import type { Prisma, WorkOrderType, Difficulty, InstallerName, OptionType } from "@/generated/prisma/client";
import type { CurrentUser } from "@/lib/auth/session";

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
  sort?: string;
  dir?: string;
};

type SortDir = "asc" | "desc";

const SORT_FIELD_MAP: Record<string, (dir: SortDir) => Prisma.WorkOrderOrderByWithRelationInput> = {
  orderDate: (dir) => ({ orderDate: dir }),
  ident: (dir) => ({ ident: dir }),
  vehiclePlate: (dir) => ({ vehiclePlate: dir }),
  imei: (dir) => ({ imei: dir }),
  client: (dir) => ({ client: { name: dir } }),
  type: (dir) => ({ type: dir }),
};

export function buildWorkOrderQuery(
  filters: WorkOrderFilters,
  user: CurrentUser
): { where: Prisma.WorkOrderWhereInput; orderBy: Prisma.WorkOrderOrderByWithRelationInput } {
  const where: Prisma.WorkOrderWhereInput = {};

  if (!user.canViewAllOrders) {
    where.createdById = user.id;
  }

  if (filters.from || filters.to) {
    where.orderDate = {};
    if (filters.from) where.orderDate.gte = new Date(`${filters.from}T00:00:00`);
    if (filters.to) where.orderDate.lte = new Date(`${filters.to}T23:59:59`);
  }

  if (filters.clientId) where.clientId = filters.clientId;
  if (filters.vehiclePlate) where.vehiclePlate = { contains: filters.vehiclePlate, mode: "insensitive" };
  if (filters.imei) where.imei = { contains: filters.imei };
  if (filters.type) where.type = filters.type as WorkOrderType;
  if (filters.difficulty) where.difficulty = filters.difficulty as Difficulty;
  if (filters.installer) {
    where.installers = { some: { name: filters.installer as InstallerName } };
  }
  if (filters.option) {
    where.options = { some: { optionType: filters.option as OptionType } };
  }

  const dir: SortDir = filters.dir === "asc" ? "asc" : "desc";
  const sortField = filters.sort && SORT_FIELD_MAP[filters.sort] ? filters.sort : "orderDate";
  const orderBy = SORT_FIELD_MAP[sortField](dir);

  return { where, orderBy };
}
