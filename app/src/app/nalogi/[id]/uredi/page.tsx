import { notFound, redirect } from "next/navigation";
import { getSessionUser } from "@/lib/session";
import { prisma } from "@/lib/prisma";
import WorkOrderEditForm from "@/components/WorkOrderEditForm";

export default async function UrediNalogPage({ params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) redirect("/login");
  if (!user.canEditOrders) redirect("/nalogi");

  const { id } = await params;
  const [order, clients] = await Promise.all([
    prisma.workOrder.findUnique({ where: { id }, include: { installers: true, options: true } }),
    prisma.client.findMany({ orderBy: { name: "asc" } }),
  ]);

  if (!order) notFound();

  return <WorkOrderEditForm order={order} clients={clients} />;
}
