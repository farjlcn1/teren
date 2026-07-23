import { notFound, redirect } from "next/navigation";
import Link from "next/link";
import { getSessionUser } from "@/lib/session";
import { prisma } from "@/lib/prisma";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
  WORK_ORDER_STATUS_LABELS,
} from "@/lib/constants";
import SendEmailButton from "@/components/SendEmailButton";

export default async function NalogDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) redirect("/login");

  const { id } = await params;
  const order = await prisma.workOrder.findUnique({
    where: { id },
    include: {
      client: true,
      installers: true,
      options: true,
      photos: true,
      createdBy: { select: { fullName: true } },
    },
  });

  if (!order) notFound();
  if (!user.canViewAllOrders && order.createdById !== user.id) {
    return <p className="p-4 text-red-600">Nimate dostopa do tega naloga.</p>;
  }

  return (
    <div className="max-w-lg mx-auto p-4 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold font-mono">{order.ident}</h1>
        <span className="text-xs text-gray-500">{WORK_ORDER_STATUS_LABELS[order.status]}</span>
      </div>

      <div className="grid grid-cols-2 gap-y-2 text-sm bg-white border border-gray-200 rounded-lg p-3">
        <span className="text-gray-500">Datum</span>
        <span>{order.orderDate.toLocaleDateString("sl-SI")}</span>

        <span className="text-gray-500">Stranka</span>
        <span>{order.client.name}</span>

        <span className="text-gray-500">Tip</span>
        <span>{WORK_ORDER_TYPE_LABELS[order.type]}</span>

        <span className="text-gray-500">Zahtevnost</span>
        <span>{DIFFICULTY_LABELS[order.difficulty]}</span>

        <span className="text-gray-500">Monterji</span>
        <span>
          {order.installers.map((i) => (i.name === "OSTALO" ? i.otherText : INSTALLER_NAME_LABELS[i.name])).join(", ")}
        </span>

        <span className="text-gray-500">Registrska št.</span>
        <span>{order.vehiclePlate}</span>

        <span className="text-gray-500">Vozilo</span>
        <span>
          {order.vehicleBrand} {order.vehicleModel} ({order.vehicleYear})
        </span>

        <span className="text-gray-500">IMEI</span>
        <span className="font-mono">{order.imei}</span>

        {order.imeiPrev && (
          <>
            <span className="text-gray-500">IMEI prej</span>
            <span className="font-mono">{order.imeiPrev}</span>
          </>
        )}

        <span className="text-gray-500">Izdelal</span>
        <span>{order.createdBy.fullName}</span>
      </div>

      {order.options.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-lg p-3">
          <h2 className="text-sm font-medium mb-2">Izbire</h2>
          <ul className="text-sm space-y-1">
            {order.options.map((o) => (
              <li key={o.id}>
                <span className="font-medium">{OPTION_TYPE_LABELS[o.optionType]}</span>
                {o.comment && <span className="text-gray-500"> — {o.comment}</span>}
              </li>
            ))}
          </ul>
        </div>
      )}

      {order.comment && (
        <div className="bg-white border border-gray-200 rounded-lg p-3">
          <h2 className="text-sm font-medium mb-1">Komentar</h2>
          <p className="text-sm text-gray-700 whitespace-pre-wrap">{order.comment}</p>
        </div>
      )}

      {order.photos.length > 0 && (
        <div>
          <h2 className="text-sm font-medium mb-2">Slike</h2>
          <div className="grid grid-cols-3 gap-2">
            {order.photos.map((p) => (
              // eslint-disable-next-line @next/next/no-img-element
              <img key={p.id} src={p.fileUrl} alt="Slika naloga" className="w-full h-24 object-cover rounded-lg border border-gray-200" />
            ))}
          </div>
        </div>
      )}

      {order.signatureUrl && (
        <div>
          <h2 className="text-sm font-medium mb-2">Podpis stranke</h2>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={order.signatureUrl} alt="Podpis stranke" className="border border-gray-200 rounded-lg bg-white" />
        </div>
      )}

      <div className="flex flex-wrap gap-2 pt-2">
        {user.canExportData && (
          <a href={`/api/work-orders/${order.id}/pdf`} target="_blank" rel="noreferrer" className="text-sm border border-gray-300 rounded-lg px-3 py-1.5">
            Prenesi PDF
          </a>
        )}
        {user.canSendEmail && <SendEmailButton workOrderId={order.id} />}
        {user.canEditOrders && (
          <Link href={`/nalogi/${order.id}/uredi`} className="text-sm border border-gray-300 rounded-lg px-3 py-1.5">
            Uredi
          </Link>
        )}
      </div>
    </div>
  );
}
