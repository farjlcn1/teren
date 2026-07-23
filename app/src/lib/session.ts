import { SignJWT, jwtVerify } from "jose";
import { cookies } from "next/headers";
import { prisma } from "./prisma";
import type { User } from "@prisma/client";
import { SESSION_COOKIE_NAME } from "./session-constants";

export { SESSION_COOKIE_NAME };

const REMEMBER_ME_SECONDS = 60 * 60 * 24 * 30; // 30 dni
const DEFAULT_SECONDS = 60 * 60 * 12; // 12 ur

function getSecret() {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error("SESSION_SECRET ni nastavljen.");
  }
  return new TextEncoder().encode(secret);
}

export async function createSessionToken(userId: string, rememberMe: boolean) {
  const maxAgeSeconds = rememberMe ? REMEMBER_ME_SECONDS : DEFAULT_SECONDS;
  const token = await new SignJWT({ sub: userId })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime(Math.floor(Date.now() / 1000) + maxAgeSeconds)
    .sign(getSecret());

  return { token, maxAgeSeconds };
}

export async function verifySessionToken(token: string): Promise<string | null> {
  try {
    const { payload } = await jwtVerify(token, getSecret());
    return typeof payload.sub === "string" ? payload.sub : null;
  } catch {
    return null;
  }
}

export type SafeUser = Omit<User, "passwordHash">;

export async function getSessionUser(): Promise<SafeUser | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  if (!token) return null;

  const userId = await verifySessionToken(token);
  if (!userId) return null;

  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user || !user.isActive) return null;

  const { passwordHash: _passwordHash, ...safeUser } = user;
  return safeUser;
}
