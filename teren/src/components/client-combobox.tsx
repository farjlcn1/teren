"use client";

import { useEffect, useRef, useState } from "react";

type Client = { id: string; name: string };

export function ClientCombobox({
  clients,
  value,
  onChange,
  required,
}: {
  clients: Client[];
  value: string;
  onChange: (id: string) => void;
  required?: boolean;
}) {
  const selected = clients.find((c) => c.id === value);
  const [query, setQuery] = useState(selected?.name ?? "");
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const current = clients.find((c) => c.id === value);
    setQuery(current?.name ?? "");
  }, [value, clients]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        const current = clients.find((c) => c.id === value);
        setQuery(current?.name ?? "");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [value, clients]);

  const trimmed = query.trim().toLowerCase();
  const filtered = trimmed ? clients.filter((c) => c.name.toLowerCase().includes(trimmed)) : clients;

  return (
    <div ref={containerRef} className="relative">
      <input
        type="text"
        value={query}
        required={required}
        onChange={(e) => {
          setQuery(e.target.value);
          setIsOpen(true);
          if (value) onChange("");
        }}
        onFocus={() => setIsOpen(true)}
        placeholder="Išči ali izberi stranko …"
        className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2.5 text-base text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 sm:py-2"
      />
      {isOpen && (
        <ul className="absolute z-10 mt-1 max-h-64 w-full overflow-y-auto rounded-md border border-gray-300 bg-white shadow-lg dark:border-gray-600 dark:bg-gray-800">
          {filtered.length === 0 && (
            <li className="px-4 py-3 text-base text-gray-400 dark:text-gray-500 sm:text-sm">Ni zadetkov.</li>
          )}
          {filtered.slice(0, 200).map((c) => (
            <li key={c.id}>
              <button
                type="button"
                onClick={() => {
                  onChange(c.id);
                  setQuery(c.name);
                  setIsOpen(false);
                }}
                className="block w-full px-4 py-3 text-left text-base text-gray-900 hover:bg-blue-50 dark:text-gray-100 dark:hover:bg-gray-700 sm:px-3 sm:py-2 sm:text-sm"
              >
                {c.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
