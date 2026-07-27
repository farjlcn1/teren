"use client";

import { useActionState } from "react";
import { sendOrderEmail } from "./actions";

export function SendEmailForm({ workOrderId, clientContact }: { workOrderId: string; clientContact: string }) {
  const action = sendOrderEmail.bind(null, workOrderId);
  const [state, formAction, pending] = useActionState(action, undefined);

  return (
    <form action={formAction} className="flex flex-wrap items-end gap-2">
      <label className="text-sm text-gray-700 dark:text-gray-300">
        Pošlji nalog na email
        <input
          type="email"
          name="email"
          defaultValue={clientContact}
          required
          placeholder="naslovnik@example.com"
          className="mt-1 block rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
      </label>
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-gray-900 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-gray-700"
      >
        {pending ? "Pošiljam …" : "Pošlji"}
      </button>
      {state?.error && <p className="w-full text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      {state?.success && <p className="w-full text-sm text-green-600 dark:text-green-400">{state.success}</p>}
    </form>
  );
}
