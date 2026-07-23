import type { Prisma, PrismaClient } from "@prisma/client";

type TxClient = PrismaClient | Prisma.TransactionClient;

// Ident je sestavljen iz datuma (DDMMYYYY) in zaporedne st. znotraj tega dneva (npr. 10072026001).
// Uporabi atomaren UPSERT na DailySequence, da je varno tudi ob soboccasnem shranjevanju vec nalogov.
export async function generateIdent(tx: TxClient, orderDate: Date): Promise<string> {
  const dateOnly = new Date(Date.UTC(orderDate.getFullYear(), orderDate.getMonth(), orderDate.getDate()));

  const rows = await tx.$queryRaw<{ lastNumber: number }[]>`
    INSERT INTO "DailySequence" ("orderDate", "lastNumber")
    VALUES (${dateOnly}, 1)
    ON CONFLICT ("orderDate")
    DO UPDATE SET "lastNumber" = "DailySequence"."lastNumber" + 1
    RETURNING "lastNumber"
  `;

  const sequence = rows[0].lastNumber;
  if (sequence > 999) {
    throw new Error("Preseženo je največje dovoljeno število nalogov v enem dnevu (999).");
  }

  const dd = String(orderDate.getDate()).padStart(2, "0");
  const mm = String(orderDate.getMonth() + 1).padStart(2, "0");
  const yyyy = orderDate.getFullYear();
  const seq = String(sequence).padStart(3, "0");

  return `${dd}${mm}${yyyy}${seq}`;
}
