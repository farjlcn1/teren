"use client";

import { updateUserPermissions } from "../../actions";

const PERMISSION_FIELDS = [
  { key: "canManageUsers", label: "Upravljanje uporabnikov" },
  { key: "canManageClients", label: "Upravljanje strank" },
  { key: "canManageVehicles", label: "Upravljanje registrskih št." },
  { key: "canViewAllOrders", label: "Pregled vseh nalogov" },
  { key: "canEditOrders", label: "Urejanje nalogov" },
  { key: "canExportData", label: "Izvoz podatkov" },
  { key: "canSendEmail", label: "Pošiljanje po mailu" },
] as const;

export function PermissionsForm({
  userId,
  isActive,
  permissions,
}: {
  userId: string;
  isActive: boolean;
  permissions: Record<string, boolean>;
}) {
  const action = updateUserPermissions.bind(null, userId);

  return (
    <form
      action={action}
      className="space-y-4 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800"
    >
      <label className="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-gray-100">
        <input type="checkbox" name="isActive" defaultChecked={isActive} />
        Aktiven uporabnik
      </label>

      <div className="space-y-2">
        {PERMISSION_FIELDS.map((p) => (
          <label key={p.key} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
            <input type="checkbox" name={p.key} defaultChecked={permissions[p.key]} />
            {p.label}
          </label>
        ))}
      </div>

      <button type="submit" className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white">
        Shrani pravice
      </button>
    </form>
  );
}
