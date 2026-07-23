import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { Prisma, InstallerName, OptionType } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { generateIdent } from "@/lib/ident";
import { saveCompressedPhoto, saveSignature } from "@/lib/uploads";
import { OPTION_TYPE_VALUES } from "@/lib/constants";
import { buildWorkOrderWhere, SORTABLE_FIELDS } from "@/lib/work-order-filters";

const installerSchema = z.object({
  name: z.enum(["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"]),
  otherText: z.string().optional(),
});

const optionSchema = z.object({
  optionType: z.enum(OPTION_TYPE_VALUES as [string, ...string[]]),
  comment: z.string().optional(),
});

const createWorkOrderSchema = z
  .object({
    type: z.enum(["MONTAZA", "DEMONTAZA", "INTERVENCIJA", "PREMONTAZA", "OSTALO"]),
    difficulty: z.enum(["OSNOVNA", "ZAHTEVNA"]),
    clientId: z.string().min(1, "Stranka je obvezna."),
    installers: z.array(installerSchema).min(1, "Izberite vsaj enega monterja."),
    vehiclePlate: z.string().min(1, "Registrska št. je obvezna."),
    vehicleBrand: z.string().min(1, "Znamka vozila je obvezna."),
    vehicleModel: z.string().min(1, "Model vozila je obvezen."),
    vehicleYear: z.string().min(1, "Letnik vozila je obvezen."),
    imei: z.string().regex(/^\d{10}$/, "IMEI mora vsebovati natanko 10 številk."),
    imeiPrev: z.string().regex(/^\d{10}$/, "IMEI prej mora vsebovati natanko 10 številk.").optional(),
    options: z.array(optionSchema),
    comment: z.string().optional(),
  })
  .refine((data) => data.type !== "INTERVENCIJA" || !!data.imeiPrev, {
    message: "IMEI prej je obvezen pri intervenciji.",
    path: ["imeiPrev"],
  });

export async function POST(request: NextRequest) {
  const user = await getSessionUser();
  if (!user) {
    return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  }

  const formData = await request.formData();

  const rawFields = {
    type: formData.get("type")?.toString() ?? "",
    difficulty: formData.get("difficulty")?.toString() ?? "",
    clientId: formData.get("clientId")?.toString() ?? "",
    vehiclePlate: formData.get("vehiclePlate")?.toString() ?? "",
    vehicleBrand: formData.get("vehicleBrand")?.toString() ?? "",
    vehicleModel: formData.get("vehicleModel")?.toString() ?? "",
    vehicleYear: formData.get("vehicleYear")?.toString() ?? "",
    imei: formData.get("imei")?.toString() ?? "",
    imeiPrev: formData.get("imeiPrev")?.toString() || undefined,
    comment: formData.get("comment")?.toString() || undefined,
  };

  let installers: unknown;
  let options: unknown;
  try {
    installers = JSON.parse(formData.get("installers")?.toString() ?? "[]");
    options = JSON.parse(formData.get("options")?.toString() ?? "[]");
  } catch {
    return NextResponse.json({ error: "Neveljaven format monterjev/opcij." }, { status: 400 });
  }

  const parsed = createWorkOrderSchema.safeParse({ ...rawFields, installers, options });
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Neveljavni podatki." }, { status: 400 });
  }

  const client = await prisma.client.findUnique({ where: { id: parsed.data.clientId } });
  if (!client) {
    return NextResponse.json({ error: "Izbrana stranka ne obstaja." }, { status: 400 });
  }

  const signatureFile = formData.get("signature");
  if (!(signatureFile instanceof File) || signatureFile.size === 0) {
    return NextResponse.json({ error: "Podpis stranke je obvezen." }, { status: 400 });
  }

  const photoFiles = formData.getAll("photos").filter((f): f is File => f instanceof File && f.size > 0);

  const data = parsed.data;
  const now = new Date();

  const created = await prisma.$transaction(async (tx) => {
    const ident = await generateIdent(tx, now);

    return tx.workOrder.create({
      data: {
        ident,
        orderDate: now,
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
        createdById: user.id,
        installers: {
          create: data.installers.map((i) => ({ name: i.name as InstallerName, otherText: i.otherText })),
        },
        options: {
          create: data.options.map((o) => ({ optionType: o.optionType as OptionType, comment: o.comment })),
        },
      },
    });
  });

  // Shranjevanje datotek na disk (izven transakcije, ker gre za pocasnejsi I/O)
  const signatureUrl = await saveSignature(created.id, signatureFile);

  for (let i = 0; i < photoFiles.length; i++) {
    const fileUrl = await saveCompressedPhoto(created.id, i, photoFiles[i]);
    await prisma.workOrderPhoto.create({ data: { workOrderId: created.id, fileUrl } });
  }

  await prisma.workOrder.update({ where: { id: created.id }, data: { signatureUrl } });

  return NextResponse.json({ id: created.id, ident: created.ident }, { status: 201 });
}

export async function GET(request: NextRequest) {
  const user = await getSessionUser();
  if (!user) {
    return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  }

  const params = request.nextUrl.searchParams;

  const sortBy = params.get("sortBy") || "orderDate";
  const sortDir = params.get("sortDir") === "asc" ? "asc" : "desc";
  const page = Math.max(1, Number(params.get("page") ?? "1") || 1);
  const pageSize = Math.min(100, Math.max(1, Number(params.get("pageSize") ?? "25") || 25));

  const where = buildWorkOrderWhere(params, user);

  const orderBy: Prisma.WorkOrderOrderByWithRelationInput = SORTABLE_FIELDS.has(sortBy)
    ? ({ [sortBy]: sortDir } as Prisma.WorkOrderOrderByWithRelationInput)
    : { orderDate: "desc" };

  const [items, total] = await Promise.all([
    prisma.workOrder.findMany({
      where,
      orderBy,
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: {
        client: true,
        installers: true,
        options: true,
        photos: true,
        createdBy: { select: { id: true, fullName: true } },
      },
    }),
    prisma.workOrder.count({ where }),
  ]);

  return NextResponse.json({ items, total, page, pageSize });
}
