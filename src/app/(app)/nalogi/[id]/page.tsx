import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { requireUser } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { SendEmailForm } from "./send-email-form";
import { ImageLightbox } from "@/components/image-lightbox";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};

const DIFFICULTY_LABELS: Record<string, string> = {
  OSNOVNA: "Osnovna",
  ZAHTEVNA: "Zahtevna",
};

const OPTION_LABELS: Record<string, string> = { ALL_CAN: "ALL CAN" };

export default async function WorkOrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const user = await requireUser();
  const { id } = await params;

  const order = await prisma.workOrder.findUnique({
    where: { id },
    include: {
      client: true,
      installers: true,
      options: true,
      photos: true,
      createdBy: true,
      edits: { include: { editedBy: true }, orderBy: { editedAt: "desc" } },
    },
  });

  if (!order) notFound();

  if (!user.canViewAllOrders && order.createdById !== user.id) {
    redirect("/nalogi");
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Nalog {order.ident}</h1>
        <div className="flex items-center gap-2">
          {order.lockedAt && (
            <span className="rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-600 dark:bg-gray-700 dark:text-gray-300">
              Zaklenjen
            </span>
          )}
          <a
            href={`/api/nalogi/${order.id}/pdf`}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
          >
            PDF
          </a>
          {user.canEditOrders && (
            <Link
              href={`/nalogi/${order.id}/edit`}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
            >
              Uredi
            </Link>
          )}
        </div>
      </div>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg border border-gray-200 bg-white p-4 text-sm dark:border-gray-700 dark:bg-gray-800">
        <dt className="text-gray-500 dark:text-gray-400">Datum</dt>
        <dd className="text-gray-900 dark:text-gray-100">{order.orderDate.toLocaleDateString("sl-SI")}</dd>

        <dt className="text-gray-500 dark:text-gray-400">Tip</dt>
        <dd className="text-gray-900 dark:text-gray-100">{TYPE_LABELS[order.type]}</dd>

        <dt className="text-gray-500 dark:text-gray-400">Zahtevnost</dt>
        <dd className="text-gray-900 dark:text-gray-100">{DIFFICULTY_LABELS[order.difficulty]}</dd>

        <dt className="text-gray-500 dark:text-gray-400">Stranka</dt>
        <dd className="text-gray-900 dark:text-gray-100">{order.client.name}</dd>

        <dt className="text-gray-500 dark:text-gray-400">Monterji</dt>
        <dd className="text-gray-900 dark:text-gray-100">
          {order.installers
            .map((i) => (i.name === "OSTALO" ? i.otherText : i.name.charAt(0) + i.name.slice(1).toLowerCase()))
            .join(", ")}
        </dd>

        <dt className="text-gray-500 dark:text-gray-400">Registrska št.</dt>
        <dd className="text-gray-900 dark:text-gray-100">{order.vehiclePlate}</dd>

        <dt className="text-gray-500 dark:text-gray-400">Vozilo</dt>
        <dd className="text-gray-900 dark:text-gray-100">
          {order.vehicleBrand} {order.vehicleModel} ({order.vehicleYear})
        </dd>

        <dt className="text-gray-500 dark:text-gray-400">IMEI</dt>
        <dd className="text-gray-900 dark:text-gray-100">{order.imei}</dd>

        {order.imeiPrev && (
          <>
            <dt className="text-gray-500 dark:text-gray-400">IMEI prej</dt>
            <dd className="text-gray-900 dark:text-gray-100">{order.imeiPrev}</dd>
          </>
        )}

        <dt className="text-gray-500 dark:text-gray-400">Izdelal</dt>
        <dd className="text-gray-900 dark:text-gray-100">{order.createdBy.fullName}</dd>
      </dl>

      {user.canSendEmail && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">Pošlji po mailu</h2>
          {order.sentAt && (
            <p className="mb-2 text-xs text-gray-500 dark:text-gray-400">
              Nazadnje poslano: {order.sentAt.toLocaleString("sl-SI")}
            </p>
          )}
          <SendEmailForm workOrderId={order.id} clientContact={order.client.contactInfo?.includes("@") ? order.client.contactInfo : ""} />
        </div>
      )}

      {order.options.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">DIN / ANI / CAN</h2>
          <ul className="space-y-1 text-sm">
            {order.options.map((o) => (
              <li key={o.id} className="text-gray-700 dark:text-gray-300">
                <span className="font-medium">{OPTION_LABELS[o.optionType] ?? o.optionType}</span>
                {o.comment ? ` — ${o.comment}` : ""}
              </li>
            ))}
          </ul>
        </div>
      )}

      {order.comment && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-1 text-sm font-semibold text-gray-900 dark:text-gray-100">Komentar</h2>
          <p className="text-sm text-gray-700 whitespace-pre-wrap dark:text-gray-300">{order.comment}</p>
        </div>
      )}

      {order.photos.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">Slike</h2>
          <div className="grid grid-cols-3 gap-2">
            {order.photos.map((p) => (
              <ImageLightbox key={p.id} src={`/api/uploads/${p.filePath}`} alt="">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={`/api/uploads/${p.filePath}`}
                  alt=""
                  className="h-24 w-full rounded-md object-cover transition hover:opacity-80"
                />
              </ImageLightbox>
            ))}
          </div>
        </div>
      )}

      {order.signatureUrl && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">Podpis stranke</h2>
          <ImageLightbox src={`/api/uploads/${order.signatureUrl}`} alt="Podpis">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={`/api/uploads/${order.signatureUrl}`}
              alt="Podpis"
              className="h-32 rounded-md border border-gray-200 bg-white transition hover:opacity-80"
            />
          </ImageLightbox>
        </div>
      )}

      {order.edits.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
          <h2 className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">Zgodovina sprememb</h2>
          <ul className="space-y-3 text-sm">
            {order.edits.map((edit) => {
              const changes = JSON.parse(edit.changesJson) as Record<string, { from: unknown; to: unknown }>;
              return (
                <li
                  key={edit.id}
                  className="border-t border-gray-100 pt-2 first:border-t-0 first:pt-0 dark:border-gray-700"
                >
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {edit.editedAt.toLocaleString("sl-SI")} — {edit.editedBy.fullName}
                  </p>
                  <ul className="mt-1 space-y-0.5 text-gray-700 dark:text-gray-300">
                    {Object.entries(changes).map(([field, { from, to }]) => (
                      <li key={field}>
                        <span className="font-medium">{field}:</span> {String(from ?? "—")} → {String(to ?? "—")}
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
