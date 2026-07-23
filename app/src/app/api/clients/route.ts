import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { getSessionUser } from "@/lib/session";

export async function GET() {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });

  const clients = await prisma.client.findMany({ orderBy: { name: "asc" } });
  return NextResponse.json(clients);
}

const clientSchema = z.object({
  name: z.string().min(1, "Ime stranke je obvezno."),
  address: z.string().optional(),
  contact: z.string().optional(),
});

export async function POST(request: NextRequest) {
  const user = await getSessionUser();
  if (!user) return NextResponse.json({ error: "Ni prijavljen." }, { status: 401 });
  if (!user.canManageClients) {
    return NextResponse.json({ error: "Nimate pravice za upravljanje strank." }, { status: 403 });
  }

  const body = await request.json().catch(() => null);
  const parsed = clientSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message || "Neveljavni podatki." }, { status: 400 });
  }

  const client = await prisma.client.create({ data: parsed.data });
  return NextResponse.json(client, { status: 201 });
}
