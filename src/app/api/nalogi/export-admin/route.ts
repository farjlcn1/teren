import { NextRequest, NextResponse } from "next/server";
import ExcelJS from "exceljs";
import { prisma } from "@/lib/db";
import { requirePermission } from "@/lib/auth/session";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};
const DIFFICULTY_LABELS: Record<string, string> = { OSNOVNA: "Osnovna", ZAHTEVNA: "Zahtevna" };
const CULPRIT_LABELS: Record<string, string> = { SLEDENJE: "Sledenje", STRANKA: "Stranka" };
const DEVICE_MODEL_LABELS: Record<string, string> = { OSTALO: "Drugo" };
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

function installerLabel(name: string, otherText: string | null) {
  return name === "OSTALO" ? otherText || "Drugo" : name.charAt(0) + name.slice(1).toLowerCase();
}

// Posebna postavitev stolpcev za administrativni izvoz — vrstni red po dogovoru, ne po istem
// vzorcu kot navaden izvoz (glej /api/nalogi/export). Stolpci E, N, O, P so namenoma prazni.
// Brez ?from=&to= (npr. star zaznamek) privzeto zajame prejšnji dan.
export async function GET(request: NextRequest) {
  const user = await requirePermission("canExportData");

  const fromParam = request.nextUrl.searchParams.get("from");
  const toParam = request.nextUrl.searchParams.get("to");

  let from: Date;
  let to: Date;
  if (fromParam && toParam) {
    from = new Date(`${fromParam}T00:00:00`);
    to = new Date(`${toParam}T23:59:59`);
  } else {
    const now = new Date();
    from = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
    to = new Date(from.getTime() + 24 * 60 * 60 * 1000 - 1);
  }

  const orders = await prisma.workOrder.findMany({
    where: { orderDate: { gte: from, lte: to }, ...(user.canViewAllOrders ? {} : { createdById: user.id }) },
    orderBy: { orderDate: "asc" },
    include: { client: true, installers: true, options: true, deviceModels: true, createdBy: true },
  });

  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet("Nalogi - prejšnji dan");

  sheet.columns = [
    { header: "Datum", key: "orderDate", width: 12 }, // A
    { header: "Stranka", key: "client", width: 24 }, // B
    { header: "Zahtevnost", key: "difficulty", width: 12 }, // C
    { header: "Tip", key: "type", width: 14 }, // D
    { header: "", key: "_blank1", width: 4 }, // E
    { header: "Znamka", key: "vehicleBrand", width: 14 }, // F
    { header: "Model", key: "vehicleModel", width: 14 }, // G
    { header: "Registrska", key: "vehiclePlate", width: 14 }, // H
    { header: "IMEI", key: "imei", width: 14 }, // I
    { header: "IMEI prej", key: "imeiPrev", width: 14 }, // J
    { header: "DIN/ANI, model naprave, letnik", key: "combined", width: 36 }, // K
    { header: "Krivec", key: "culprit1", width: 14 }, // L
    { header: "Monterji", key: "installers", width: 24 }, // M
    { header: "", key: "_blank2", width: 4 }, // N
    { header: "", key: "_blank3", width: 4 }, // O
    { header: "", key: "_blank4", width: 4 }, // P
    { header: "Krivec", key: "culprit2", width: 14 }, // Q
    ...OPTION_TYPES.map((o) => ({ header: OPTION_LABELS[o] ?? o, key: o, width: 14 })), // R..AI
    { header: "Komentar", key: "comment", width: 30 }, // AJ
    { header: "Status", key: "status", width: 12 }, // AK
    { header: "Izdelal", key: "createdBy", width: 18 }, // AL
    { header: "Podpisano", key: "signedAt", width: 18 }, // AM
    { header: "Poslano", key: "sentAt", width: 18 }, // AN
    { header: "Letnik", key: "vehicleYear", width: 10 }, // AO
    { header: "Ident", key: "ident", width: 16 }, // AP
    { header: "Krivec", key: "culprit3", width: 14 }, // AQ
  ];

  for (const o of orders) {
    const optionsSummary = o.options.map((opt) => OPTION_LABELS[opt.optionType] ?? opt.optionType).join(", ");
    const deviceModelSummary = o.deviceModels
      .map((dm) => DEVICE_MODEL_LABELS[dm.deviceModel] ?? dm.deviceModel)
      .join(", ");
    const culprit = o.culprit ? CULPRIT_LABELS[o.culprit] ?? o.culprit : "";

    const row: Record<string, string | number> = {
      orderDate: o.orderDate.toLocaleDateString("sl-SI"),
      client: o.client.name,
      difficulty: DIFFICULTY_LABELS[o.difficulty] ?? o.difficulty,
      type: TYPE_LABELS[o.type] ?? o.type,
      vehicleBrand: o.vehicleBrand,
      vehicleModel: o.vehicleModel,
      vehiclePlate: o.vehiclePlate,
      imei: o.imei,
      imeiPrev: o.imeiPrev ?? "",
      combined: [optionsSummary, deviceModelSummary, o.vehicleYear].filter(Boolean).join(" / "),
      culprit1: culprit,
      installers: o.installers.map((i) => installerLabel(i.name, i.otherText)).join(", "),
      culprit2: culprit,
      comment: o.comment ?? "",
      status: o.status,
      createdBy: o.createdBy.fullName,
      signedAt: o.signedAt ? o.signedAt.toLocaleString("sl-SI") : "",
      sentAt: o.sentAt ? o.sentAt.toLocaleString("sl-SI") : "",
      vehicleYear: o.vehicleYear,
      ident: o.ident,
      culprit3: culprit,
    };
    for (const opt of OPTION_TYPES) {
      const found = o.options.find((x) => x.optionType === opt);
      row[opt] = found ? found.comment || "DA" : "";
    }
    sheet.addRow(row);
  }

  sheet.getRow(1).font = { bold: true };

  const buffer = await workbook.xlsx.writeBuffer();
  const fromStr = from.toISOString().slice(0, 10);
  const toStr = to.toISOString().slice(0, 10);
  const fileDateStr = fromStr === toStr ? fromStr : `${fromStr}_${toStr}`;

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "Content-Disposition": `attachment; filename="nalogi-admin-${fileDateStr}.xlsx"`,
    },
  });
}
