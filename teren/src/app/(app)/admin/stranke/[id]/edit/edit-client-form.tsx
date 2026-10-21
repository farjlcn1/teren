"use client";

import { useActionState } from "react";
import { updateClient } from "../../actions";

export function EditClientForm({
  id,
  name,
  address,
  contactInfo,
}: {
  id: string;
  name: string;
  address: string;
  contactInfo: string;
}) {
  const action = updateClient.bind(null, id);
  const [state, formAction, pending] = useActionState(action, undefined);

  return (
    <form
      action={formAction}
      className="max-w-md space-y-3 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800"
    >
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Ime *</label>
        <input
          name="name"
          defaultValue={name}
          required
          className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Naslov</label>
        <input
          name="address"
          defaultValue={address}
          className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Kontakt</label>
        <input
          name="contactInfo"
          defaultValue={contactInfo}
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
