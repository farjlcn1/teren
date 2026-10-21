"use client";

import { useTransition } from "react";
import { deleteVehicle } from "./actions";

export function DeleteVehicleButton({ id }: { id: string }) {
  const [pending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={pending}
      onClick={() => {
        if (!confirm("Izbriši to registrsko št.?")) return;
        startTransition(async () => {
          try {
            await deleteVehicle(id);
          } catch (e) {
            alert(e instanceof Error ? e.message : "Napaka pri brisanju.");
          }
        });
      }}
      className="text-sm text-red-600 hover:underline disabled:opacity-50 dark:text-red-400"
    >
      {pending ? "…" : "Izbriši"}
    </button>
  );
}
