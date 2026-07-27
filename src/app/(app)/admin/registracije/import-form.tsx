"use client";

import { useActionState, useRef, useEffect } from "react";
import { importVehiclesXlsx } from "./actions";

export function ImportVehiclesForm() {
  const [state, formAction, pending] = useActionState(importVehiclesXlsx, undefined);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state?.success) formRef.current?.reset();
  }, [state]);

  return (
    <form
      ref={formRef}
      action={formAction}
      className="space-y-3 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800"
    >
      <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Uvoz iz Excel (.xlsx)</h2>
      <p className="text-xs text-gray-500 dark:text-gray-400">
        Prva vrstica naj vsebuje stolpec &quot;Registrska&quot; (ali &quot;Plate&quot;).
      </p>
      <input
        type="file"
        name="file"
        accept=".xlsx"
        required
        className="block w-full text-sm text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-2 file:text-sm dark:text-gray-300 dark:file:bg-gray-700 dark:file:text-gray-100"
      />
      {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      {state?.success && <p className="text-sm text-green-600 dark:text-green-400">{state.success}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Uvažam …" : "Uvozi"}
      </button>
    </form>
  );
}
