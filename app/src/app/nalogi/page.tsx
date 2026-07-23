import Link from "next/link";
import { getSessionUser } from "@/lib/session";
import { prisma } from "@/lib/prisma";
import { WORK_ORDER_TYPE_LABELS, DIFFICULTY_LABELS, WORK_ORDER_STATUS_LABELS } from "@/lib/constants";

export default async function MojiNalogiPage() {
  const user = await getSessionUser();
  if (!user) return null;

  const orders = await prisma.workOrder.findMany({
    where: user.canViewAllOrders ? {} : { createdById: user.id },
    orderBy: { orderDate: "desc" },
    take: 50,
    include: { client: true },
  });

  return (
    <div className="max-w-lg mx-auto p-4 space-y-3">
      <h1 className="text-lg font-semibold">{user.canViewAllOrders ? "Vsi nalogi" : "Moji nalogi"}</h1>

      {orders.length === 0 && <p className="text-gray-500 text-sm">Ni še nobenega naloga.</p>}

      {orders.map((o) => (
        <Link
          key={o.id}
          href={`/nalogi/${o.id}`}
          className="block border border-gray-200 rounded-lg p-3 bg-white hover:bg-gray-50"
        >
          <div className="flex justify-between items-start">
            <span className="font-mono text-sm font-semibold">{o.ident}</span>
            <span className="text-xs text-gray-500">{o.orderDate.toLocaleDateString("sl-SI")}</span>
          </div>
          <div className="text-sm mt-1">{o.client.name}</div>
          <div className="text-xs text-gray-500 mt-0.5">
            {WORK_ORDER_TYPE_LABELS[o.type]} · {DIFFICULTY_LABELS[o.difficulty]} · {o.vehiclePlate}
          </div>
          <div className="text-xs text-gray-400 mt-0.5">{WORK_ORDER_STATUS_LABELS[o.status]}</div>
        </Link>
      ))}
    </div>
  );
}
