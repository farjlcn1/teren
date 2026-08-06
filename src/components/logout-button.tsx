import { logout } from "@/lib/auth/logout-action";

export function LogoutButton() {
  return (
    <form action={logout}>
      <button
        type="submit"
        className="rounded-md border border-blue-400 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
      >
        Odjava
      </button>
    </form>
  );
}
