"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { requirePermission } from "@/lib/auth/session";
import { sendWorkOrderEmail } from "@/lib/mail";

const emailSchema = z.string().email("Vnesi veljaven email naslov.");

export type SendEmailState = { error?: string; success?: string } | undefined;

export async function sendOrderEmail(
  workOrderId: string,
  _prevState: SendEmailState,
  formData: FormData
): Promise<SendEmailState> {
  await requirePermission("canSendEmail");

  const parsed = emailSchema.safeParse(formData.get("email"));
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? "Neveljaven email." };
  }

  try {
    await sendWorkOrderEmail(workOrderId, parsed.data);
  } catch (e) {
    return { error: e instanceof Error ? e.message : "Pošiljanje ni uspelo." };
  }

  revalidatePath(`/nalogi/${workOrderId}`);
  return { success: `Nalog poslan na ${parsed.data}.` };
}
