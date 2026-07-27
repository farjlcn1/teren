"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";
import { hashPassword, passwordSchema } from "@/lib/auth/password";

export type ActionState = { error?: string; success?: string } | undefined;

const createUserSchema = z.object({
  email: z.string().email("Vnesi veljaven email."),
  fullName: z.string().min(1, "Vnesi ime in priimek."),
  password: passwordSchema,
});

export async function createUser(_prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageUsers");

  const parsed = createUserSchema.safeParse({
    email: formData.get("email"),
    fullName: formData.get("fullName"),
    password: formData.get("password"),
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavni podatki." };
  }

  const existing = await prisma.user.findUnique({ where: { email: parsed.data.email.toLowerCase() } });
  if (existing) {
    return { error: "Uporabnik s tem emailom že obstaja." };
  }

  const passwordHash = await hashPassword(parsed.data.password);
  await prisma.user.create({
    data: {
      email: parsed.data.email.toLowerCase(),
      fullName: parsed.data.fullName,
      passwordHash,
    },
  });

  revalidatePath("/admin/uporabniki");
  return { success: "Uporabnik ustvarjen." };
}

const permissionKeys = [
  "canManageUsers",
  "canViewAllOrders",
  "canExportData",
  "canSendEmail",
  "canEditOrders",
  "canManageClients",
  "canManageVehicles",
] as const;

export async function updateUserPermissions(userId: string, formData: FormData): Promise<void> {
  await requirePermission("canManageUsers");

  const data = Object.fromEntries(permissionKeys.map((key) => [key, formData.get(key) === "on"]));
  const isActive = formData.get("isActive") === "on";

  await prisma.user.update({ where: { id: userId }, data: { ...data, isActive } });

  revalidatePath("/admin/uporabniki");
  redirect("/admin/uporabniki");
}

const passwordResetSchema = z.object({ password: passwordSchema });

export async function resetUserPassword(userId: string, _prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageUsers");

  const parsed = passwordResetSchema.safeParse({ password: formData.get("password") });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavno geslo." };
  }

  const passwordHash = await hashPassword(parsed.data.password);
  await prisma.user.update({ where: { id: userId }, data: { passwordHash } });

  return { success: "Geslo posodobljeno." };
}
