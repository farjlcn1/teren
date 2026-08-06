import { NextRequest, NextResponse } from "next/server";
import ExcelJS from "exceljs";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";
import { buildWorkOrderQuery, type WorkOrderFilters } from "@/lib/work-orders/query";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};
const DIFFICULTY_LABELS: Record<string, string> = { OSNOVNA: "Osnovna", ZAHTEVNA: "Zahtevna" };
const OPTION_TYPES = [
  "DIN1",
  "DIN2",
  "DIN3",
  "DIN4",
  "DIN5",
  "ANI1",
  "ANI2",
  "ANI3",
  "ALL_CAN",
  "FMSCAN",
  "TACHO",
  "WIRE_TEMP1",
  "WIRE_TEMP2",
  "WIRE_TEMP3",
  "ID_KEY",
  "RFID_125",
  "RFID_1356",
  "BUZZER",
];
const OPTION_LABELS: Record<string, string> = {
  DIN1: "DIN1 (IGN)",
  ALL_CAN: "ALL CAN",
  WIRE_TEMP1: "1 Wire Temp (1)",
  WIRE_TEMP2: "1 Wire Temp (2)",
  WIRE_TEMP3: "1 Wire Temp (3)",
  ID_KEY: "ID",
  RFID_125: "RFID 125 kHz",
  RFID_1356: "RFID 13,56 MHz",
  BUZZER: "Brenčač",
};

export async function GET(request: NextRequest) {
  const user = await requirePermission("canExportData");

  const filters = Object.fromEntries(request.nextUrl.searchParams) as WorkOrderFilters;
  const { where, orderBy } = buildWorkOrderQuery(filters, user);

  const orders = await prisma.workOrder.findMany({
    where,
    orderBy,
    include: { client: true, installers: true, options: true, createdBy: true },
  });

  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet("Delovni nalogi");

  sheet.columns = [
    { header: "Ident", key: "ident", width: 16 },
    { header: "Datum", key: "orderDate", width: 12 },
    { header: "Tip", key: "type", width: 14 },
    { header: "Zahtevnost", key: "difficulty", width: 12 },
    { header: "Stranka", key: "client", width: 24 },
    { header: "Monterji", key: "installers", width: 24 },
    { header: "Registrska", key: "vehiclePlate", width: 14 },
    { header: "Znamka", key: "vehicleBrand", width: 14 },
    { header: "Model", key: "vehicleModel", width: 14 },
    { header: "Letnik", key: "vehicleYear", width: 10 },
    { header: "IMEI", key: "imei", width: 14 },
    { header: "IMEI prej", key: "imeiPrev", width: 14 },
    ...OPTION_TYPES.map((o) => ({ header: OPTION_LABELS[o] ?? o, key: o, width: 14 })),
    { header: "Komentar", key: "comment", width: 30 },
    { header: "Status", key: "status", width: 12 },
    { header: "Izdelal", key: "createdBy", width: 18 },
    { header: "Podpisano", key: "signedAt", width: 18 },
    { header: "Poslano", key: "sentAt", width: 18 },
  ];

  for (const o of orders) {
    const row: Record<string, string | number> = {
      ident: o.ident,
      orderDate: o.orderDate.toLocaleDateString("sl-SI"),
      type: TYPE_LABELS[o.type] ?? o.type,
      difficulty: DIFFICULTY_LABELS[o.difficulty] ?? o.difficulty,
      client: o.client.name,
      installers: o.installers
        .map((i) => (i.name === "OSTALO" ? i.otherText ?? "Ostalo" : i.name.charAt(0) + i.name.slice(1).toLowerCase()))
        .join(", "),
      vehiclePlate: o.vehiclePlate,
      vehicleBrand: o.vehicleBrand,
      vehicleModel: o.vehicleModel,
      vehicleYear: o.vehicleYear,
      imei: o.imei,
      imeiPrev: o.imeiPrev ?? "",
      comment: o.comment ?? "",
      status: o.status,
      createdBy: o.createdBy.fullName,
      signedAt: o.signedAt ? o.signedAt.toLocaleString("sl-SI") : "",
      sentAt: o.sentAt ? o.sentAt.toLocaleString("sl-SI") : "",
    };
    for (const opt of OPTION_TYPES) {
      const found = o.options.find((x) => x.optionType === opt);
      row[opt] = found ? found.comment || "DA" : "";
    }
    sheet.addRow(row);
  }

  sheet.getRow(1).font = { bold: true };

  const buffer = await workbook.xlsx.writeBuffer();

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "Content-Disposition": `attachment; filename="delovni-nalogi-${new Date().toISOString().slice(0, 10)}.xlsx"`,
    },
  });
}
