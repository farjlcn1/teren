import { notFound } from "next/navigation";
import { requirePermission } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { PermissionsForm } from "./permissions-form";
import { ResetPasswordForm } from "./reset-password-form";

export default async function EditUserPage({ params }: { params: Promise<{ id: string }> }) {
  await requirePermission("canManageUsers");
  const { id } = await params;

  const user = await prisma.user.findUnique({ where: { id } });
  if (!user) notFound();

  return (
    <div className="max-w-md space-y-4">
      <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
        Uredi uporabnika — {user.fullName} ({user.email})
      </h1>
      <PermissionsForm
        userId={user.id}
        isActive={user.isActive}
        permissions={{
          canManageUsers: user.canManageUsers,
          canManageClients: user.canManageClients,
          canManageVehicles: user.canManageVehicles,
          canViewAllOrders: user.canViewAllOrders,
          canEditOrders: user.canEditOrders,
          canExportData: user.canExportData,
          canSendEmail: user.canSendEmail,
        }}
      />
      <ResetPasswordForm userId={user.id} />
    </div>
  );
}
