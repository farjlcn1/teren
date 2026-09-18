import Link from "next/link";
import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { WorkOrderForm } from "./work-order-form";

export default async function NovNalogPage({
  searchParams,
}: {
  searchParams: Promise<{ planTaskId?: string }>;
}) {
  await requireUser();
  const { planTaskId } = await searchParams;

  const [clients, vehicles, plannedTask] = await Promise.all([
    prisma.client.findMany({ orderBy: { name: "asc" }, select: { id: true, name: true } }),
    prisma.vehicle.findMany({ orderBy: { plate: "asc" }, select: { plate: true } }),
    planTaskId
      ? prisma.plannedTask.findUnique({
          where: { id: planTaskId },
          include: { planGroup: { include: { client: { select: { id: true, name: true } } } } },
        })
      : null,
  ]);

  // Nekdo drug je ta planirani nalog med tem že izpolnil -- pokaži povezavo na obstoječi nalog
  // namesto praznega obrazca, ki bi ob oddaji ustvaril podvojen nalog.
  if (plannedTask?.workOrderId) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Nalog je že opravljen</h1>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Ta planirani nalog je že bil izpolnjen s strani drugega monterja.
        </p>
        <Link
          href={`/nalogi/${plannedTask.workOrderId}`}
          className="text-sm font-medium text-blue-600 dark:text-blue-400"
        >
          Ogled opravljenega naloga →
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
        {plannedTask ? `Zaključi planirani nalog — ${plannedTask.planGroup.client.name}` : "Nov delovni nalog"}
      </h1>
      <WorkOrderForm
        clients={clients}
        vehiclePlates={vehicles.map((v) => v.plate)}
        plannedTaskId={plannedTask?.id}
        initialClientId={plannedTask?.planGroup.clientId}
        lockedClientName={plannedTask?.planGroup.client.name}
        initialVehiclePlate={plannedTask?.vehiclePlate}
        initialComment={plannedTask?.note ?? undefined}
      />
    </div>
  );
}
