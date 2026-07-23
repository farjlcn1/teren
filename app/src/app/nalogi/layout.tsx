import Link from "next/link";
import { redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import LogoutButton from "@/components/LogoutButton";

export default async function NalogiLayout({ children }: { children: React.ReactNode }) {
  const user = await getSessionUser();
  if (!user) redirect("/login");

  return (
    <div>
      <header className="sticky top-0 bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between z-10">
        <Link href="/nalogi" className="font-semibold">
          Teren
        </Link>
        <div className="flex items-center gap-3">
          <Link href="/nalogi/novi" className="text-sm bg-gray-900 text-white rounded-lg px-3 py-1.5">
            + Nov nalog
          </Link>
          <LogoutButton />
        </div>
      </header>
      {children}
    </div>
  );
}
