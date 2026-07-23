import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { hashPassword, validatePassword } from "@/lib/password";

const updateUserSchema = z.object({
  fullName: z.string().min(1).optional(),
  isActive: z.boolean().optional(),
  newPassword: z.string().optional(),
  canManageUsers: z.boolean().optional(),
  canManageClients: z.boolean().optional(),
  canViewAllOrders: z.boolean().optional(),
  canExportData: z.boolean().optional(),
  canSendEmail: z.boolean().optional(),
  canEditOrders: z.boolean().optional(),
});

export async function PATCH(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const sessionUser = await getSessionUser();
  if (!sessionUser) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!sessionUser.canManageUsers) {
    return NextResponse.json({ error: "Nimate pravice za upravljanje uporabnikov." }, { status: 403 });
  }

  const { id } = await params;
  const target = await prisma.user.findUnique({ where: { id } });
  if (!target) return NextResponse.json({ error: "Uporabnik ne obstaja." }, { status: 404 });

  const body = await request.json().catch(() => null);
  const parsed = updateUserSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Neveljavni podatki." }, { status: 400 });
  }

  const data = parsed.data;

  // prepreci, da bi zadnjemu uporabniku s pravico "canManageUsers" to pravico odvzeli/deaktivirali sami sebe
  const removingManageUsers = data.canManageUsers === false && target.canManageUsers;
  const deactivating = data.isActive === false && target.isActive;
  if (removingManageUsers || deactivating) {
    const otherAdmins = await prisma.user.count({
      where: { canManageUsers: true, isActive: true, id: { not: id } },
    });
    if (otherAdmins === 0) {
      return NextResponse.json(
        { error: "To je zadnji aktivni uporabnik s pravico upravljanja uporabnikov — ni ga mogoče onemogočiti." },
        { status: 400 }
      );
    }
  }

  let passwordHash: string | undefined;
  if (data.newPassword) {
    const passwordErrors = validatePassword(data.newPassword);
    if (passwordErrors.length > 0) {
      return NextResponse.json({ error: passwordErrors.join(" ") }, { status: 400 });
    }
    passwordHash = await hashPassword(data.newPassword);
  }

  const updated = await prisma.user.update({
    where: { id },
    data: {
      fullName: data.fullName,
      isActive: data.isActive,
      canManageUsers: data.canManageUsers,
      canManageClients: data.canManageClients,
      canViewAllOrders: data.canViewAllOrders,
      canExportData: data.canExportData,
      canSendEmail: data.canSendEmail,
      canEditOrders: data.canEditOrders,
      ...(passwordHash ? { passwordHash } : {}),
    },
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
    },
  });

  return NextResponse.json(updated);
}
