import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { comparePassword } from "@/lib/password";
import { createSessionToken, SESSION_COOKIE_NAME } from "@/lib/session";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
  rememberMe: z.boolean().optional().default(false),
});

export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null);
  const parsed = loginSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Neveljavni podatki." }, { status: 400 });
  }

  const { email, password, rememberMe } = parsed.data;

  const user = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
  if (!user || !user.isActive) {
    return NextResponse.json({ error: "Napačen e-mail ali geslo." }, { status: 401 });
  }

  const valid = await comparePassword(password, user.passwordHash);
  if (!valid) {
    return NextResponse.json({ error: "Napačen e-mail ali geslo." }, { status: 401 });
  }

  const { token, maxAgeSeconds } = await createSessionToken(user.id, rememberMe);

  const response = NextResponse.json({
    id: user.id,
    email: user.email,
    fullName: user.fullName,
    canManageUsers: user.canManageUsers,
    canManageClients: user.canManageClients,
    canViewAllOrders: user.canViewAllOrders,
    canExportData: user.canExportData,
    canSendEmail: user.canSendEmail,
    canEditOrders: user.canEditOrders,
  });

  response.cookies.set(SESSION_COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: maxAgeSeconds,
  });

  return response;
}
