import "server-only";
import type { CurrentUser } from "@/lib/auth/session";

// Nalog sme videti (in s tem urejati, poslati ali prenesti njegove datoteke) njegov avtor ali uporabnik s
// pregledom vseh nalogov. API tega ne preverja, zato mora vsak dostop do posameznega naloga iti skozi to funkcijo.
export function canAccessWorkOrder(user: CurrentUser, order: { createdById: string }): boolean {
  return user.canViewAllOrders || order.createdById === user.id;
}
