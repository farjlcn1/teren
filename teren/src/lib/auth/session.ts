import "server-only";
import { cache } from "react";
import { SignJWT, jwtVerify } from "jose";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { findUser } from "@/lib/api/users";

const SESSION_COOKIE = "teren_session";
const REMEMBER_ME_MAX_AGE = 60 * 60 * 24 * 30; // 30 dni
const DEFAULT_MAX_AGE = 60 * 60 * 12; // 12 ur

// Kratek ali privzet ključ bi omogočil ponarejanje sej, zato aplikacija z njim ne deluje.
function getSecret() {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) throw new Error("SESSION_SECRET ni nastavljen ali je krajši od 32 znakov");
  return new TextEncoder().encode(secret);
}

export type CurrentUser = {
  id: string;
  email: string;
  fullName: string;
  canManageUsers: boolean;
  canViewAllOrders: boolean;
  canExportData: boolean;
  canSendEmail: boolean;
  canEditOrders: boolean;
  canManageClients: boolean;
  canManageVehicles: boolean;
  canManagePlan: boolean;
};

export async function createSession(userId: string, rememberMe: boolean) {
  const maxAge = rememberMe ? REMEMBER_ME_MAX_AGE : DEFAULT_MAX_AGE;
  const token = await new SignJWT({ userId })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime(Math.floor(Date.now() / 1000) + maxAge)
    .sign(getSecret());

  const cookieStore = await cookies();
  cookieStore.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    path: "/",
    maxAge,
  });
}

export async function destroySession() {
  const cookieStore = await cookies();
  cookieStore.delete(SESSION_COOKIE);
}

async function getSessionFromCookie(): Promise<{ userId: string; issuedAt: number } | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE)?.value;
  if (!token) return null;

  try {
    const { payload } = await jwtVerify(token, getSecret(), { algorithms: ["HS256"] });
    if (typeof payload.userId !== "string" || typeof payload.iat !== "number") return null;
    return { userId: payload.userId, issuedAt: payload.iat };
  } catch {
    return null;
  }
}

// Pravice se vedno berejo sveže iz API-ja (ne iz JWT), da odvzem pravic velja takoj.
// React cache: znotraj enega zahtevka (layout, stran, akcija) se uporabnik prebere samo enkrat.
export const getSession = cache(async (): Promise<CurrentUser | null> => {
  const session = await getSessionFromCookie();
  if (!session) return null;

  const user = await findUser(session.userId);
  if (!user || !user.isActive) return null;
  // Sprememba gesla prekliče seje, izdane pred njo (iat je v sekundah).
  if (user.sessionsValidFrom && session.issuedAt < Math.floor(Date.parse(user.sessionsValidFrom) / 1000)) {
    return null;
  }

  return {
    id: user.id,
    email: user.email,
    fullName: user.fullName,
    canManageUsers: user.canManageUsers,
    canViewAllOrders: user.canViewAllOrders,
    canExportData: user.canExportData,
    canSendEmail: user.canSendEmail,
    canEditOrders: user.canEditOrders,
    canManageClients: user.canManageClients,
    canManageVehicles: user.canManageVehicles,
    canManagePlan: user.canManagePlan,
  };
});

export async function requireUser(): Promise<CurrentUser> {
  const user = await getSession();
  if (!user) redirect("/login");
  return user;
}

export async function requirePermission(
  permission: keyof Omit<CurrentUser, "id" | "email" | "fullName">
): Promise<CurrentUser> {
  const user = await requireUser();
  if (!user[permission]) redirect("/");
  return user;
}
