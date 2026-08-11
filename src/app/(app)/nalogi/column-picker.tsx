"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export type ColumnDef = { key: string; label: string };

export function ColumnPicker({
  columns,
  defaultColumns,
}: {
  columns: ColumnDef[];
  defaultColumns: string[];
}) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [open, setOpen] = useState(false);

  const activeCols = searchParams.get("cols")?.split(",").filter(Boolean) ?? defaultColumns;

  function toggle(key: string) {
    const next = activeCols.includes(key) ? activeCols.filter((k) => k !== key) : [...activeCols, key];
    const params = new URLSearchParams(searchParams);
    params.set("cols", next.join(","));
    router.push(`?${params.toString()}`);
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
      >
        Izberi stolpce
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-20 mt-1 w-56 rounded-md border border-gray-300 bg-white p-3 shadow-lg dark:border-gray-600 dark:bg-gray-800">
            {columns.map((col) => (
              <label
                key={col.key}
                className="flex items-center gap-2 py-1 text-sm text-gray-700 dark:text-gray-300"
              >
                <input
                  type="checkbox"
                  checked={activeCols.includes(col.key)}
                  onChange={() => toggle(col.key)}
                />
                {col.label}
              </label>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
