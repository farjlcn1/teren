import { NextRequest, NextResponse } from "next/server";
import { renderToBuffer } from "@react-pdf/renderer";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";
import { readPhotoBuffers, readSignatureBuffer } from "@/lib/uploads";
import { WorkOrderPdfDocument } from "@/lib/pdf";

export async function GET(_request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });

  const { id } = await params;
  const order = await prisma.workOrder.findUnique({
    where: { id },
    include: { client: true, installers: true, options: true, photos: true },
  });

  if (!order) return NextResponse.json({ error: "Nalog ne obstaja." }, { status: 404 });

  const canAccess = user.canViewAllOrders || order.createdById === user.id;
  if (!canAccess) return NextResponse.json({ error: "Nimate dostopa." }, { status: 403 });

  const [photoBuffers, signatureBuffer] = await Promise.all([
    readPhotoBuffers(order.id, order.photos),
    readSignatureBuffer(order.id, order.signatureUrl),
  ]);

  const buffer = await renderToBuffer(
    <WorkOrderPdfDocument order={order} photoBuffers={photoBuffers} signatureBuffer={signatureBuffer} />
  );

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `inline; filename="${order.ident}.pdf"`,
    },
  });
}
