"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";
import type { InstallerName } from "@/generated/prisma/client";

const INSTALLER_NAMES = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"] as const;
const WORK_ORDER_TYPES = ["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"] as const;

export type PlanActionState = { error?: string; success?: boolean; groupId?: string } | undefined;

const planGroupSchema = z.object({
  clientId: z.string().min(1, "Izberi stranko."),
  startAt: z.string().min(1, "Vnesi začetek."),
  // Neobvezno pri kreiranju (createPlanGroup privzame 1h trajanje) -- updatePlanGroup ob manjkajočem
  // koncu ob ureji vseeno vrne napako, glej tam.
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

// Isti monter (po imenu; pri "Ostalo" tudi po prostem besedilu, ker gre lahko za različne ljudi) ne
// sme imeti dveh časovno prekrivajočih se dogodkov -- brez tega bi lahko dispečer po nesreči
// razporedil isto osebo na dve mesti hkrati. Brez izbranega monterja preverjanje odpade, saj ni s
// čim preveriti prekrivanja.
async function findInstallerOverlapError(
  installer: InstallerName | null,
  otherText: string | null,
  startAt: Date,
  endAt: Date,
  excludeId?: string
): Promise<string | null> {
  if (!installer) return null;

  const conflict = await prisma.planGroup.findFirst({
    where: {
      expectedInstaller: installer,
      ...(installer === "OSTALO" ? { expectedInstallerOtherText: otherText } : {}),
      startAt: { lt: endAt },
      endAt: { gt: startAt },
      ...(excludeId ? { id: { not: excludeId } } : {}),
    },
    select: { id: true },
  });

  return conflict ? "Izbrani monter ima v tem času že drug dogodek." : null;
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

  const startAt = new Date(parsed.data.startAt);
  if (Number.isNaN(startAt.getTime())) {
    return { error: "Neveljaven datum/čas." };
  }

  // Konec je neobvezen -- brez njega dogodek privzeto traja 1h, enako kot nalog ustvarjen izven
  // plana (glej createWorkOrder v nalogi/nov/actions.ts).
  let endAt: Date;
  if (parsed.data.endAt) {
    endAt = new Date(parsed.data.endAt);
    if (Number.isNaN(endAt.getTime())) {
      return { error: "Neveljaven datum/čas." };
    }
    if (endAt <= startAt) {
      return { error: "Konec mora biti po začetku." };
    }
  } else {
    endAt = new Date(startAt.getTime() + 60 * 60 * 1000);
  }

  const expectedInstaller: InstallerName | null = parsed.data.expectedInstaller || null;
  if (expectedInstaller === "OSTALO" && !parsed.data.expectedInstallerOtherText?.trim()) {
    return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
  }

  const client = await prisma.client.findUnique({ where: { id: parsed.data.clientId } });
  if (!client) return { error: "Stranka ne obstaja." };

  const overlapError = await findInstallerOverlapError(
    expectedInstaller,
    expectedInstaller === "OSTALO" ? parsed.data.expectedInstallerOtherText || null : null,
    startAt,
    endAt
  );
  if (overlapError) return { error: overlapError };

  const group = await prisma.planGroup.create({
    data: {
      clientId: client.id,
      startAt,
      endAt,
      note: parsed.data.note || null,
      contact: parsed.data.contact || null,
      expectedInstaller,
      expectedInstallerOtherText: expectedInstaller === "OSTALO" ? parsed.data.expectedInstallerOtherText || null : null,
      createdById: user.id,
      tasks:
        tasksParsed.data.length > 0
          ? { create: tasksParsed.data.map((t) => ({ vehiclePlate: t.vehiclePlate, type: t.type || null, note: t.note || null })) }
          : undefined,
    },
  });

  revalidatePath("/plan");
  return { success: true, groupId: group.id };
}

export async function updatePlanGroup(
  planGroupId: string,
  _prevState: PlanActionState,
  formData: FormData
): Promise<PlanActionState> {
  await requirePermission("canManagePlan");

  const existing = await prisma.planGroup.findUnique({
    where: { id: planGroupId },
    include: { tasks: { select: { workOrderId: true } } },
  });
  if (!existing) return { error: "Dogodek ne obstaja." };

  const parsed = parsePlanGroupInput(formData);
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const hasCompletedTask = existing.tasks.some((t) => t.workOrderId);
  if (hasCompletedTask && parsed.data.clientId !== existing.clientId) {
    return { error: "Stranke ni mogoče spremeniti, ker je bil vsaj en nalog v tej skupini že opravljen." };
  }

  // Pri urejanju (za razliko od kreiranja) konec ostane obvezen -- samodejno 1h trajanje velja samo
  // za nov dogodek brez izbranega konca.
  if (!parsed.data.endAt) {
    return { error: "Vnesi konec." };
  }

  const startAt = new Date(parsed.data.startAt);
  const endAt = new Date(parsed.data.endAt);
  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime())) {
    return { error: "Neveljaven datum/čas." };
  }
  if (endAt <= startAt) {
    return { error: "Konec mora biti po začetku." };
  }

  const expectedInstaller: InstallerName | null = parsed.data.expectedInstaller || null;
  if (expectedInstaller === "OSTALO" && !parsed.data.expectedInstallerOtherText?.trim()) {
    return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
  }

  const client = await prisma.client.findUnique({ where: { id: parsed.data.clientId } });
  if (!client) return { error: "Stranka ne obstaja." };

  const overlapError = await findInstallerOverlapError(
    expectedInstaller,
    expectedInstaller === "OSTALO" ? parsed.data.expectedInstallerOtherText || null : null,
    startAt,
    endAt,
    planGroupId
  );
  if (overlapError) return { error: overlapError };

  await prisma.planGroup.update({
    where: { id: planGroupId },
    data: {
      clientId: client.id,
      startAt,
      endAt,
      note: parsed.data.note || null,
      contact: parsed.data.contact || null,
      expectedInstaller,
      expectedInstallerOtherText: expectedInstaller === "OSTALO" ? parsed.data.expectedInstallerOtherText || null : null,
    },
  });

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

  const existing = await prisma.planGroup.findUnique({ where: { id: planGroupId } });
  if (!existing) return { error: "Dogodek ne obstaja." };

  const overlapError = await findInstallerOverlapError(
    existing.expectedInstaller,
    existing.expectedInstallerOtherText,
    startAt,
    endAt,
    planGroupId
  );
  if (overlapError) return { error: overlapError };

  await prisma.planGroup.update({ where: { id: planGroupId }, data: { startAt, endAt } });
  revalidatePath("/plan");
}

export async function deletePlanGroup(planGroupId: string): Promise<{ error?: string } | undefined> {
  await requirePermission("canManagePlan");

  const existing = await prisma.planGroup.findUnique({
    where: { id: planGroupId },
    include: { tasks: { select: { workOrderId: true } } },
  });
  if (!existing) return { error: "Dogodek ne obstaja." };

  if (existing.tasks.some((t) => t.workOrderId)) {
    return { error: "Dogodka ni mogoče izbrisati, ker vsebuje že opravljen nalog." };
  }

  await prisma.planGroup.delete({ where: { id: planGroupId } });
  revalidatePath("/plan");
}

export async function addPlannedTask(
  planGroupId: string,
  _prevState: PlanActionState,
  formData: FormData
): Promise<PlanActionState> {
  await requirePermission("canManagePlan");

  const group = await prisma.planGroup.findUnique({ where: { id: planGroupId } });
  if (!group) return { error: "Dogodek ne obstaja." };

  const parsed = plannedTaskSchema.safeParse({
    vehiclePlate: formData.get("vehiclePlate"),
    type: formData.get("type") || "",
    note: formData.get("note") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  await prisma.plannedTask.create({
    data: {
      planGroupId,
      vehiclePlate: parsed.data.vehiclePlate,
      type: parsed.data.type || null,
      note: parsed.data.note || null,
    },
  });

  revalidatePath("/plan");
  return { success: true };
}

export async function deletePlannedTask(taskId: string): Promise<{ error?: string } | undefined> {
  await requirePermission("canManagePlan");

  const task = await prisma.plannedTask.findUnique({ where: { id: taskId } });
  if (!task) return { error: "Nalog ne obstaja." };
  if (task.workOrderId) return { error: "Opravljenega naloga ni mogoče izbrisati." };

  await prisma.plannedTask.delete({ where: { id: taskId } });
  revalidatePath("/plan");
}
