import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { requireUser } from "@/lib/auth/session";
import { generateWorkOrderPdf } from "@/lib/work-orders/pdf";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const user = await requireUser();
  const { id } = await params;

  const order = await prisma.workOrder.findUnique({ where: { id }, select: { createdById: true, ident: true } });
  if (!order) return new NextResponse("Not found", { status: 404 });
  if (!user.canViewAllOrders && order.createdById !== user.id) {
    return new NextResponse("Forbidden", { status: 403 });
  }

  const buffer = await generateWorkOrderPdf(id);

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="nalog-${order.ident}.pdf"`,
    },
  });
}
