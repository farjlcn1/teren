import { redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import AdminOrdersTable from "@/components/AdminOrdersTable";

export default async function AdminOrdersPage() {
  const user = await getSessionUser();
  if (!user) redirect("/login");
  if (!user.canViewAllOrders) redirect("/nalogi");

  return (
    <div>
      <h1 className="text-lg font-semibold mb-4">Delovni nalogi</h1>
      <AdminOrdersTable canExport={user.canExportData} />
    </div>
  );
}
