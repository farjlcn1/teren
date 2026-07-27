import Link from "next/link";
import { requireUser } from "@/lib/auth/session";

export default async function HomePage() {
  const user = await requireUser();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Pozdravljen, {user.fullName}</h1>
      <div className="flex flex-wrap gap-3">
        <Link
          href="/nalogi/nov"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Nov delovni nalog
        </Link>
        <Link
          href="/nalogi"
          className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
        >
          Pregled nalogov
        </Link>
      </div>
    </div>
  );
}
