import Link from "next/link";
import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { buildWorkOrderQuery, type WorkOrderFilters } from "@/lib/work-orders/query";
import { ColumnPicker, type ColumnDef } from "./column-picker";
import { AdminExportButton } from "./admin-export-button";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};

const DIFFICULTY_LABELS: Record<string, string> = {
  OSNOVNA: "Osnovna",
  ZAHTEVNA: "Zahtevna",
};

const CULPRIT_LABELS: Record<string, string> = {
  SLEDENJE: "Sledenje",
  STRANKA: "Stranka",
};

const INSTALLERS = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"];
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

const DEVICE_MODELS = ["FMC130", "FMC150", "FMC650", "FMC880", "TFT100", "OSTALO"];
const DEVICE_MODEL_LABELS: Record<string, string> = {
  OSTALO: "Drugo",
};

function installerLabel(name: string, otherText: string | null) {
  return name === "OSTALO" ? otherText || "Drugo" : name.charAt(0) + name.slice(1).toLowerCase();
}

// Stolpci, ki jih lahko izbereš prek "Izberi stolpce" — ujemajo se z parametri, po katerih lahko
// tudi filtriraš. Ident je vedno prikazan (povezava na nalog), zato ni v tem seznamu.
const COLUMN_DEFS: ColumnDef[] = [
  { key: "datum", label: "Datum" },
  { key: "tip", label: "Tip" },
  { key: "stranka", label: "Stranka" },
  { key: "registrska", label: "Registrska" },
  { key: "imei", label: "IMEI" },
  { key: "zahtevnost", label: "Zahtevnost" },
  { key: "krivec", label: "Krivec" },
  { key: "monter", label: "Monter" },
  { key: "opcije", label: "DIN/ANI/CAN" },
  { key: "naprava", label: "Model naprave" },
];

const DEFAULT_COLUMNS = ["datum", "tip", "stranka", "registrska", "imei"];

// Skalarna polja na WorkOrder se da smiselno urediti z enim ORDER BY — relacije ena-proti-več
// (monter, opcije, naprava) ne, zato za te tri glava stolpca ni klikljiva.
const SORT_KEY_MAP: Record<string, string> = {
  datum: "orderDate",
  tip: "type",
  stranka: "client",
  registrska: "vehiclePlate",
  imei: "imei",
  zahtevnost: "difficulty",
  krivec: "culprit",
};

type PageSearchParams = WorkOrderFilters & { cols?: string };

function selectClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1.5 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100";
}

function sortHref(sortKey: string, filters: PageSearchParams): string {
  const currentSort = filters.sort ?? "orderDate";
  const currentDir = filters.dir ?? "desc";
  const nextDir = currentSort === sortKey && currentDir === "asc" ? "desc" : "asc";

  const params = new URLSearchParams(Object.entries(filters).filter(([, v]) => v) as [string, string][]);
  params.set("sort", sortKey);
  params.set("dir", nextDir);
  return `?${params.toString()}`;
}

function sortIndicator(sortKey: string, filters: PageSearchParams): string {
  const currentSort = filters.sort ?? "orderDate";
  const currentDir = filters.dir ?? "desc";
  if (currentSort !== sortKey) return "";
  return currentDir === "asc" ? " ▲" : " ▼";
}

function ThHeader({ label, sortKey, filters }: { label: string; sortKey?: string; filters: PageSearchParams }) {
  const thClass = "px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400";
  if (!sortKey) return <th className={thClass}>{label}</th>;
  return (
    <th className={thClass}>
      <Link href={sortHref(sortKey, filters)} className="hover:text-gray-700 dark:hover:text-gray-200">
        {label}
        {sortIndicator(sortKey, filters)}
      </Link>
    </th>
  );
}

export default async function NalogiPage({ searchParams }: { searchParams: Promise<PageSearchParams> }) {
  const user = await requireUser();
  const filters = await searchParams;
  const activeCols = filters.cols?.split(",").filter(Boolean) ?? DEFAULT_COLUMNS;

  const [orders, clients] = await Promise.all([
    prisma.workOrder.findMany({
      ...buildWorkOrderQuery(filters, user),
      include: { client: true, installers: true, options: true, deviceModels: true },
      take: 500,
    }),
    prisma.client.findMany({ orderBy: { name: "asc" }, select: { id: true, name: true } }),
  ]);

  const queryString = new URLSearchParams(
    Object.entries(filters).filter(([, v]) => v) as [string, string][]
  ).toString();

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
          {user.canViewAllOrders ? "Vsi delovni nalogi" : "Moji delovni nalogi"}
        </h1>
        <div className="flex gap-2">
          <ColumnPicker columns={COLUMN_DEFS} defaultColumns={DEFAULT_COLUMNS} />
          {user.canExportData && (
            <>
              <a
                href={`/api/nalogi/export${queryString ? `?${queryString}` : ""}`}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
              >
                Izvozi v Excel (filtrirano)
              </a>
              <a
                href="/api/nalogi/export"
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
              >
                Izvozi vse
              </a>
              <AdminExportButton />
            </>
          )}
        </div>
      </div>

      {user.canViewAllOrders && (
        <form
          method="get"
          className="grid grid-cols-2 gap-3 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800 sm:grid-cols-4"
        >
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Datum od
            <input type="date" name="from" defaultValue={filters.from} className={selectClass()} />
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Datum do
            <input type="date" name="to" defaultValue={filters.to} className={selectClass()} />
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Stranka
            <select name="clientId" defaultValue={filters.clientId ?? ""} className={selectClass()}>
              <option value="">vse</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Registrska
            <input name="vehiclePlate" defaultValue={filters.vehiclePlate} className={selectClass()} />
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            IMEI
            <input name="imei" defaultValue={filters.imei} className={selectClass()} />
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Tip
            <select name="type" defaultValue={filters.type ?? ""} className={selectClass()}>
              <option value="">vsi</option>
              {Object.entries(TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Zahtevnost
            <select name="difficulty" defaultValue={filters.difficulty ?? ""} className={selectClass()}>
              <option value="">vse</option>
              <option value="OSNOVNA">Osnovna</option>
              <option value="ZAHTEVNA">Zahtevna</option>
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Krivec
            <select name="culprit" defaultValue={filters.culprit ?? ""} className={selectClass()}>
              <option value="">vsi</option>
              <option value="SLEDENJE">Sledenje</option>
              <option value="STRANKA">Stranka</option>
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Monter
            <select name="installer" defaultValue={filters.installer ?? ""} className={selectClass()}>
              <option value="">vsi</option>
              {INSTALLERS.map((i) => (
                <option key={i} value={i}>
                  {i.charAt(0) + i.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            DIN/ANI/CAN
            <select name="option" defaultValue={filters.option ?? ""} className={selectClass()}>
              <option value="">vsi</option>
              {OPTION_TYPES.map((o) => (
                <option key={o} value={o}>
                  {OPTION_LABELS[o] ?? o}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Model naprave
            <select name="deviceModel" defaultValue={filters.deviceModel ?? ""} className={selectClass()}>
              <option value="">vsi</option>
              {DEVICE_MODELS.map((dm) => (
                <option key={dm} value={dm}>
                  {DEVICE_MODEL_LABELS[dm] ?? dm}
                </option>
              ))}
            </select>
          </label>
          <div className="col-span-2 flex items-end gap-2 sm:col-span-4">
            <button type="submit" className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white">
              Filtriraj
            </button>
            <Link href="/nalogi" className="text-sm text-gray-500 underline dark:text-gray-400">
              Počisti filtre
            </Link>
          </div>
        </form>
      )}

      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
        <table className="min-w-full divide-y divide-gray-200 text-sm dark:divide-gray-700">
          <thead className="bg-gray-50 dark:bg-gray-900">
            <tr>
              <ThHeader label="Ident" sortKey="ident" filters={filters} />
              {COLUMN_DEFS.filter((c) => activeCols.includes(c.key)).map((c) => (
                <ThHeader key={c.key} label={c.label} sortKey={SORT_KEY_MAP[c.key]} filters={filters} />
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {orders.map((o) => (
              <tr key={o.id}>
                <td className="px-4 py-2">
                  <Link href={`/nalogi/${o.id}`} className="text-blue-600 hover:underline dark:text-blue-400">
                    {o.ident}
                  </Link>
                </td>
                {activeCols.includes("datum") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {o.orderDate.toLocaleDateString("sl-SI")}
                  </td>
                )}
                {activeCols.includes("tip") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{TYPE_LABELS[o.type]}</td>
                )}
                {activeCols.includes("stranka") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.client.name}</td>
                )}
                {activeCols.includes("registrska") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.vehiclePlate}</td>
                )}
                {activeCols.includes("imei") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.imei}</td>
                )}
                {activeCols.includes("zahtevnost") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {DIFFICULTY_LABELS[o.difficulty]}
                  </td>
                )}
                {activeCols.includes("krivec") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {o.culprit ? CULPRIT_LABELS[o.culprit] ?? o.culprit : "—"}
                  </td>
                )}
                {activeCols.includes("monter") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {o.installers.map((i) => installerLabel(i.name, i.otherText)).join(", ") || "—"}
                  </td>
                )}
                {activeCols.includes("opcije") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {o.options.map((opt) => OPTION_LABELS[opt.optionType] ?? opt.optionType).join(", ") || "—"}
                  </td>
                )}
                {activeCols.includes("naprava") && (
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">
                    {o.deviceModels.map((dm) => DEVICE_MODEL_LABELS[dm.deviceModel] ?? dm.deviceModel).join(", ") ||
                      "—"}
                  </td>
                )}
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td
                  colSpan={activeCols.length + 1}
                  className="px-4 py-6 text-center text-gray-400 dark:text-gray-500"
                >
                  Ni nalogov.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
