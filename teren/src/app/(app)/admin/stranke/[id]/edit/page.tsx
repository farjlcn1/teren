import { notFound } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { EditClientForm } from "./edit-client-form";

export default async function EditClientPage({ params }: { params: Promise<{ id: string }> }) {
  await requirePermission("canManageClients");
  const { id } = await params;

  const client = await prisma.client.findUnique({ where: { id } });
  if (!client) notFound();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Uredi stranko</h1>
      <EditClientForm
        id={client.id}
        name={client.name}
        address={client.address ?? ""}
        contactInfo={client.contactInfo ?? ""}
      />
    </div>
  );
}
