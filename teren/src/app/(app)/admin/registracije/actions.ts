"use server";

import { z } from "zod";
import ExcelJS from "exceljs";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";

function normalizePlate(plate: string): string {
  return plate.trim().toUpperCase();
}

const vehicleSchema = z.object({
  plate: z.string().min(1, "Registrska št. je obvezna").transform(normalizePlate),
});

export type ActionState = { error?: string; success?: string } | undefined;

export async function createVehicle(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const parsed = vehicleSchema.safeParse({ plate: formData.get("plate") });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  const existing = await prisma.vehicle.findUnique({ where: { plate: parsed.data.plate } });
  if (existing) {
    return { error: "Ta registrska št. že obstaja." };
  }

  await prisma.vehicle.create({ data: { plate: parsed.data.plate, source: "MANUAL" } });
  revalidatePath("/admin/registracije");
  return { success: "Registrska št. dodana." };
}

export async function updateVehicle(id: string, _prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const parsed = vehicleSchema.safeParse({ plate: formData.get("plate") });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  await prisma.vehicle.update({ where: { id }, data: { plate: parsed.data.plate } });
  revalidatePath("/admin/registracije");
  redirect("/admin/registracije");
}

export async function deleteVehicle(id: string) {
  await requirePermission("canManageVehicles");

  try {
    await prisma.vehicle.delete({ where: { id } });
  } catch {
    throw new Error("Registrske št. ni mogoče izbrisati.");
  }
  revalidatePath("/admin/registracije");
}

const importRowSchema = z.object({
  plate: z.string().min(1).transform(normalizePlate),
});

function normalizeHeader(header: string): string {
  return header.trim().toLowerCase();
}

export async function importVehiclesXlsx(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { error: "Izberi .xlsx datoteko." };
  }

  const buffer = Buffer.from(await file.arrayBuffer());
  const workbook = new ExcelJS.Workbook();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- exceljs's Buffer type is incompatible with TS 5.9's generic Buffer; value is a valid Buffer at runtime.
  await workbook.xlsx.load(buffer as any);
  const sheet = workbook.worksheets[0];
  if (!sheet) {
    return { error: "Datoteka ne vsebuje delovnega lista." };
  }

  const headerRow = sheet.getRow(1);
  const columnMap: Record<number, "plate"> = {};
  headerRow.eachCell((cell, colNumber) => {
    const value = normalizeHeader(String(cell.value ?? ""));
    if (["registrska", "registrska št.", "registrska st", "plate", "reg"].includes(value)) {
      columnMap[colNumber] = "plate";
    }
  });

  if (!Object.values(columnMap).includes("plate")) {
    return { error: 'Datoteka mora imeti stolpec "Registrska" (ali "Plate").' };
  }

  let created = 0;
  let skipped = 0;

  for (let rowNumber = 2; rowNumber <= sheet.rowCount; rowNumber++) {
    const row = sheet.getRow(rowNumber);
    if (row.cellCount === 0) continue;

    const raw: Record<string, string> = {};
    row.eachCell((cell, colNumber) => {
      const field = columnMap[colNumber];
      if (field) raw[field] = String(cell.value ?? "").trim();
    });

    const parsed = importRowSchema.safeParse({ plate: raw.plate });
    if (!parsed.success || !parsed.data.plate) {
      skipped++;
      continue;
    }

    const existing = await prisma.vehicle.findUnique({ where: { plate: parsed.data.plate } });
    if (existing) {
      skipped++;
      continue;
    }

    await prisma.vehicle.create({ data: { plate: parsed.data.plate, source: "IMPORT" } });
    created++;
  }

  revalidatePath("/admin/registracije");
  return { success: `Uvoženih ${created} registrskih št.${skipped ? `, preskočenih ${skipped} vrstic` : ""}.` };
}

const syncConfigSchema = z.object({
  apiUrl: z.string().url("Vnesi veljaven URL").or(z.literal("")),
  apiToken: z.string().optional(),
});

export async function saveSyncConfig(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const parsed = syncConfigSchema.safeParse({
    apiUrl: formData.get("apiUrl") ?? "",
    apiToken: formData.get("apiToken") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  await prisma.vehicleSyncConfig.upsert({
    where: { id: "default" },
    create: { id: "default", apiUrl: parsed.data.apiUrl || null, apiToken: parsed.data.apiToken || null },
    update: { apiUrl: parsed.data.apiUrl || null, apiToken: parsed.data.apiToken || null },
  });

  revalidatePath("/admin/registracije");
  return { success: "Nastavitve sinhronizacije shranjene." };
}

const syncItemSchema = z.object({
  externalId: z.string().min(1),
  plate: z.string().min(1).transform(normalizePlate),
});

export async function runSyncNow(): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const config = await prisma.vehicleSyncConfig.findUnique({ where: { id: "default" } });
  if (!config?.apiUrl) {
    return { error: "Zunanja povezava še ni nastavljena." };
  }

  try {
    const res = await fetch(config.apiUrl, {
      headers: config.apiToken ? { Authorization: `Bearer ${config.apiToken}` } : undefined,
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const data = await res.json();
    const parsed = z.array(syncItemSchema).parse(data);

    for (const item of parsed) {
      await prisma.vehicle.upsert({
        where: { externalId: item.externalId },
        create: { plate: item.plate, externalId: item.externalId, source: "SYNC" },
        update: { plate: item.plate },
      });
    }

    await prisma.vehicleSyncConfig.update({
      where: { id: "default" },
      data: { lastSyncAt: new Date(), lastSyncStatus: `V redu — ${parsed.length} registrskih št.` },
    });

    revalidatePath("/admin/registracije");
    return { success: `Sinhroniziranih ${parsed.length} registrskih št.` };
  } catch (e) {
    const message = e instanceof Error ? e.message : "neznana napaka";
    await prisma.vehicleSyncConfig
      .update({ where: { id: "default" }, data: { lastSyncAt: new Date(), lastSyncStatus: `Napaka: ${message}` } })
      .catch(() => undefined);
    return { error: `Sinhronizacija ni uspela: ${message}` };
  }
}
