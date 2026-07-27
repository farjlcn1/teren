import "server-only";
import { prisma } from "@/lib/db";

// Atomarno generira ident oblike DDMMYYYYNNN (npr. 10072026001).
// INSERT ... ON CONFLICT ... DO UPDATE je en sam atomarni SQL stavek,
// zato je varno tudi če dva monterja shranita nalog istočasno.
export async function generateIdent(orderDate: Date): Promise<string> {
  const dateOnly = new Date(
    Date.UTC(orderDate.getFullYear(), orderDate.getMonth(), orderDate.getDate())
  );

  const rows = await prisma.$queryRaw<{ lastNumber: number }[]>`
    INSERT INTO "daily_sequence" ("orderDate", "lastNumber")
    VALUES (${dateOnly}, 1)
    ON CONFLICT ("orderDate")
    DO UPDATE SET "lastNumber" = "daily_sequence"."lastNumber" + 1
    RETURNING "lastNumber"
  `;

  const seq = rows[0].lastNumber;
  const dd = String(dateOnly.getUTCDate()).padStart(2, "0");
  const mm = String(dateOnly.getUTCMonth() + 1).padStart(2, "0");
  const yyyy = dateOnly.getUTCFullYear();

  return `${dd}${mm}${yyyy}${String(seq).padStart(3, "0")}`;
}
