"use server";

import { z } from "zod";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/db";
import { requireUser } from "@/lib/auth/session";
import { generateIdent } from "@/lib/work-orders/ident";
import { savePhoto, saveSignature } from "@/lib/uploads";
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
    "WIRE_TEMP1",
    "WIRE_TEMP2",
    "WIRE_TEMP3",
    "ID_KEY",
    "RFID_125",
    "RFID_1356",
    "BUZZER",
  ]),
  comment: z.string().optional(),
});

const deviceModelSchema = z.object({
  deviceModel: z.enum(["FMC130", "FMC150", "FMC650", "FMC880", "TFT100", "OSTALO"]),
  comment: z.string().optional(),
});

const imeiSchema = z
  .string()
  .regex(/^\d{10}$/, "IMEI mora vsebovati natanko zadnjih 10 številk.");

const baseSchema = z.object({
  type: z.enum(["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"]),
  difficulty: z.enum(["OSNOVNA", "ZAHTEVNA"]),
  clientId: z.string().min(1, "Izberi stranko."),
  vehiclePlate: z.string().min(1, "Vnesi registrsko številko."),
  vehicleBrand: z.string().min(1, "Vnesi znamko vozila."),
  vehicleModel: z.string().min(1, "Vnesi model vozila."),
  vehicleYear: z.coerce
    .number()
    .int()
    .min(1950, "Neveljavno leto.")
    .max(new Date().getFullYear() + 1, "Neveljavno leto."),
  imei: imeiSchema,
  imeiPrev: z.union([imeiSchema, z.literal("")]).optional(),
  culprit: z.union([z.enum(["SLEDENJE", "STRANKA"]), z.literal("")]).optional(),
  comment: z.string().optional(),
  installers: z.array(installerSchema).min(1, "Izberi vsaj enega monterja."),
  options: z.array(optionSchema),
  deviceModels: z.array(deviceModelSchema),
});

export type CreateWorkOrderState = { error?: string } | undefined;

export async function createWorkOrder(
  _prevState: CreateWorkOrderState,
  formData: FormData
): Promise<CreateWorkOrderState> {
  const user = await requireUser();

  let installers: unknown;
  let options: unknown;
  let deviceModels: unknown;
  try {
    installers = JSON.parse(String(formData.get("installers") ?? "[]"));
    options = JSON.parse(String(formData.get("options") ?? "[]"));
    deviceModels = JSON.parse(String(formData.get("deviceModels") ?? "[]"));
  } catch {
    return { error: "Napaka pri branju obrazca." };
  }

  const parsed = baseSchema.safeParse({
    type: formData.get("type"),
    difficulty: formData.get("difficulty"),
    clientId: formData.get("clientId"),
    vehiclePlate: formData.get("vehiclePlate"),
    vehicleBrand: formData.get("vehicleBrand"),
    vehicleModel: formData.get("vehicleModel"),
    vehicleYear: formData.get("vehicleYear"),
    imei: formData.get("imei"),
    imeiPrev: formData.get("imeiPrev") ?? "",
    culprit: formData.get("culprit") ?? "",
    comment: formData.get("comment") ?? "",
    installers,
    options,
    deviceModels,
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const data = parsed.data;

  if (data.type === "INTERVENCIJA" && !data.imeiPrev) {
    return { error: "Pri intervenciji je polje 'IMEI prej' obvezno." };
  }

  if (data.type === "INTERVENCIJA" && !data.culprit) {
    return { error: "Pri intervenciji je polje 'Krivec' obvezno." };
  }

  for (const inst of data.installers) {
    if (inst.name === "OSTALO" && !inst.otherText?.trim()) {
      return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
    }
  }

  for (const dm of data.deviceModels) {
    if (dm.deviceModel === "OSTALO" && !dm.comment?.trim()) {
      return { error: "Vnesi napravo pri izbiri 'Drugo'." };
    }
  }

  const signatureFile = formData.get("signature");
  const signature = signatureFile instanceof File && signatureFile.size > 0 ? signatureFile : null;

  const photos = formData.getAll("photos").filter((f): f is File => f instanceof File && f.size > 0);

  const orderDate = new Date();
  const ident = await generateIdent(orderDate);

  const workOrder = await prisma.workOrder.create({
    data: {
      ident,
      orderDate,
      type: data.type,
      difficulty: data.difficulty,
      clientId: data.clientId,
      vehiclePlate: data.vehiclePlate,
      vehicleBrand: data.vehicleBrand,
      vehicleModel: data.vehicleModel,
      vehicleYear: data.vehicleYear,
      imei: data.imei,
      imeiPrev: data.imeiPrev || null,
      culprit: data.culprit || null,
      comment: data.comment || null,
      createdById: user.id,
      installers: { create: data.installers },
      options: { create: data.options },
      deviceModels: { create: data.deviceModels },
    },
  });

  await ensureVehicleExists(data.vehiclePlate);

  const signaturePath = signature ? await saveSignature(workOrder.id, signature) : null;
  const savedPhotos = await Promise.all(photos.map((file, i) => savePhoto(workOrder.id, file, i)));

  const now = new Date();
  await prisma.workOrder.update({
    where: { id: workOrder.id },
    data: {
      ...(signaturePath ? { signatureUrl: signaturePath, signedAt: now, lockedAt: now } : {}),
      photos: { create: savedPhotos.map((p) => ({ filePath: p.filePath, takenAt: p.takenAt })) },
    },
  });

  redirect(`/nalogi/${workOrder.id}`);
}
