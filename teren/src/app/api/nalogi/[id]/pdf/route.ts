import { NextResponse } from "next/server";
import { findWorkOrder } from "@/lib/api/work-orders";
import { requireUser } from "@/lib/auth/session";
import { generateWorkOrderPdf } from "@/lib/work-orders/pdf";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const user = await requireUser();
  const { id } = await params;

  const order = await findWorkOrder(id);
  if (!order) return new NextResponse("Not found", { status: 404 });
  if (!user.canViewAllOrders && order.createdById !== user.id) {
    return new NextResponse("Forbidden", { status: 403 });
  }

  const buffer = await generateWorkOrderPdf(order);

  return new NextResponse(new Uint8Array(buffer), {
    headers: {
      "Content-Type": "application/pdf",
      "Content-Disposition": `attachment; filename="nalog-${order.ident}.pdf"`,
    },
  });
}
