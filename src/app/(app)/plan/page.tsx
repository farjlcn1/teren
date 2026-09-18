import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { PlanCalendar } from "./plan-calendar";

function startOfWeek(d: Date): Date {
  const date = new Date(d);
  const day = date.getDay(); // 0=Ned..6=Sob
  const diff = (day === 0 ? -6 : 1) - day; // premakni nazaj na ponedeljek
  date.setDate(date.getDate() + diff);
  date.setHours(0, 0, 0, 0);
  return date;
}

export default async function PlanPage({
  searchParams,
}: {
  searchParams: Promise<{ teden?: string }>;
}) {
  const user = await requireUser();
  const { teden } = await searchParams;

  const parsedWeekDate = teden ? new Date(teden) : new Date();
  const weekStart = startOfWeek(Number.isNaN(parsedWeekDate.getTime()) ? new Date() : parsedWeekDate);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekEnd.getDate() + 7);

  const [clients, vehicles, groups] = await Promise.all([
    prisma.client.findMany({ orderBy: { name: "asc" }, select: { id: true, name: true } }),
    prisma.vehicle.findMany({ orderBy: { plate: "asc" }, select: { plate: true } }),
    prisma.planGroup.findMany({
      where: { startAt: { lt: weekEnd }, endAt: { gt: weekStart } },
      orderBy: { startAt: "asc" },
      include: {
        client: { select: { id: true, name: true } },
        tasks: { orderBy: { createdAt: "asc" } },
      },
    }),
  ]);

  return (
    <div className="space-y-6">
      <h1 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Plan</h1>

      <PlanCalendar
        weekStartIso={weekStart.toISOString()}
        clients={clients}
        vehiclePlates={vehicles.map((v) => v.plate)}
        groups={groups.map((g) => ({
          id: g.id,
          clientId: g.clientId,
          clientName: g.client.name,
          startAt: g.startAt.toISOString(),
          endAt: g.endAt.toISOString(),
          note: g.note,
          tasks: g.tasks.map((t) => ({
            id: t.id,
            vehiclePlate: t.vehiclePlate,
            note: t.note,
            expectedInstaller: t.expectedInstaller,
            expectedInstallerOtherText: t.expectedInstallerOtherText,
            workOrderId: t.workOrderId,
          })),
        }))}
        canManagePlan={user.canManagePlan}
      />
    </div>
  );
}
