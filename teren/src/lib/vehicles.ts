import "server-only";
import { prisma } from "@/lib/db";

export function normalizePlate(plate: string): string {
  return plate.trim().toUpperCase();
}

// Če registrska št. še ne obstaja v bazi, jo doda (vir MANUAL), sicer je ne spreminja.
export async function ensureVehicleExists(plate: string): Promise<void> {
  const normalized = normalizePlate(plate);
  if (!normalized) return;

  await prisma.vehicle
    .upsert({
      where: { plate: normalized },
      create: { plate: normalized, source: "MANUAL" },
      update: {},
    })
    .catch(() => undefined);
}
