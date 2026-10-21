"use client";

import { useEffect, useRef, useState } from "react";

export function PlateCombobox({
  plates,
  value,
  onChange,
  className,
}: {
  plates: string[];
  value: string;
  onChange: (plate: string) => void;
  className?: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const trimmed = value.trim().toLowerCase();
  const filtered = trimmed ? plates.filter((p) => p.toLowerCase().includes(trimmed)) : plates;
  const showAddNew = trimmed.length > 0 && !plates.some((p) => p.toLowerCase() === trimmed);

  return (
    <div ref={containerRef} className="relative">
      <input
        type="text"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setIsOpen(true);
        }}
        onFocus={() => setIsOpen(true)}
        placeholder="Išči ali vnesi novo registrsko …"
        className={className}
      />
      {isOpen && (filtered.length > 0 || showAddNew) && (
        <ul className="absolute z-10 mt-1 max-h-64 w-full overflow-y-auto rounded-md border border-gray-300 bg-white shadow-lg dark:border-gray-600 dark:bg-gray-800">
          {filtered.slice(0, 100).map((p) => (
            <li key={p}>
              <button
                type="button"
                onClick={() => {
                  onChange(p);
                  setIsOpen(false);
                }}
                className="block w-full px-4 py-3 text-left text-base text-gray-900 hover:bg-blue-50 dark:text-gray-100 dark:hover:bg-gray-700 sm:px-3 sm:py-2 sm:text-sm"
              >
                {p}
              </button>
            </li>
          ))}
          {showAddNew && (
            <li>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="block w-full px-4 py-3 text-left text-base text-blue-600 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-gray-700 sm:px-3 sm:py-2 sm:text-sm"
              >
                + Dodaj novo: &quot;{value.trim()}&quot;
              </button>
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
