import Link from "next/link";
import { redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import LogoutButton from "@/components/LogoutButton";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const user = await getSessionUser();
  if (!user) redirect("/login");
  if (!user.canViewAllOrders && !user.canManageUsers && !user.canManageClients) {
    redirect("/nalogi");
  }

  return (
    <div className="min-h-screen">
      <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="font-semibold">Teren — admin</span>
          <nav className="flex gap-4 text-sm">
            {user.canViewAllOrders && (
              <Link href="/admin" className="text-gray-700 hover:text-black">
                Delovni nalogi
              </Link>
            )}
            {user.canManageClients && (
              <Link href="/admin/stranke" className="text-gray-700 hover:text-black">
                Stranke
              </Link>
            )}
            {user.canManageUsers && (
              <Link href="/admin/uporabniki" className="text-gray-700 hover:text-black">
                Uporabniki
              </Link>
            )}
          </nav>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-gray-500">{user.fullName}</span>
          <LogoutButton />
        </div>
      </header>
      <main className="p-6">{children}</main>
    </div>
  );
}
