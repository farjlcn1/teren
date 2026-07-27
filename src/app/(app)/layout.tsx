import Link from "next/link";
import { requireUser } from "@/lib/auth/session";
import { LogoutButton } from "@/components/logout-button";
import { ThemeToggle } from "@/components/theme-toggle";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser();

  const links = [
    { href: "/nalogi/nov", label: "Nov nalog", show: true },
    { href: "/nalogi", label: "Nalogi", show: true },
    { href: "/admin/stranke", label: "Stranke", show: user.canManageClients },
    { href: "/admin/registracije", label: "Registracije", show: user.canManageVehicles },
    { href: "/admin/uporabniki", label: "Uporabniki", show: user.canManageUsers },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="border-b border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
        <div className="mx-auto max-w-6xl px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <span className="font-semibold text-gray-900 dark:text-gray-100">Teren</span>
            <div className="flex items-center gap-3">
              <span className="hidden text-sm text-gray-500 dark:text-gray-400 sm:inline">{user.fullName}</span>
              <ThemeToggle />
              <LogoutButton />
            </div>
          </div>
          <nav className="mt-2 flex flex-wrap gap-x-1 gap-y-1">
            {links
              .filter((l) => l.show)
              .map((l) => (
                <Link
                  key={l.href}
                  href={l.href}
                  className="rounded-md px-2 py-1.5 text-base text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white sm:px-2 sm:py-1 sm:text-sm"
                >
                  {l.label}
                </Link>
              ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  );
}
