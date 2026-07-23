import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import type { InstallerName, OptionType } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { OPTION_TYPE_VALUES } from "@/lib/constants";

async function loadOrderWithAccessCheck(id: string, userId: string, canViewAllOrders: boolean) {
  const order = await prisma.workOrder.findUnique({
    where: { id },
    include: {
      client: true,
      installers: true,
      options: true,
      photos: true,
      createdBy: { select: { id: true, fullName: true } },
    },
  });

  if (!order) return { order: null, allowed: false };
  const allowed = canViewAllOrders || order.createdById === userId;
  return { order, allowed };
}

export async function GET(_request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });

  const { id } = await params;
  const { order, allowed } = await loadOrderWithAccessCheck(id, user.id, user.canViewAllOrders);

  if (!order) return NextResponse.json({ error: "Nalog ne obstaja." }, { status: 404 });
  if (!allowed) return NextResponse.json({ error: "Nimate dostopa." }, { status: 403 });

  return NextResponse.json(order);
}

const installerSchema = z.object({
  name: z.enum(["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"]),
  otherText: z.string().optional(),
});

const optionSchema = z.object({
  optionType: z.enum(OPTION_TYPE_VALUES as [string, ...string[]]),
  comment: z.string().optional(),
});

const editWorkOrderSchema = z
  .object({
    type: z.enum(["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"]),
    difficulty: z.enum(["OSNOVNA", "ZAHTEVNA"]),
    clientId: z.string().min(1),
    installers: z.array(installerSchema).min(1),
    vehiclePlate: z.string().min(1),
    vehicleBrand: z.string().min(1),
    vehicleModel: z.string().min(1),
    vehicleYear: z.string().min(1),
    imei: z.string().regex(/^\d{10}$/),
    imeiPrev: z.string().regex(/^\d{10}$/).optional(),
    options: z.array(optionSchema),
    comment: z.string().optional(),
  })
  .refine((data) => data.type !== "INTERVENCIJA" || !!data.imeiPrev, {
    message: "IMEI prej je obvezen pri intervenciji.",
    path: ["imeiPrev"],
  });

export async function PATCH(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canEditOrders) {
    return NextResponse.json({ error: "Nimate pravice za urejanje nalogov." }, { status: 403 });
  }

  const { id } = await params;
  const existing = await prisma.workOrder.findUnique({ where: { id } });
  if (!existing) return NextResponse.json({ error: "Nalog ne obstaja." }, { status: 404 });

  const body = await request.json().catch(() => null);
  const parsed = editWorkOrderSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Neveljavni podatki." }, { status: 400 });
  }

  const data = parsed.data;
  const client = await prisma.client.findUnique({ where: { id: data.clientId } });
  if (!client) return NextResponse.json({ error: "Izbrana stranka ne obstaja." }, { status: 400 });

  const updated = await prisma.$transaction(async (tx) => {
    await tx.workOrderInstaller.deleteMany({ where: { workOrderId: id } });
    await tx.workOrderOption.deleteMany({ where: { workOrderId: id } });

    return tx.workOrder.update({
      where: { id },
      data: {
        type: data.type,
        difficulty: data.difficulty,
        clientId: data.clientId,
        vehiclePlate: data.vehiclePlate,
        vehicleBrand: data.vehicleBrand,
        vehicleModel: data.vehicleModel,
        vehicleYear: data.vehicleYear,
        imei: data.imei,
        imeiPrev: data.type === "INTERVENCIJA" ? data.imeiPrev : null,
        comment: data.comment,
        installers: {
          create: data.installers.map((i) => ({ name: i.name as InstallerName, otherText: i.otherText })),
        },
        options: {
          create: data.options.map((o) => ({ optionType: o.optionType as OptionType, comment: o.comment })),
        },
      },
      include: { client: true, installers: true, options: true, photos: true },
    });
  });

  return NextResponse.json(updated);
}
