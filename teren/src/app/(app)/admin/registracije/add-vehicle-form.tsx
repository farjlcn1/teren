"use client";

import { useActionState, useRef, useEffect } from "react";
import { createVehicle } from "./actions";

export function AddVehicleForm() {
  const [state, formAction, pending] = useActionState(createVehicle, undefined);
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
      <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Dodaj registrsko št. ročno</h2>
      <input
        name="plate"
        placeholder="npr. LJ AB-123"
        required
        className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100 sm:max-w-xs"
      />
      {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      {state?.success && <p className="text-sm text-green-600 dark:text-green-400">{state.success}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Dodajam …" : "Dodaj"}
      </button>
    </form>
  );
}
