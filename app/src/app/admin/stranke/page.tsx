import { redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import ClientsAdmin from "@/components/ClientsAdmin";

export default async function StrankePage() {
  const user = await getSessionUser();
  if (!user) redirect("/login");
  if (!user.canManageClients) redirect("/nalogi");

  return <ClientsAdmin />;
}
