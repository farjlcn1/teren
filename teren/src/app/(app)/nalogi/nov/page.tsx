import Link from "next/link";
import { requireUser } from "@/lib/auth/session";
import { listClientOptions } from "@/lib/api/clients";
import { listVehicleOptions } from "@/lib/api/vehicles";
import { findPlannedTask } from "@/lib/api/plan";
import { WorkOrderForm } from "./work-order-form";

export default async function NovNalogPage({
  searchParams,
}: {
  searchParams: Promise<{ planTaskId?: string }>;
}) {
  await requireUser();
  const { planTaskId } = await searchParams;

  const [clients, vehicles, plannedTask] = await Promise.all([
    listClientOptions(),
    listVehicleOptions(),
    planTaskId ? findPlannedTask(planTaskId) : null,
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
        {plannedTask ? `Zaključi planirani nalog — ${plannedTask.clientName}` : "Nov delovni nalog"}
      </h1>
      <WorkOrderForm
        clients={clients}
        vehiclePlates={vehicles.map((v) => v.plate)}
        plannedTaskId={plannedTask?.id}
        initialClientId={plannedTask?.clientId}
        lockedClientName={plannedTask?.clientName}
        initialVehiclePlate={plannedTask?.vehiclePlate}
        initialComment={plannedTask?.note ?? undefined}
        initialType={plannedTask?.type ?? undefined}
      />
    </div>
  );
}
