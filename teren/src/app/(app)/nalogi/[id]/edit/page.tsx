import { notFound } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { EditWorkOrderForm } from "./edit-work-order-form";

export default async function EditWorkOrderPage({ params }: { params: Promise<{ id: string }> }) {
  await requirePermission("canEditOrders");
  const { id } = await params;

  const [order, clients, vehicles] = await Promise.all([
    prisma.workOrder.findUnique({ where: { id }, include: { installers: true, options: true } }),
    prisma.client.findMany({ orderBy: { name: "asc" }, select: { id: true, name: true } }),
    prisma.vehicle.findMany({ orderBy: { plate: "asc" }, select: { plate: true } }),
  ]);

  if (!order) notFound();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Uredi nalog {order.ident}</h1>
      <EditWorkOrderForm
        id={order.id}
        clients={clients}
        vehiclePlates={vehicles.map((v) => v.plate)}
        initial={{
          type: order.type,
          difficulty: order.difficulty,
          clientId: order.clientId,
          vehiclePlate: order.vehiclePlate,
          vehicleBrand: order.vehicleBrand,
          vehicleModel: order.vehicleModel,
          vehicleYear: String(order.vehicleYear),
          imei: order.imei,
          imeiPrev: order.imeiPrev ?? "",
          culprit: order.culprit ?? "",
          comment: order.comment ?? "",
          installers: order.installers,
          options: order.options,
        }}
      />
    </div>
  );
}
