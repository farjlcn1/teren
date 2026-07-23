import { redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import UsersAdmin from "@/components/UsersAdmin";

export default async function UporabnikiPage() {
  const user = await getSessionUser();
  if (!user) redirect("/login");
  if (!user.canManageUsers) redirect("/nalogi");

  return <UsersAdmin />;
}
