"use client";

import { useActionState } from "react";
import { saveSyncConfig, runSyncNow } from "./actions";

export function SyncForm({
  apiUrl,
  hasToken,
  lastSyncAt,
  lastSyncStatus,
}: {
  apiUrl: string;
  hasToken: boolean;
  lastSyncAt: string | null;
  lastSyncStatus: string | null;
}) {
  const [saveState, saveAction, savePending] = useActionState(saveSyncConfig, undefined);
  const [syncState, syncAction, syncPending] = useActionState(async () => runSyncNow(), undefined);

  return (
    <div className="space-y-4 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
      <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Sinhronizacija z zunanjo povezavo</h2>
      <p className="text-xs text-gray-500 dark:text-gray-400">
        Generična povezava: pričakuje se GET zahteva, ki vrne JSON seznam objektov z polji externalId, name, address,
        contactInfo. Prilagodi ob znanem zunanjem sistemu.
      </p>

      <form action={saveAction} className="grid gap-3 sm:grid-cols-2">
        <input
          name="apiUrl"
          placeholder="https://..."
          defaultValue={apiUrl}
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100 sm:col-span-2"
        />
        <input
          name="apiToken"
          type="password"
          placeholder={hasToken ? "Žeton je nastavljen (vnesi za spremembo)" : "API žeton (neobvezno)"}
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100 sm:col-span-2"
        />
        {saveState?.error && <p className="text-sm text-red-600 dark:text-red-400 sm:col-span-2">{saveState.error}</p>}
        {saveState?.success && (
          <p className="text-sm text-green-600 dark:text-green-400 sm:col-span-2">{saveState.success}</p>
        )}
        <button
          type="submit"
          disabled={savePending}
          className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 sm:w-fit"
        >
          {savePending ? "Shranjujem …" : "Shrani nastavitve"}
        </button>
      </form>

      <div className="border-t border-gray-100 pt-3 dark:border-gray-700">
        <p className="mb-2 text-xs text-gray-500 dark:text-gray-400">
          Zadnja sinhronizacija: {lastSyncAt ?? "še nikoli"}
          {lastSyncStatus ? ` — ${lastSyncStatus}` : ""}
        </p>
        <form action={syncAction}>
          {syncState?.error && <p className="mb-2 text-sm text-red-600 dark:text-red-400">{syncState.error}</p>}
          {syncState?.success && <p className="mb-2 text-sm text-green-600 dark:text-green-400">{syncState.success}</p>}
          <button
            type="submit"
            disabled={syncPending || !apiUrl}
            className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-gray-700"
          >
            {syncPending ? "Sinhroniziram …" : "Sinhroniziraj zdaj"}
          </button>
        </form>
      </div>
    </div>
  );
}
