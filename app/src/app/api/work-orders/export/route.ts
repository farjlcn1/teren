import { NextRequest, NextResponse } from "next/server";
import ExcelJS from "exceljs";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { buildWorkOrderWhere } from "@/lib/work-order-filters";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
  OPTION_TYPE_VALUES,
  WORK_ORDER_STATUS_LABELS,
} from "@/lib/constants";

export async function GET(request: NextRequest) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canExportData) {
    return NextResponse.json({ error: "Nimate pravice za izvoz podatkov." }, { status: 403 });
  }

  const where = buildWorkOrderWhere(request.nextUrl.searchParams, user);

  const orders = await prisma.workOrder.findMany({
    where,
    orderBy: { orderDate: "desc" },
    take: 10000,
    include: { client: true, installers: true, options: true, photos: true, createdBy: true },
  });

  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet("Delovni nalogi");

  const baseColumns = [
    { header: "Ident", key: "ident", width: 16 },
    { header: "Datum", key: "datum", width: 12 },
    { header: "Stranka", key: "stranka", width: 22 },
    { header: "Tip naloga", key: "tip", width: 14 },
    { header: "Zahtevnost", key: "zahtevnost", width: 12 },
    { header: "Monterji", key: "monterji", width: 22 },
    { header: "Registrska št.", key: "registrska", width: 14 },
    { header: "Znamka", key: "znamka", width: 14 },
    { header: "Model", key: "model", width: 14 },
    { header: "Letnik", key: "letnik", width: 10 },
    { header: "IMEI", key: "imei", width: 14 },
    { header: "IMEI prej", key: "imeiPrev", width: 14 },
  ];

  const optionColumns = OPTION_TYPE_VALUES.map((key) => ({
    header: OPTION_TYPE_LABELS[key],
    key: `opt_${key}`,
    width: 14,
  }));

  const tailColumns = [
    { header: "Komentar", key: "komentar", width: 30 },
    { header: "Status", key: "status", width: 12 },
    { header: "Izdelal", key: "izdelal", width: 18 },
    { header: "Št. slik", key: "stSlik", width: 10 },
  ];

  sheet.columns = [...baseColumns, ...optionColumns, ...tailColumns];
  sheet.getRow(1).font = { bold: true };

  for (const order of orders) {
    const row: Record<string, string | number> = {
      ident: order.ident,
      datum: order.orderDate.toLocaleDateString("sl-SI"),
      stranka: order.client.name,
      tip: WORK_ORDER_TYPE_LABELS[order.type],
      zahtevnost: DIFFICULTY_LABELS[order.difficulty],
      monterji: order.installers
        .map((i) => (i.name === "OSTALO" ? i.otherText : INSTALLER_NAME_LABELS[i.name]))
        .join(", "),
      registrska: order.vehiclePlate,
      znamka: order.vehicleBrand,
      model: order.vehicleModel,
      letnik: order.vehicleYear,
      imei: order.imei,
      imeiPrev: order.imeiPrev || "",
      komentar: order.comment || "",
      status: WORK_ORDER_STATUS_LABELS[order.status],
      izdelal: order.createdBy.fullName,
      stSlik: order.photos.length,
    };

    for (const key of OPTION_TYPE_VALUES) {
      const opt = order.options.find((o) => o.optionType === key);
      row[`opt_${key}`] = opt ? opt.comment || "DA" : "";
    }

    sheet.addRow(row);
  }

  const buffer = await workbook.xlsx.writeBuffer();

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "Content-Disposition": `attachment; filename="delovni-nalogi.xlsx"`,
    },
  });
}
