import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { hashPassword, validatePassword } from "@/lib/password";

const permissionsShape = {
  canManageUsers: z.boolean().default(false),
  canManageClients: z.boolean().default(false),
  canViewAllOrders: z.boolean().default(false),
  canExportData: z.boolean().default(false),
  canSendEmail: z.boolean().default(false),
  canEditOrders: z.boolean().default(false),
};

const createUserSchema = z.object({
  email: z.string().email(),
  password: z.string(),
  fullName: z.string().min(1, "Ime in priimek sta obvezna."),
  ...permissionsShape,
});

export async function GET() {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canManageUsers) {
    return NextResponse.json({ error: "Nimate pravice za upravljanje uporabnikov." }, { status: 403 });
  }

  const users = await prisma.user.findMany({
    orderBy: { fullName: "asc" },
    select: {
      id: true,
      email: true,
      fullName: true,
      isActive: true,
      canManageUsers: true,
      canManageClients: true,
      canViewAllOrders: true,
      canExportData: true,
      canSendEmail: true,
      canEditOrders: true,
      createdAt: true,
    },
  });

  return NextResponse.json(users);
}

export async function POST(request: NextRequest) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canManageUsers) {
    return NextResponse.json({ error: "Nimate pravice za upravljanje uporabnikov." }, { status: 403 });
  }

  const body = await request.json().catch(() => null);
  const parsed = createUserSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Neveljavni podatki." }, { status: 400 });
  }

  const passwordErrors = validatePassword(parsed.data.password);
  if (passwordErrors.length > 0) {
    return NextResponse.json({ error: passwordErrors.join(" ") }, { status: 400 });
  }

  const existing = await prisma.user.findUnique({ where: { email: parsed.data.email.toLowerCase() } });
  if (existing) {
    return NextResponse.json({ error: "Uporabnik s tem e-mailom že obstaja." }, { status: 400 });
  }

  const passwordHash = await hashPassword(parsed.data.password);

  const created = await prisma.user.create({
    data: {
      email: parsed.data.email.toLowerCase(),
      passwordHash,
      fullName: parsed.data.fullName,
      canManageUsers: parsed.data.canManageUsers,
      canManageClients: parsed.data.canManageClients,
      canViewAllOrders: parsed.data.canViewAllOrders,
      canExportData: parsed.data.canExportData,
      canSendEmail: parsed.data.canSendEmail,
      canEditOrders: parsed.data.canEditOrders,
    },
    select: { id: true, email: true, fullName: true },
  });

  return NextResponse.json(created, { status: 201 });
}
