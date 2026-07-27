"use client";

import { useActionState } from "react";
import { updateVehicle } from "../../actions";

export function EditVehicleForm({ id, plate }: { id: string; plate: string }) {
  const action = updateVehicle.bind(null, id);
  const [state, formAction, pending] = useActionState(action, undefined);

  return (
    <form
      action={formAction}
      className="max-w-md space-y-3 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800"
    >
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Registrska št. *</label>
        <input
          name="plate"
          defaultValue={plate}
          required
          className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
      </div>
      {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Shranjujem …" : "Shrani"}
      </button>
    </form>
  );
}
