import { NextRequest, NextResponse } from "next/server";
import { getSession } from "@/lib/auth/session";
import { findWorkOrder } from "@/lib/api/work-orders";
import { readUploadedFile } from "@/lib/uploads";
import { canAccessWorkOrder } from "@/lib/work-orders/access";

function contentTypeFor(path: string) {
  if (path.endsWith(".png")) return "image/png";
  if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
  return "application/octet-stream";
}

// Datoteke so samo fotografije in podpisi nalogov (work-orders/<id>/<datoteka>). Vidi jih, kdor sme videti
// nalog: avtor ali uporabnik s pregledom vseh nalogov (enako kot podrobnosti in PDF naloga).
export async function GET(_request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const user = await getSession();
  if (!user) return new NextResponse("Unauthorized", { status: 401 });

  const { path: pathParts } = await params;
  if (pathParts.length !== 3 || pathParts[0] !== "work-orders") {
    return new NextResponse("Not found", { status: 404 });
  }
  const [, workOrderId, fileName] = pathParts;

  if (!user.canViewAllOrders) {
    const order = await findWorkOrder(workOrderId);
    // Enak odgovor kot za neobstoječo datoteko, da se obstoj tujih nalogov ne razkrije.
    if (!order || !canAccessWorkOrder(user, order)) {
      return new NextResponse("Not found", { status: 404 });
    }
  }

  const relativePath = pathParts.join("/");
  try {
    const buffer = await readUploadedFile(relativePath);
    return new NextResponse(new Uint8Array(buffer), {
      headers: {
        "Content-Type": contentTypeFor(fileName),
        "Content-Disposition": "inline",
        "X-Content-Type-Options": "nosniff",
        "Content-Security-Policy": "default-src 'none'",
        "Cache-Control": "private, max-age=31536000, immutable",
      },
    });
  } catch {
    return new NextResponse("Not found", { status: 404 });
  }
}
