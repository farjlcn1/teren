import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

async function main() {
  const existingAdminCount = await prisma.user.count({ where: { canManageUsers: true } });
  if (existingAdminCount > 0) {
    console.log("Admin uporabnik ze obstaja, seed preskocen.");
    return;
  }

  const email = process.env.SEED_ADMIN_EMAIL;
  const password = process.env.SEED_ADMIN_PASSWORD;
  const fullName = process.env.SEED_ADMIN_NAME || "Admin";

  if (!email || !password) {
    console.log("SEED_ADMIN_EMAIL / SEED_ADMIN_PASSWORD nista nastavljena, seed preskocen.");
    return;
  }

  const passwordHash = await bcrypt.hash(password, 12);

  await prisma.user.create({
    data: {
      email: email.toLowerCase(),
      passwordHash,
      fullName,
      isActive: true,
      canManageUsers: true,
      canManageClients: true,
      canViewAllOrders: true,
      canExportData: true,
      canSendEmail: true,
      canEditOrders: true,
    },
  });

  console.log(`Zacetni admin uporabnik ustvarjen: ${email}`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
