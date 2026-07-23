import { Prisma, WorkOrderType, Difficulty, InstallerName, OptionType } from "@prisma/client";
import type { SafeUser } from "./session";

export function buildWorkOrderWhere(params: URLSearchParams, user: SafeUser): Prisma.WorkOrderWhereInput {
  const where: Prisma.WorkOrderWhereInput = {};

  if (!user.canViewAllOrders) {
    where.createdById = user.id;
  }

  const dateFrom = params.get("dateFrom");
  const dateTo = params.get("dateTo");
  if (dateFrom || dateTo) {
    where.orderDate = {
      ...(dateFrom ? { gte: new Date(dateFrom) } : {}),
      ...(dateTo ? { lte: new Date(dateTo) } : {}),
    };
  }

  const clientId = params.get("clientId");
  if (clientId) where.clientId = clientId;

  const plate = params.get("plate");
  if (plate) where.vehiclePlate = { contains: plate, mode: "insensitive" };

  const imei = params.get("imei");
  if (imei) where.imei = { contains: imei };

  const type = params.get("type");
  if (type) where.type = type as WorkOrderType;

  const difficulty = params.get("difficulty");
  if (difficulty) where.difficulty = difficulty as Difficulty;

  const installer = params.get("installer");
  if (installer) where.installers = { some: { name: installer as InstallerName } };

  const optionType = params.get("optionType");
  if (optionType) where.options = { some: { optionType: optionType as OptionType } };

  return where;
}

export const SORTABLE_FIELDS = new Set(["orderDate", "ident", "vehiclePlate", "imei", "type", "difficulty"]);
