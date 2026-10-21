import { notFound } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { EditVehicleForm } from "./edit-vehicle-form";

export default async function EditVehiclePage({ params }: { params: Promise<{ id: string }> }) {
  await requirePermission("canManageVehicles");
  const { id } = await params;

  const vehicle = await prisma.vehicle.findUnique({ where: { id } });
  if (!vehicle) notFound();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Uredi registrsko št.</h1>
      <EditVehicleForm id={vehicle.id} plate={vehicle.plate} />
    </div>
  );
}
