"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { requirePermission } from "@/lib/auth/session";
import { apiErrorMessage } from "@/lib/api/http";
import * as planApi from "@/lib/api/plan";

const INSTALLER_NAMES = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"] as const;
const WORK_ORDER_TYPES = ["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"] as const;

export type PlanActionState = { error?: string; success?: boolean; groupId?: string } | undefined;

const planGroupSchema = z.object({
  clientId: z.string().min(1, "Izberi stranko."),
  startAt: z.string().min(1, "Vnesi začetek."),
  // Neobvezno pri kreiranju (API privzame 1h trajanje) -- updatePlanGroup ob manjkajočem koncu
  // vseeno vrne napako, glej tam.
  endAt: z.string().optional(),
  note: z.string().optional(),
  contact: z.string().optional(),
  expectedInstaller: z.union([z.enum(INSTALLER_NAMES), z.literal("")]).optional(),
  expectedInstallerOtherText: z.string().optional(),
});

const plannedTaskSchema = z.object({
  vehiclePlate: z.string().trim().min(1, "Vnesi registrsko številko."),
  type: z.union([z.enum(WORK_ORDER_TYPES), z.literal("")]).optional(),
  note: z.string().optional(),
});

function parsePlanGroupInput(formData: FormData) {
  return planGroupSchema.safeParse({
    clientId: formData.get("clientId"),
    startAt: formData.get("startAt"),
    endAt: formData.get("endAt"),
    note: formData.get("note") || undefined,
    contact: formData.get("contact") || undefined,
    expectedInstaller: formData.get("expectedInstaller") || "",
    expectedInstallerOtherText: formData.get("expectedInstallerOtherText") || undefined,
  });
}

// Čas iz obrazca (datetime-local) je po lokalnem času strežnika; API dobi točen trenutek.
// Prekrivajoče se dogodke istega monterja zamakne API (glej PlanService), tu se preveri samo obrazec.
function toPlanGroupInput(
  data: z.infer<typeof planGroupSchema>,
  endRequired: boolean
): { input: planApi.PlanGroupInput } | { error: string } {
  if (endRequired && !data.endAt) {
    return { error: "Vnesi konec." };
  }
  const startAt = new Date(data.startAt);
  const endAt = data.endAt ? new Date(data.endAt) : null;
  if (Number.isNaN(startAt.getTime()) || (endAt && Number.isNaN(endAt.getTime()))) {
    return { error: "Neveljaven datum/čas." };
  }
  if (endAt && endAt <= startAt) {
    return { error: "Konec mora biti po začetku." };
  }
  const expectedInstaller = data.expectedInstaller || null;
  if (expectedInstaller === "OSTALO" && !data.expectedInstallerOtherText?.trim()) {
    return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
  }
  return {
    input: {
      clientId: data.clientId,
      startAt,
      endAt,
      note: data.note || null,
      contact: data.contact || null,
      expectedInstaller,
      expectedInstallerOtherText: expectedInstaller === "OSTALO" ? data.expectedInstallerOtherText || null : null,
    },
  };
}

export async function createPlanGroup(_prevState: PlanActionState, formData: FormData): Promise<PlanActionState> {
  const user = await requirePermission("canManagePlan");

  const parsed = parsePlanGroupInput(formData);
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  let tasksInput: unknown;
  try {
    tasksInput = JSON.parse(String(formData.get("tasks") ?? "[]"));
  } catch {
    return { error: "Napaka pri branju nalogov." };
  }
  const tasksParsed = z.array(plannedTaskSchema).safeParse(tasksInput);
  if (!tasksParsed.success) {
    return { error: tasksParsed.error.issues[0]?.message ?? "Neveljavni nalogi." };
  }

  const planGroup = toPlanGroupInput(parsed.data, false);
  if ("error" in planGroup) return planGroup;

  let groupId: string;
  try {
    ({ id: groupId } = await planApi.createPlanGroup({
      ...planGroup.input,
      createdById: user.id,
      tasks: tasksParsed.data.map((t) => ({ vehiclePlate: t.vehiclePlate, type: t.type || null, note: t.note || null })),
    }));
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  revalidatePath("/plan");
  return { success: true, groupId };
}

export async function updatePlanGroup(
  planGroupId: string,
  _prevState: PlanActionState,
  formData: FormData
): Promise<PlanActionState> {
  await requirePermission("canManagePlan");

  const parsed = parsePlanGroupInput(formData);
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  // Pri urejanju (za razliko od kreiranja) konec ostane obvezen -- samodejno 1h trajanje velja samo
  // za nov dogodek brez izbranega konca. Spremembo stranke dogodka z opravljenim nalogom zavrne API.
  const planGroup = toPlanGroupInput(parsed.data, true);
  if ("error" in planGroup) return planGroup;

  try {
    await planApi.updatePlanGroup(planGroupId, planGroup.input);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  revalidatePath("/plan");
  return { success: true };
}

// Lahkotna posodobitev samo časa -- za povleci-in-spusti premikanje dogodka po koledarju. Za
// razliko od updatePlanGroup ne spreminja stranke/opombe/monterja in zato ne potrebuje polnega
// obrazca; kliče se neposredno (ne prek useActionState), enako kot deletePlanGroup spodaj.
export async function movePlanGroup(
  planGroupId: string,
  startAtIso: string,
  endAtIso: string
): Promise<{ error?: string } | undefined> {
  await requirePermission("canManagePlan");

  const startAt = new Date(startAtIso);
  const endAt = new Date(endAtIso);
  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime())) {
    return { error: "Neveljaven datum/čas." };
  }
  if (endAt <= startAt) {
    return { error: "Konec mora biti po začetku." };
  }

  try {
    await planApi.movePlanGroup(planGroupId, startAt, endAt);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/plan");
}

export async function deletePlanGroup(planGroupId: string): Promise<{ error?: string } | undefined> {
  await requirePermission("canManagePlan");

  // Dogodka z že opravljenim nalogom API ne izbriše (sporočilo vrne v napaki).
  try {
    await planApi.deletePlanGroup(planGroupId);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/plan");
}

export async function addPlannedTask(
  planGroupId: string,
  _prevState: PlanActionState,
  formData: FormData
): Promise<PlanActionState> {
  await requirePermission("canManagePlan");

  const parsed = plannedTaskSchema.safeParse({
    vehiclePlate: formData.get("vehiclePlate"),
    type: formData.get("type") || "",
    note: formData.get("note") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  try {
    await planApi.addPlannedTask(planGroupId, {
      vehiclePlate: parsed.data.vehiclePlate,
      type: parsed.data.type || null,
      note: parsed.data.note || null,
    });
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  revalidatePath("/plan");
  return { success: true };
}

export async function deletePlannedTask(taskId: string): Promise<{ error?: string } | undefined> {
  await requirePermission("canManagePlan");

  // Opravljenega planiranega naloga API ne izbriše (sporočilo vrne v napaki).
  try {
    await planApi.deletePlannedTask(taskId);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/plan");
}
