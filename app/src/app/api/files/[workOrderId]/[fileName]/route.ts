import { NextRequest, NextResponse } from "next/server";
import { readFile } from "fs/promises";
import path from "path";
import { getSessionUser } from "@/lib/session";
import { prisma } from "@/lib/prisma";
import { uploadDirFor } from "@/lib/uploads";

const SAFE_FILENAME_RE = /^(podpis\.png|foto-\d+\.jpg)$/;

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ workOrderId: string; fileName: string }> }
) {
  const user = await getSessionUser();
  if (!user) {
    return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  }

  const { workOrderId, fileName } = await params;

  if (!SAFE_FILENAME_RE.test(fileName)) {
    return NextResponse.json({ error: "Neveljavno ime datoteke." }, { status: 400 });
  }

  const workOrder = await prisma.workOrder.findUnique({
    where: { id: workOrderId },
    select: { createdById: true },
  });

  if (!workOrder) {
    return NextResponse.json({ error: "Nalog ne obstaja." }, { status: 404 });
  }

  const canAccess = user.canViewAllOrders || workOrder.createdById === user.id;
  if (!canAccess) {
    return NextResponse.json({ error: "Nimate dostopa." }, { status: 403 });
  }

  const filePath = path.join(uploadDirFor(workOrderId), fileName);

  try {
    const data = await readFile(filePath);
    const contentType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
    return new NextResponse(new Uint8Array(data), {
      headers: { "Content-Type": contentType, "Cache-Control": "private, max-age=31536000" },
    });
  } catch {
    return NextResponse.json({ error: "Datoteka ne obstaja." }, { status: 404 });
  }
}
