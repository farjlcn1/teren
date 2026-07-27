import Link from "next/link";
import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { buildWorkOrderQuery, type WorkOrderFilters } from "@/lib/work-orders/query";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};

const INSTALLERS = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"];
const OPTION_TYPES = ["DIN1", "DIN2", "DIN3", "DIN4", "DIN5", "ANI1", "ANI2", "ANI3", "ALL_CAN", "FMSCAN", "TACHO"];
const OPTION_LABELS: Record<string, string> = { ALL_CAN: "ALL CAN" };

const SORT_OPTIONS = [
  { value: "orderDate", label: "Datum" },
  { value: "ident", label: "Ident" },
  { value: "vehiclePlate", label: "Registrska" },
  { value: "imei", label: "IMEI" },
  { value: "client", label: "Stranka" },
  { value: "type", label: "Tip" },
];

function selectClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1.5 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100";
}

export default async function NalogiPage({ searchParams }: { searchParams: Promise<WorkOrderFilters> }) {
  const user = await requireUser();
  const filters = await searchParams;

  const [orders, clients] = await Promise.all([
    prisma.workOrder.findMany({
      ...buildWorkOrderQuery(filters, user),
      include: { client: true },
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
        {user.canExportData && (
          <div className="flex gap-2">
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
          </div>
        )}
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
            Razvrsti po
            <select name="sort" defaultValue={filters.sort ?? "orderDate"} className={selectClass()}>
              {SORT_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
            Smer
            <select name="dir" defaultValue={filters.dir ?? "desc"} className={selectClass()}>
              <option value="desc">padajoče</option>
              <option value="asc">naraščajoče</option>
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
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Ident</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Datum</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Tip</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Stranka</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Registrska</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">IMEI</th>
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
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.orderDate.toLocaleDateString("sl-SI")}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{TYPE_LABELS[o.type]}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.client.name}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.vehiclePlate}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{o.imei}</td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-gray-400 dark:text-gray-500">
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
