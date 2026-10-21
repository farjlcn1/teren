"use server";

import { z } from "zod";
import { redirect } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { apiErrorMessage } from "@/lib/api/http";
import { findWorkOrder, updateWorkOrder as updateWorkOrderInApi } from "@/lib/api/work-orders";
import { canAccessWorkOrder } from "@/lib/work-orders/access";

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
  culprit: z.union([z.enum(["SLEDENJE", "STRANKA"]), z.literal("")]).optional(),
  comment: z.string().optional(),
  installers: z.array(installerSchema).min(1, "Izberi vsaj enega monterja."),
  options: z.array(optionSchema),
});

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
    culprit: formData.get("culprit") ?? "",
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
  if (data.type === "INTERVENCIJA" && !data.culprit) {
    return { error: "Pri intervenciji je polje 'Krivec' obvezno." };
  }
  for (const inst of data.installers) {
    if (inst.name === "OSTALO" && !inst.otherText?.trim()) {
      return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
    }
  }

  // ID naloga pride od odjemalca, zato se pravica do naloga preveri tu (enako kot pri podrobnostih).
  const existing = await findWorkOrder(id);
  if (!existing || !canAccessWorkOrder(user, existing)) {
    return { error: "Nalog ne obstaja." };
  }

  // API spremembe zapiše v zgodovino urejanj (z enakimi oznakami polj kot doslej) in doda vozilo, če ga še ni.
  try {
    await updateWorkOrderInApi(id, {
      details: {
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
        installers: data.installers,
        options: data.options,
      },
      editedById: user.id,
    });
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  redirect(`/nalogi/${id}`);
}
