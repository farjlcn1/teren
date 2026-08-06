"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export function NavLinks({ links }: { links: { href: string; label: string }[] }) {
  const pathname = usePathname();

  const activeHref = [...links]
    .sort((a, b) => b.href.length - a.href.length)
    .find((l) => pathname === l.href || pathname.startsWith(l.href + "/"))?.href;

  return (
    <nav className="mt-2 flex flex-wrap gap-x-1 gap-y-1">
      {links.map((l) => (
        <Link
          key={l.href}
          href={l.href}
          className={`rounded-md px-2 py-1.5 text-base hover:bg-blue-700 hover:text-white sm:px-2 sm:py-1 sm:text-sm ${
            l.href === activeHref ? "font-bold text-white" : "text-blue-100"
          }`}
        >
          {l.label}
        </Link>
      ))}
    </nav>
  );
}
