import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { renderToBuffer } from "@react-pdf/renderer";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { readPhotoBuffers, readSignatureBuffer } from "@/lib/uploads";
import { WorkOrderPdfDocument } from "@/lib/pdf";
import { sendWorkOrderEmail } from "@/lib/mailer";

const sendEmailSchema = z.object({ to: z.string().email() });

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canSendEmail) {
    return NextResponse.json({ error: "Nimate pravice za pošiljanje po e-pošti." }, { status: 403 });
  }

  const { id } = await params;
  const body = await request.json().catch(() => null);
  const parsed = sendEmailSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Vnesite veljaven e-mail naslov." }, { status: 400 });
  }

  const order = await prisma.workOrder.findUnique({
    where: { id },
    include: { client: true, installers: true, options: true, photos: true },
  });
  if (!order) return NextResponse.json({ error: "Nalog ne obstaja." }, { status: 404 });

  const [photoBuffers, signatureBuffer] = await Promise.all([
    readPhotoBuffers(order.id, order.photos),
    readSignatureBuffer(order.id, order.signatureUrl),
  ]);

  const pdfBuffer = await renderToBuffer(
    <WorkOrderPdfDocument order={order} photoBuffers={photoBuffers} signatureBuffer={signatureBuffer} />
  );

  try {
    await sendWorkOrderEmail(parsed.data.to, order.ident, pdfBuffer);
  } catch (err) {
    console.error("Posiljanje e-poste neuspesno:", err);
    return NextResponse.json({ error: "Pošiljanje e-pošte ni uspelo. Preverite SMTP nastavitve." }, { status: 502 });
  }

  await prisma.workOrder.update({ where: { id }, data: { status: "SENT" } });

  return NextResponse.json({ ok: true });
}
