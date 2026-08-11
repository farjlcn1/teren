"use client";

import { useEffect, useRef, useState } from "react";

function yesterday() {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().slice(0, 10);
}

export function AdminExportButton() {
  const [open, setOpen] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function close(e: MouseEvent) {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("click", close);
    return () => document.removeEventListener("click", close);
  }, [open]);

  return (
    <div className="relative" ref={boxRef}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        title="Nalogi za izbrano časovno obdobje, v posebni obliki za administracijo"
        className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
      >
        Izvoz admin
      </button>
      {open && (
        <form
          action="/api/nalogi/export-admin"
          method="get"
          className="absolute right-0 z-20 mt-1 w-64 space-y-3 rounded-md border border-gray-300 bg-white p-3 shadow-lg dark:border-gray-600 dark:bg-gray-800"
        >
          <label className="block text-xs font-medium text-gray-600 dark:text-gray-400">
            Datum od
            <input
              type="date"
              name="from"
              defaultValue={yesterday()}
              required
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1.5 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            />
          </label>
          <label className="block text-xs font-medium text-gray-600 dark:text-gray-400">
            Datum do
            <input
              type="date"
              name="to"
              defaultValue={yesterday()}
              required
              className="mt-1 w-full rounded-md border border-gray-300 bg-white px-2 py-1.5 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            />
          </label>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:text-gray-300"
            >
              Prekliči
            </button>
            <button type="submit" className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white">
              Izvozi
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
