"use client";

import { useActionState, useRef, useEffect } from "react";
import { createUser } from "./actions";

export function AddUserForm() {
  const [state, formAction, pending] = useActionState(createUser, undefined);
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
      <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Dodaj uporabnika</h2>
      <div className="grid gap-3 sm:grid-cols-3">
        <input
          name="email"
          type="email"
          placeholder="email@primer.si"
          required
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
        <input
          name="fullName"
          placeholder="Ime in priimek"
          required
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
        <input
          name="password"
          type="password"
          placeholder="Geslo"
          required
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        />
      </div>
      <p className="text-xs text-gray-500 dark:text-gray-400">
        Geslo: vsaj 8 znakov, 1 velika črka, 1 številka, 1 poseben znak.
      </p>
      {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      {state?.success && <p className="text-sm text-green-600 dark:text-green-400">{state.success}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Dodajam …" : "Dodaj uporabnika"}
      </button>
    </form>
  );
}
