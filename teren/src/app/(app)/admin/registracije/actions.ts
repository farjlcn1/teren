"use server";

import { z } from "zod";
import ExcelJS from "exceljs";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { apiErrorMessage } from "@/lib/api/http";
import * as vehiclesApi from "@/lib/api/vehicles";

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

  try {
    await vehiclesApi.createVehicle(parsed.data.plate);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/registracije");
  return { success: "Registrska št. dodana." };
}

export async function updateVehicle(id: string, _prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  const parsed = vehicleSchema.safeParse({ plate: formData.get("plate") });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  try {
    await vehiclesApi.updateVehicle(id, parsed.data.plate);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/registracije");
  redirect("/admin/registracije");
}

// Vrne napako namesto da jo vrže: Next.js v produkciji sporočilo vržene napake skrije (glej deleteUser).
export async function deleteVehicle(id: string): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  try {
    await vehiclesApi.deleteVehicle(id);
  } catch (e) {
    return { error: apiErrorMessage(e) };
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

  const plates: string[] = [];
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

    plates.push(parsed.data.plate);
  }

  // API preskoči registrske številke, ki že obstajajo ali se v datoteki ponovijo.
  let created = 0;
  if (plates.length > 0) {
    try {
      const result = await vehiclesApi.importVehicles(plates);
      created = result.createdCount;
      skipped += result.skippedCount;
    } catch (e) {
      return { error: apiErrorMessage(e) };
    }
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

  // Prazen žeton obdrži obstoječega (obrazec ga ne prikaže), prazen URL izbriše povezavo skupaj z žetonom.
  try {
    await vehiclesApi.saveVehicleSyncConfig(parsed.data);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  revalidatePath("/admin/registracije");
  return { success: "Nastavitve sinhronizacije shranjene." };
}

// Sinhronizacijo izvede API: prenese registrske številke z nastavljenega URL-ja in zapiše izid.
export async function runSyncNow(): Promise<ActionState> {
  await requirePermission("canManageVehicles");

  try {
    const { syncedCount } = await vehiclesApi.syncVehicles();
    revalidatePath("/admin/registracije");
    return { success: `Sinhroniziranih ${syncedCount} registrskih št.` };
  } catch (e) {
    // Neuspeh je zapisan v nastavitvah, zato se osveži tudi prikaz zadnje sinhronizacije.
    revalidatePath("/admin/registracije");
    return { error: apiErrorMessage(e) };
  }
}
