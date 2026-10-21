"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { passwordSchema } from "@/lib/auth/password";
import { apiErrorMessage } from "@/lib/api/http";
import * as usersApi from "@/lib/api/users";

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

  // API geslo zgošči in zavrne email, ki ga že uporablja drug uporabnik.
  try {
    await usersApi.createUser(parsed.data);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

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
  "canManagePlan",
] as const;

export async function updateUserPermissions(userId: string, formData: FormData): Promise<void> {
  await requirePermission("canManageUsers");

  const permissions = Object.fromEntries(permissionKeys.map((key) => [key, formData.get(key) === "on"])) as Record<
    (typeof permissionKeys)[number],
    boolean
  >;
  const isActive = formData.get("isActive") === "on";

  try {
    await usersApi.updateUserAccess(userId, { ...permissions, isActive });
  } catch (e) {
    throw new Error(apiErrorMessage(e));
  }

  revalidatePath("/admin/uporabniki");
  redirect("/admin/uporabniki");
}

// Vrne ActionState namesto da bi napako vrgel: vržene napake iz server akcij se v produkciji
// pošljejo klientu samo kot splošno "An error occurred..." sporočilo (React/Next.js privzeto
// odstrani dejansko sporočilo iz vsakega vrženega Errorja pri prenosu prek RSC) -- ostale akcije
// v tej datoteki (createUser, resetUserPassword) iz istega razloga tudi vračajo, ne mečejo.
export async function deleteUser(id: string): Promise<ActionState> {
  await requirePermission("canManageUsers");

  try {
    await usersApi.deleteUser(id);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  revalidatePath("/admin/uporabniki");
}

const passwordResetSchema = z.object({ password: passwordSchema });

export async function resetUserPassword(userId: string, _prevState: ActionState, formData: FormData): Promise<ActionState> {
  await requirePermission("canManageUsers");

  const parsed = passwordResetSchema.safeParse({ password: formData.get("password") });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljavno geslo." };
  }

  try {
    await usersApi.changeUserPassword(userId, parsed.data.password);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }

  return { success: "Geslo posodobljeno." };
}
