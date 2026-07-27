import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { WorkOrderForm } from "./work-order-form";

export default async function NovNalogPage() {
  await requireUser();

  const [clients, vehicles] = await Promise.all([
    prisma.client.findMany({ orderBy: { name: "asc" }, select: { id: true, name: true } }),
    prisma.vehicle.findMany({ orderBy: { plate: "asc" }, select: { plate: true } }),
  ]);

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Nov delovni nalog</h1>
      <WorkOrderForm clients={clients} vehiclePlates={vehicles.map((v) => v.plate)} />
    </div>
  );
}
