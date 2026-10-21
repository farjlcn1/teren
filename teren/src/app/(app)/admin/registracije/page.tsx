import Link from "next/link";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AddVehicleForm } from "./add-vehicle-form";
import { ImportVehiclesForm } from "./import-form";
import { SyncForm } from "./sync-form";
import { DeleteVehicleButton } from "./delete-vehicle-button";

const sourceLabels: Record<string, string> = {
  MANUAL: "Ročno",
  IMPORT: "Uvoz",
  SYNC: "Sinhronizacija",
};

export default async function RegistracijePage() {
  await requirePermission("canManageVehicles");

  const [vehicles, syncConfig] = await Promise.all([
    prisma.vehicle.findMany({ orderBy: { plate: "asc" } }),
    prisma.vehicleSyncConfig.findUnique({ where: { id: "default" } }),
  ]);

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Registrske številke</h1>

      <div className="grid gap-4 lg:grid-cols-2">
        <AddVehicleForm />
        <ImportVehiclesForm />
      </div>

      <SyncForm
        apiUrl={syncConfig?.apiUrl ?? ""}
        hasToken={Boolean(syncConfig?.apiToken)}
        lastSyncAt={syncConfig?.lastSyncAt ? syncConfig.lastSyncAt.toLocaleString("sl-SI") : null}
        lastSyncStatus={syncConfig?.lastSyncStatus ?? null}
      />

      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
        <table className="min-w-full divide-y divide-gray-200 text-sm dark:divide-gray-700">
          <thead className="bg-gray-50 dark:bg-gray-900">
            <tr>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Registrska</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Vir</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td className="px-4 py-2 text-gray-900 dark:text-gray-100">{v.plate}</td>
                <td className="px-4 py-2 text-gray-500 dark:text-gray-400">{sourceLabels[v.source] ?? v.source}</td>
                <td className="px-4 py-2 text-right">
                  <div className="flex justify-end gap-3">
                    <Link
                      href={`/admin/registracije/${v.id}/edit`}
                      className="text-sm text-blue-600 hover:underline dark:text-blue-400"
                    >
                      Uredi
                    </Link>
                    <DeleteVehicleButton id={v.id} />
                  </div>
                </td>
              </tr>
            ))}
            {vehicles.length === 0 && (
              <tr>
                <td colSpan={3} className="px-4 py-6 text-center text-gray-400 dark:text-gray-500">
                  Ni registrskih številk.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
