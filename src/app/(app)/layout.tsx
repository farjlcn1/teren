import { requireUser } from "@/lib/auth/session";
import { LogoutButton } from "@/components/logout-button";
import { ThemeToggle } from "@/components/theme-toggle";
import { NavLinks } from "@/components/nav-links";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser();

  const links = [
    { href: "/nalogi/nov", label: "Nov nalog", show: true },
    { href: "/nalogi", label: "Nalogi", show: true },
    { href: "/admin/stranke", label: "Stranke", show: user.canManageClients },
    { href: "/admin/registracije", label: "Vozila", show: user.canManageVehicles },
    { href: "/admin/uporabniki", label: "Uporabniki", show: user.canManageUsers },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="border-b border-blue-700 bg-blue-600">
        <div className="mx-auto max-w-6xl px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <span className="font-semibold text-white">Teren</span>
            <div className="flex items-center gap-3">
              <span className="hidden text-sm text-blue-100 sm:inline">{user.fullName}</span>
              <ThemeToggle />
              <LogoutButton />
            </div>
          </div>
          <NavLinks links={links.filter((l) => l.show)} />
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  );
}
