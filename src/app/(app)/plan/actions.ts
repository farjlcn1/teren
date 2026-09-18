"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";
import type { InstallerName } from "@/generated/prisma/client";

const INSTALLER_NAMES = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"] as const;

export type PlanActionState = { error?: string; success?: boolean; groupId?: string } | undefined;

const planGroupSchema = z.object({
  clientId: z.string().min(1, "Izberi stranko."),
  startAt: z.string().min(1, "Vnesi začetek."),
  endAt: z.string().min(1, "Vnesi konec."),
  note: z.string().optional(),
});

function parsePlanGroupInput(formData: FormData) {
  return planGroupSchema.safeParse({
    clientId: formData.get("clientId"),
    startAt: formData.get("startAt"),
    endAt: formData.get("endAt"),
    note: formData.get("note") || undefined,
  });
}

export async function createPlanGroup(_prevState: PlanActionState, formData: FormData): Promise<PlanActionState> {
  const user = await requirePermission("canManagePlan");

  const parsed = parsePlanGroupInput(formData);
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const startAt = new Date(parsed.data.startAt);
  const endAt = new Date(parsed.data.endAt);
  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime())) {
    return { error: "Neveljaven datum/čas." };
  }
  if (endAt <= startAt) {
    return { error: "Konec mora biti po začetku." };
  }

  const client = await prisma.client.findUnique({ where: { id: parsed.data.clientId } });
  if (!client) return { error: "Stranka ne obstaja." };

  const group = await prisma.planGroup.create({
    data: {
      clientId: client.id,
      startAt,
      endAt,
      note: parsed.data.note || null,
      createdById: user.id,
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

  const startAt = new Date(parsed.data.startAt);
  const endAt = new Date(parsed.data.endAt);
  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime())) {
    return { error: "Neveljaven datum/čas." };
  }
  if (endAt <= startAt) {
    return { error: "Konec mora biti po začetku." };
  }

  const client = await prisma.client.findUnique({ where: { id: parsed.data.clientId } });
  if (!client) return { error: "Stranka ne obstaja." };

  await prisma.planGroup.update({
    where: { id: planGroupId },
    data: { clientId: client.id, startAt, endAt, note: parsed.data.note || null },
  });

  revalidatePath("/plan");
  return { success: true };
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

const plannedTaskSchema = z.object({
  vehiclePlate: z.string().trim().min(1, "Vnesi registrsko številko."),
  note: z.string().optional(),
  expectedInstaller: z.union([z.enum(INSTALLER_NAMES), z.literal("")]).optional(),
  expectedInstallerOtherText: z.string().optional(),
});

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
    note: formData.get("note") || undefined,
    expectedInstaller: formData.get("expectedInstaller") || "",
    expectedInstallerOtherText: formData.get("expectedInstallerOtherText") || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const expectedInstaller: InstallerName | null = parsed.data.expectedInstaller || null;
  if (expectedInstaller === "OSTALO" && !parsed.data.expectedInstallerOtherText?.trim()) {
    return { error: "Vnesi ime monterja pri izbiri 'Ostalo'." };
  }

  await prisma.plannedTask.create({
    data: {
      planGroupId,
      vehiclePlate: parsed.data.vehiclePlate,
      note: parsed.data.note || null,
      expectedInstaller,
      expectedInstallerOtherText: expectedInstaller === "OSTALO" ? parsed.data.expectedInstallerOtherText || null : null,
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
