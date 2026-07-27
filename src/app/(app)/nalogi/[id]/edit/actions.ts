"use server";

import { z } from "zod";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";
import { ensureVehicleExists } from "@/lib/vehicles";

const installerSchema = z.object({
  name: z.enum(["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"]),
  otherText: z.string().optional(),
});

const optionSchema = z.object({
  optionType: z.enum([
    "DIN1",
    "DIN2",
    "DIN3",
    "DIN4",
    "DIN5",
    "ANI1",
    "ANI2",
    "ANI3",
    "ALL_CAN",
    "FMSCAN",
    "TACHO",
  ]),
  comment: z.string().optional(),
});

const imeiSchema = z.string().regex(/^\d{10}$/, "IMEI mora vsebovati natanko zadnjih 10 številk.");

const editSchema = z.object({
  type: z.enum(["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"]),
  difficulty: z.enum(["OSNOVNA", "ZAHTEVNA"]),
  clientId: z.string().min(1, "Izberi stranko."),
  vehiclePlate: z.string().min(1),
  vehicleBrand: z.string().min(1),
  vehicleModel: z.string().min(1),
  vehicleYear: z.coerce.number().int().min(1950).max(new Date().getFullYear() + 1),
  imei: imeiSchema,
  imeiPrev: z.union([imeiSchema, z.literal("")]).optional(),
  comment: z.string().optional(),
  installers: z.array(installerSchema).min(1, "Izberi vsaj enega monterja."),
  options: z.array(optionSchema),
});

const scalarFieldLabels: Record<string, string> = {
  type: "Tip",
  difficulty: "Zahtevnost",
  clientId: "Stranka",
  vehiclePlate: "Registrska št.",
  vehicleBrand: "Znamka",
  vehicleModel: "Model",
  vehicleYear: "Letnik",
  imei: "IMEI",
  imeiPrev: "IMEI prej",
  comment: "Komentar",
};

export type EditState = { error?: string } | undefined;

export async function updateWorkOrder(id: string, _prevState: EditState, formData: FormData): Promise<EditState> {
  const user = await requirePermission("canEditOrders");

  let installers: unknown;
  let options: unknown;
  try {
    installers = JSON.parse(String(formData.get("installers") ?? "[]"));
    options = JSON.parse(String(formData.get("options") ?? "[]"));
  } catch {
    return { error: "Napaka pri branju obrazca." };
  }

  const parsed = editSchema.safeParse({
    type: formData.get("type"),
    difficulty: formData.get("difficulty"),
    clientId: formData.get("clientId"),
    vehiclePlate: formData.get("vehiclePlate"),
    vehicleBrand: formData.get("vehicleBrand"),
    vehicleModel: formData.get("vehicleModel"),
    vehicleYear: formData.get("vehicleYear"),
    imei: formData.get("imei"),
    imeiPrev: formData.get("imeiPrev") ?? "",
    comment: formData.get("comment") ?? "",
    installers,
    options,
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const data = parsed.data;
  if (data.type === "INTERVENCIJA" && !data.imeiPrev) {
    return { error: "Pri intervenciji je polje 'IMEI prej' obvezno." };
  }
  for (const inst of data.installers) {
    if (inst.name === "OSTALO" && !inst.otherText?.trim()) {
      return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
    }
  }

  const before = await prisma.workOrder.findUnique({
    where: { id },
    include: { installers: true, options: true },
  });
  if (!before) return { error: "Nalog ne obstaja." };

  const changes: Record<string, { from: unknown; to: unknown }> = {};
  const scalarValues: Record<string, unknown> = {
    type: data.type,
    difficulty: data.difficulty,
    clientId: data.clientId,
    vehiclePlate: data.vehiclePlate,
    vehicleBrand: data.vehicleBrand,
    vehicleModel: data.vehicleModel,
    vehicleYear: data.vehicleYear,
    imei: data.imei,
    imeiPrev: data.imeiPrev || null,
    comment: data.comment || null,
  };
  const beforeValues: Record<string, unknown> = before;
  for (const [field, newValue] of Object.entries(scalarValues)) {
    const oldValue = beforeValues[field] ?? null;
    if (String(oldValue ?? "") !== String(newValue ?? "")) {
      changes[scalarFieldLabels[field] ?? field] = { from: oldValue, to: newValue };
    }
  }

  const beforeInstallers = before.installers.map((i) => `${i.name}:${i.otherText ?? ""}`).sort().join(",");
  const afterInstallers = data.installers.map((i) => `${i.name}:${i.otherText ?? ""}`).sort().join(",");
  if (beforeInstallers !== afterInstallers) {
    changes["Monterji"] = { from: beforeInstallers, to: afterInstallers };
  }

  const beforeOptions = before.options.map((o) => `${o.optionType}:${o.comment ?? ""}`).sort().join(",");
  const afterOptions = data.options.map((o) => `${o.optionType}:${o.comment ?? ""}`).sort().join(",");
  if (beforeOptions !== afterOptions) {
    changes["DIN/ANI/CAN"] = { from: beforeOptions, to: afterOptions };
  }

  if (Object.keys(changes).length === 0) {
    await ensureVehicleExists(data.vehiclePlate);
    redirect(`/nalogi/${id}`);
  }

  await prisma.$transaction([
    prisma.workOrder.update({
      where: { id },
      data: {
        ...scalarValues,
        installers: { deleteMany: {}, create: data.installers },
        options: { deleteMany: {}, create: data.options },
      },
    }),
    prisma.workOrderEdit.create({
      data: { workOrderId: id, editedById: user.id, changesJson: JSON.stringify(changes) },
    }),
  ]);

  await ensureVehicleExists(data.vehiclePlate);

  redirect(`/nalogi/${id}`);
}
