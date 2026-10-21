"use client";

import { useActionState } from "react";
import { resetUserPassword } from "../../actions";

export function ResetPasswordForm({ userId }: { userId: string }) {
  const action = resetUserPassword.bind(null, userId);
  const [state, formAction, pending] = useActionState(action, undefined);

  return (
    <form
      action={formAction}
      className="space-y-3 rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800"
    >
      <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Ponastavi geslo</h2>
      <input
        name="password"
        type="password"
        placeholder="Novo geslo"
        required
        className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
      />
      <p className="text-xs text-gray-500 dark:text-gray-400">
        Vsaj 8 znakov, 1 velika črka, 1 številka, 1 poseben znak.
      </p>
      {state?.error && <p className="text-sm text-red-600 dark:text-red-400">{state.error}</p>}
      {state?.success && <p className="text-sm text-green-600 dark:text-green-400">{state.success}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300"
      >
        {pending ? "Shranjujem …" : "Ponastavi geslo"}
      </button>
    </form>
  );
}
