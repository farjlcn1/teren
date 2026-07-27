import Link from "next/link";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AddUserForm } from "./add-user-form";

export default async function UporabnikiPage() {
  await requirePermission("canManageUsers");

  const users = await prisma.user.findMany({ orderBy: { createdAt: "asc" } });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Uporabniki</h1>

      <AddUserForm />

      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
        <table className="min-w-full divide-y divide-gray-200 text-sm dark:divide-gray-700">
          <thead className="bg-gray-50 dark:bg-gray-900">
            <tr>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Ime</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Email</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Status</th>
              <th className="px-4 py-2 text-left font-medium text-gray-500 dark:text-gray-400">Pravice</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
            {users.map((u) => {
              const perms = [
                u.canManageUsers && "Uporabniki",
                u.canManageClients && "Stranke",
                u.canManageVehicles && "Registracije",
                u.canViewAllOrders && "Vsi nalogi",
                u.canEditOrders && "Urejanje",
                u.canExportData && "Izvoz",
                u.canSendEmail && "Email",
              ].filter(Boolean);
              return (
                <tr key={u.id}>
                  <td className="px-4 py-2 text-gray-900 dark:text-gray-100">{u.fullName}</td>
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{u.email}</td>
                  <td className="px-4 py-2">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs ${
                        u.isActive
                          ? "bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300"
                          : "bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400"
                      }`}
                    >
                      {u.isActive ? "Aktiven" : "Onemogočen"}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-gray-600 dark:text-gray-300">{perms.join(", ") || "—"}</td>
                  <td className="px-4 py-2 text-right">
                    <Link
                      href={`/admin/uporabniki/${u.id}/edit`}
                      className="text-sm text-blue-600 hover:underline dark:text-blue-400"
                    >
                      Uredi
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
