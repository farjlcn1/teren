"use server";

import { z } from "zod";
import ExcelJS from "exceljs";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";

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

  await prisma.client.create({ data: { ...parsed.data, source: "MANUAL" } });
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

  await prisma.client.update({ where: { id }, data: parsed.data });
  revalidatePath("/admin/stranke");
  redirect("/admin/stranke");
}

export async function deleteClient(id: string) {
  await requirePermission("canManageClients");

  try {
    await prisma.client.delete({ where: { id } });
  } catch {
    throw new Error("Stranke ni mogoče izbrisati, ker ima povezane delovne naloge.");
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

    const parsed = importRowSchema.safeParse({
      name: raw.name,
      address: raw.address || undefined,
      contactInfo: raw.contactInfo || undefined,
    });

    if (!parsed.success || !parsed.data.name) {
      skipped++;
      continue;
    }

    await prisma.client.create({ data: { ...parsed.data, source: "IMPORT" } });
    created++;
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

  await prisma.clientSyncConfig.upsert({
    where: { id: "default" },
    create: { id: "default", apiUrl: parsed.data.apiUrl || null, apiToken: parsed.data.apiToken || null },
    update: { apiUrl: parsed.data.apiUrl || null, apiToken: parsed.data.apiToken || null },
  });

  revalidatePath("/admin/stranke");
  return { success: "Nastavitve sinhronizacije shranjene." };
}

const syncItemSchema = z.object({
  externalId: z.string().min(1),
  name: z.string().min(1),
  address: z.string().optional(),
  contactInfo: z.string().optional(),
});

export async function runSyncNow(): Promise<ActionState> {
  await requirePermission("canManageClients");

  const config = await prisma.clientSyncConfig.findUnique({ where: { id: "default" } });
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
      await prisma.client.upsert({
        where: { externalId: item.externalId },
        create: { ...item, source: "SYNC" },
        update: { name: item.name, address: item.address, contactInfo: item.contactInfo },
      });
    }

    await prisma.clientSyncConfig.update({
      where: { id: "default" },
      data: { lastSyncAt: new Date(), lastSyncStatus: `V redu — ${parsed.length} strank` },
    });

    revalidatePath("/admin/stranke");
    return { success: `Sinhroniziranih ${parsed.length} strank.` };
  } catch (e) {
    const message = e instanceof Error ? e.message : "neznana napaka";
    await prisma.clientSyncConfig
      .update({ where: { id: "default" }, data: { lastSyncAt: new Date(), lastSyncStatus: `Napaka: ${message}` } })
      .catch(() => undefined);
    return { error: `Sinhronizacija ni uspela: ${message}` };
  }
}
