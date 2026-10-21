"use server";

import { z } from "zod";
import ExcelJS from "exceljs";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { apiErrorMessage } from "@/lib/api/http";
import * as clientsApi from "@/lib/api/clients";

const clientSchema = z.object({
  name: z.string().min(1, "Ime je obvezno"),
  address: z.string().optional(),
  contactInfo: z.string().optional(),
});

export type ActionState = { error?: string; success?: string } | undefined;

export async function createClient(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageClients");

  const parsed = clientSchema.safeParse({
    name: formData.get("name"),
    address: formData.get("address") || undefined,
    contactInfo: formData.get("contactInfo") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  try {
    await clientsApi.createClient(parsed.data);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/stranke");
  return { success: "Stranka dodana." };
}

export async function updateClient(id: string, _prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageClients");

  const parsed = clientSchema.safeParse({
    name: formData.get("name"),
    address: formData.get("address") || undefined,
    contactInfo: formData.get("contactInfo") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  // Prazno polje izbriše obstoječo vrednost (API zamenja vse podatke stranke).
  try {
    await clientsApi.updateClient(id, parsed.data);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/stranke");
  redirect("/admin/stranke");
}

// Vrne napako namesto da jo vrže: Next.js v produkciji sporočilo vržene napake skrije (glej deleteUser).
export async function deleteClient(id: string): Promise<ActionState> {
  await requirePermission("canManageClients");

  try {
    await clientsApi.deleteClient(id);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/stranke");
}

const importRowSchema = z.object({
  name: z.string().min(1),
  address: z.string().optional(),
  contactInfo: z.string().optional(),
});

function normalizeHeader(header: string): string {
  return header.trim().toLowerCase();
}

export async function importClientsXlsx(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageClients");

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
  const columnMap: Record<number, "name" | "address" | "contactInfo"> = {};
  headerRow.eachCell((cell, colNumber) => {
    const value = normalizeHeader(String(cell.value ?? ""));
    if (["ime", "name", "stranka"].includes(value)) columnMap[colNumber] = "name";
    else if (["naslov", "address"].includes(value)) columnMap[colNumber] = "address";
    else if (["kontakt", "contact", "contactinfo", "telefon", "email"].includes(value)) {
      columnMap[colNumber] = "contactInfo";
    }
  });

  if (!Object.values(columnMap).includes("name")) {
    return { error: 'Datoteka mora imeti stolpec "Ime" (ali "Name"/"Stranka").' };
  }

  const rows: clientsApi.ClientInput[] = [];
  let skipped = 0;

  for (let rowNumber = 2; rowNumber <= sheet.rowCount; rowNumber++) {
    const row = sheet.getRow(rowNumber);
    if (row.cellCount === 0) continue;

    const raw: Record<string, string> = {};
    row.eachCell((cell, colNumber) => {
      const field = columnMap[colNumber];
      if (field) raw[field] = String(cell.value ?? "").trim();
    });

    const parsed = importRowSchema.safeParse({
      name: raw.name,
      address: raw.address || undefined,
      contactInfo: raw.contactInfo || undefined,
    });

    if (!parsed.success || !parsed.data.name) {
      skipped++;
      continue;
    }

    rows.push(parsed.data);
  }

  let created = 0;
  if (rows.length > 0) {
    try {
      ({ createdCount: created } = await clientsApi.importClients(rows));
    } catch (e) {
      return { error: apiErrorMessage(e) };
    }
  }

  revalidatePath("/admin/stranke");
  return { success: `Uvoženih ${created} strank${skipped ? `, preskočenih ${skipped} vrstic` : ""}.` };
}

const syncConfigSchema = z.object({
  apiUrl: z.string().url("Vnesi veljaven URL").or(z.literal("")),
  apiToken: z.string().optional(),
});

export async function saveSyncConfig(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageClients");

  const parsed = syncConfigSchema.safeParse({
    apiUrl: formData.get("apiUrl") ?? "",
    apiToken: formData.get("apiToken") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki" };
  }

  // Prazen žeton obdrži obstoječega (obrazec ga ne prikaže), prazen URL izbriše povezavo skupaj z žetonom.
  try {
    await clientsApi.saveClientSyncConfig(parsed.data);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  revalidatePath("/admin/stranke");
  return { success: "Nastavitve sinhronizacije shranjene." };
}

// Sinhronizacijo izvede API: prenese stranke z nastavljenega URL-ja in zapiše izid.
export async function runSyncNow(): Promise<ActionState> {
  await requirePermission("canManageClients");

  try {
    const { syncedCount } = await clientsApi.syncClients();
    revalidatePath("/admin/stranke");
    return { success: `Sinhroniziranih ${syncedCount} strank.` };
  } catch (e) {
    // Neuspeh je zapisan v nastavitvah, zato se osveži tudi prikaz zadnje sinhronizacije.
    revalidatePath("/admin/stranke");
    return { error: apiErrorMessage(e) };
  }
}
