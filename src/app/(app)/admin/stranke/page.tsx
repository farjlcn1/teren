import Link from "next/link";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AddClientForm } from "./add-client-form";
import { ImportClientsForm } from "./import-form";
import { SyncForm } from "./sync-form";
import { DeleteClientButton } from "./delete-client-button";

const sourceLabels: Record<string, string> = {
  MANUAL: "Ročno",
  IMPORT: "Uvoz",
  SYNC: "Sinhronizacija",
};

export default async function StrankePage() {
  await requirePermission("canManageClients");

  const [clients, syncConfig] = await Promise.all([
    prisma.client.findMany({ orderBy: { name: "asc" } }),
    prisma.clientSyncConfig.findUnique({ where: { id: "default" } }),
  ]);

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Stranke</h1>

      <div className="grid gap-4 lg:grid-cols-2">
        <AddClientForm />
        <ImportClientsForm />
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
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Ime</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Naslov</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Kontakt</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Vir</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {clients.map((c) => (
              <tr key={c.id}>
                <td className="px-4 py-2 text-gray-900 dark:text-gray-100">{c.name}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{c.address ?? "—"}</td>
                <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{c.contactInfo ?? "—"}</td>
                <td className="px-4 py-2 text-gray-500 dark:text-gray-400">{sourceLabels[c.source] ?? c.source}</td>
                <td className="px-4 py-2 text-right">
                  <div className="flex justify-end gap-3">
                    <Link
                      href={`/admin/stranke/${c.id}/edit`}
                      className="text-sm text-blue-600 hover:underline dark:text-blue-400"
                    >
                      Uredi
                    </Link>
                    <DeleteClientButton id={c.id} />
                  </div>
                </td>
              </tr>
            ))}
            {clients.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-400 dark:text-gray-500">
                  Ni strank.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
