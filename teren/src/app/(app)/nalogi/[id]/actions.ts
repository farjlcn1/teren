"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { requirePermission } from "@/lib/auth/session";
import { findWorkOrder } from "@/lib/api/work-orders";
import { sendWorkOrderEmail } from "@/lib/mail";
import { canAccessWorkOrder } from "@/lib/work-orders/access";

const emailSchema = z.string().email("Vnesi veljaven email naslov.");

export type SendEmailState = { error?: string; success?: string } | undefined;

export async function sendOrderEmail(
  workOrderId: string,
  _prevState: SendEmailState,
  formData: FormData
): Promise<SendEmailState> {
  const user = await requirePermission("canSendEmail");

  const parsed = emailSchema.safeParse(formData.get("email"));
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljaven email." };
  }

  // ID naloga pride od odjemalca, zato se pravica do naloga preveri tu (enako kot pri podrobnostih).
  const order = await findWorkOrder(workOrderId);
  if (!order || !canAccessWorkOrder(user, order)) {
    return { error: "Nalog ne obstaja." };
  }

  try {
    await sendWorkOrderEmail(order, parsed.data);
  } catch (e) {
    // Podrobnosti (SMTP, nastavitve) samo v log, uporabniku splošno sporočilo.
    console.error("Pošiljanje naloga po e-pošti ni uspelo", e);
    return { error: "Pošiljanje ni uspelo. Poskusi znova ali preveri nastavitve e-pošte." };
  }

  revalidatePath(`/nalogi/${workOrderId}`);
  return { success: `Nalog poslan na ${parsed.data}.` };
}
