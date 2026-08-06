"use client";

import { useEffect, useState } from "react";

export function ThemeToggle() {
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    setIsDark(document.documentElement.classList.contains("dark"));
  }, []);

  function toggle() {
    const next = !isDark;
    setIsDark(next);
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("teren-theme", next ? "dark" : "light");
  }

  return (
    <button
      type="button"
      onClick={toggle}
      title={isDark ? "Preklopi na svetli način" : "Preklopi na temni način"}
      className="rounded-md border border-blue-400 px-2.5 py-1.5 text-sm text-white hover:bg-blue-700"
    >
      {isDark ? "☀️" : "🌙"}
    </button>
  );
}
