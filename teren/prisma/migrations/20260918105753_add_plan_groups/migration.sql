-- AlterTable
ALTER TABLE "users" ADD COLUMN     "canManagePlan" BOOLEAN NOT NULL DEFAULT false;

-- CreateTable
CREATE TABLE "plan_groups" (
    "id" TEXT NOT NULL,
    "clientId" TEXT NOT NULL,
    "startAt" TIMESTAMP(3) NOT NULL,
    "endAt" TIMESTAMP(3) NOT NULL,
    "note" TEXT,
    "createdById" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "plan_groups_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "planned_tasks" (
    "id" TEXT NOT NULL,
    "planGroupId" TEXT NOT NULL,
    "vehiclePlate" TEXT NOT NULL,
    "note" TEXT,
    "expectedInstaller" "InstallerName",
    "expectedInstallerOtherText" TEXT,
    "workOrderId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "planned_tasks_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "plan_groups_startAt_idx" ON "plan_groups"("startAt");

-- CreateIndex
CREATE INDEX "plan_groups_clientId_idx" ON "plan_groups"("clientId");

-- CreateIndex
CREATE UNIQUE INDEX "planned_tasks_workOrderId_key" ON "planned_tasks"("workOrderId");

-- CreateIndex
CREATE INDEX "planned_tasks_planGroupId_idx" ON "planned_tasks"("planGroupId");

-- AddForeignKey
ALTER TABLE "plan_groups" ADD CONSTRAINT "plan_groups_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES "clients"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "plan_groups" ADD CONSTRAINT "plan_groups_createdById_fkey" FOREIGN KEY ("createdById") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "planned_tasks" ADD CONSTRAINT "planned_tasks_planGroupId_fkey" FOREIGN KEY ("planGroupId") REFERENCES "plan_groups"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "planned_tasks" ADD CONSTRAINT "planned_tasks_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE SET NULL ON UPDATE CASCADE;
