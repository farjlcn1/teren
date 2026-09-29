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
    // Zavihek Plan potrebuje širšo tabelo kot ostale strani -- ta ovojnica prebije čez skupno
    // max-w-[1600px] omejitev iz layout.tsx nazaj na resnično širino okna, znotraj nje pa se
    // vsebina znova centrira na svojo (10% širšo) mejo.
    <div className="relative left-1/2 right-1/2 w-screen -mx-[50vw]">
      <div className="mx-auto max-w-[1760px] px-4">
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
            contact: g.contact,
            expectedInstaller: g.expectedInstaller,
            expectedInstallerOtherText: g.expectedInstallerOtherText,
            tasks: g.tasks.map((t) => ({
              id: t.id,
              vehiclePlate: t.vehiclePlate,
              type: t.type,
              note: t.note,
              workOrderId: t.workOrderId,
            })),
          }))}
          canManagePlan={user.canManagePlan}
        />
      </div>
    </div>
  );
}
