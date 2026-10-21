-- Shema baze Teren za teste API-ja.
-- Sestavljena iz Prisma migracij repozitorija farjlcn1/teren (commit e01b04a, mapa prisma/migrations).
-- Dokler shemo upravlja Prisma, je treba datoteko ob vsaki novi migraciji posodobiti (glej README).

-- ==== 20260724073207_init ====
-- CreateEnum
CREATE TYPE "WorkOrderType" AS ENUM ('MONTAZA', 'DEMONTAZA', 'INTERVENCIJA', 'PREMONTAZA', 'OSTALO');

-- CreateEnum
CREATE TYPE "Difficulty" AS ENUM ('OSNOVNA', 'ZAHTEVNA');

-- CreateEnum
CREATE TYPE "InstallerName" AS ENUM ('SIMON', 'VITO', 'SERGEJ', 'GREGOR', 'KLEMEN', 'OSTALO');

-- CreateEnum
CREATE TYPE "OptionType" AS ENUM ('DIN1', 'DIN2', 'DIN3', 'DIN4', 'DIN5', 'ANI1', 'ANI2', 'ANI3', 'ALL_CAN', 'FMSCAN', 'TACHO');

-- CreateEnum
CREATE TYPE "WorkOrderStatus" AS ENUM ('ACTIVE', 'CANCELLED');

-- CreateEnum
CREATE TYPE "ClientSource" AS ENUM ('MANUAL', 'IMPORT', 'SYNC');

-- CreateTable
CREATE TABLE "users" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "passwordHash" TEXT NOT NULL,
    "fullName" TEXT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "canManageUsers" BOOLEAN NOT NULL DEFAULT false,
    "canViewAllOrders" BOOLEAN NOT NULL DEFAULT false,
    "canExportData" BOOLEAN NOT NULL DEFAULT false,
    "canSendEmail" BOOLEAN NOT NULL DEFAULT false,
    "canEditOrders" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "clients" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "address" TEXT,
    "contactInfo" TEXT,
    "source" "ClientSource" NOT NULL DEFAULT 'MANUAL',
    "externalId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "clients_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "work_orders" (
    "id" TEXT NOT NULL,
    "ident" TEXT NOT NULL,
    "orderDate" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "type" "WorkOrderType" NOT NULL,
    "difficulty" "Difficulty" NOT NULL,
    "status" "WorkOrderStatus" NOT NULL DEFAULT 'ACTIVE',
    "clientId" TEXT NOT NULL,
    "vehiclePlate" TEXT NOT NULL,
    "vehicleBrand" TEXT NOT NULL,
    "vehicleModel" TEXT NOT NULL,
    "vehicleYear" INTEGER NOT NULL,
    "imei" TEXT NOT NULL,
    "imeiPrev" TEXT,
    "comment" TEXT,
    "signatureUrl" TEXT,
    "signedAt" TIMESTAMP(3),
    "lockedAt" TIMESTAMP(3),
    "sentAt" TIMESTAMP(3),
    "createdById" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "work_orders_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "work_order_installers" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "name" "InstallerName" NOT NULL,
    "otherText" TEXT,

    CONSTRAINT "work_order_installers_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "work_order_options" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "optionType" "OptionType" NOT NULL,
    "comment" TEXT,

    CONSTRAINT "work_order_options_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "work_order_photos" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "filePath" TEXT NOT NULL,
    "uploadedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "work_order_photos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "work_order_edits" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "editedById" TEXT NOT NULL,
    "editedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "changesJson" TEXT NOT NULL,

    CONSTRAINT "work_order_edits_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "daily_sequence" (
    "orderDate" DATE NOT NULL,
    "lastNumber" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "daily_sequence_pkey" PRIMARY KEY ("orderDate")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_email_key" ON "users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "clients_externalId_key" ON "clients"("externalId");

-- CreateIndex
CREATE UNIQUE INDEX "work_orders_ident_key" ON "work_orders"("ident");

-- CreateIndex
CREATE INDEX "work_orders_orderDate_idx" ON "work_orders"("orderDate");

-- CreateIndex
CREATE INDEX "work_orders_clientId_idx" ON "work_orders"("clientId");

-- CreateIndex
CREATE INDEX "work_orders_vehiclePlate_idx" ON "work_orders"("vehiclePlate");

-- CreateIndex
CREATE INDEX "work_orders_imei_idx" ON "work_orders"("imei");

-- CreateIndex
CREATE UNIQUE INDEX "work_order_options_workOrderId_optionType_key" ON "work_order_options"("workOrderId", "optionType");

-- AddForeignKey
ALTER TABLE "work_orders" ADD CONSTRAINT "work_orders_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES "clients"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_orders" ADD CONSTRAINT "work_orders_createdById_fkey" FOREIGN KEY ("createdById") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_order_installers" ADD CONSTRAINT "work_order_installers_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_order_options" ADD CONSTRAINT "work_order_options_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_order_photos" ADD CONSTRAINT "work_order_photos_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_order_edits" ADD CONSTRAINT "work_order_edits_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "work_order_edits" ADD CONSTRAINT "work_order_edits_editedById_fkey" FOREIGN KEY ("editedById") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- ==== 20260724075730_add_client_sync_config ====
-- CreateTable
CREATE TABLE "client_sync_config" (
    "id" TEXT NOT NULL DEFAULT 'default',
    "apiUrl" TEXT,
    "apiToken" TEXT,
    "lastSyncAt" TIMESTAMP(3),
    "lastSyncStatus" TEXT,

    CONSTRAINT "client_sync_config_pkey" PRIMARY KEY ("id")
);

-- ==== 20260724075836_add_can_manage_clients ====
-- AlterTable
ALTER TABLE "users" ADD COLUMN     "canManageClients" BOOLEAN NOT NULL DEFAULT false;

-- ==== 20260727062824_add_vehicles ====
-- AlterTable
ALTER TABLE "users" ADD COLUMN     "canManageVehicles" BOOLEAN NOT NULL DEFAULT false;

-- CreateTable
CREATE TABLE "vehicles" (
    "id" TEXT NOT NULL,
    "plate" TEXT NOT NULL,
    "source" "ClientSource" NOT NULL DEFAULT 'MANUAL',
    "externalId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "vehicles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "vehicle_sync_config" (
    "id" TEXT NOT NULL DEFAULT 'default',
    "apiUrl" TEXT,
    "apiToken" TEXT,
    "lastSyncAt" TIMESTAMP(3),
    "lastSyncStatus" TEXT,

    CONSTRAINT "vehicle_sync_config_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "vehicles_plate_key" ON "vehicles"("plate");

-- CreateIndex
CREATE UNIQUE INDEX "vehicles_externalId_key" ON "vehicles"("externalId");

-- Backfill vehicles from plates already used on existing work orders
INSERT INTO "vehicles" ("id", "plate", "source", "createdAt", "updatedAt")
SELECT
  md5(random()::text || clock_timestamp()::text || norm.plate),
  norm.plate,
  'MANUAL',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  SELECT DISTINCT UPPER(TRIM("vehiclePlate")) AS plate
  FROM "work_orders"
  WHERE "vehiclePlate" IS NOT NULL AND TRIM("vehiclePlate") <> ''
) AS norm
ON CONFLICT ("plate") DO NOTHING;

-- ==== 20260805114214_add_option_types ====
-- AlterEnum
-- This migration adds more than one value to an enum.
-- With PostgreSQL versions 11 and earlier, this is not possible
-- in a single migration. This can be worked around by creating
-- multiple migrations, each migration adding only one value to
-- the enum.


ALTER TYPE "OptionType" ADD VALUE 'WIRE_TEMP1';
ALTER TYPE "OptionType" ADD VALUE 'WIRE_TEMP2';
ALTER TYPE "OptionType" ADD VALUE 'WIRE_TEMP3';
ALTER TYPE "OptionType" ADD VALUE 'ID_KEY';
ALTER TYPE "OptionType" ADD VALUE 'RFID_125';
ALTER TYPE "OptionType" ADD VALUE 'RFID_1356';
ALTER TYPE "OptionType" ADD VALUE 'BUZZER';

-- ==== 20260806083224_add_culprit ====
-- CreateEnum
CREATE TYPE "Culprit" AS ENUM ('SLEDENJE', 'STRANKA');

-- AlterTable
ALTER TABLE "work_orders" ADD COLUMN     "culprit" "Culprit";

-- ==== 20260811090000_add_device_models ====
-- CreateEnum
CREATE TYPE "DeviceModel" AS ENUM ('FMC130', 'FMC150', 'FMC650', 'FMC880', 'TFT100', 'OSTALO');

-- CreateTable
CREATE TABLE "work_order_device_models" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "deviceModel" "DeviceModel" NOT NULL,
    "comment" TEXT,

    CONSTRAINT "work_order_device_models_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "work_order_device_models_workOrderId_deviceModel_key" ON "work_order_device_models"("workOrderId", "deviceModel");

-- AddForeignKey
ALTER TABLE "work_order_device_models" ADD CONSTRAINT "work_order_device_models_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "work_orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- ==== 20260812060000_add_photo_taken_at ====
-- AlterTable
ALTER TABLE "work_order_photos" ADD COLUMN     "takenAt" TIMESTAMP(3);

-- ==== 20260918105753_add_plan_groups ====
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

-- ==== 20260928114347_add_plan_group_contact ====
-- AlterTable
ALTER TABLE "plan_groups" ADD COLUMN     "contact" TEXT;

-- ==== 20260928120000_move_installer_to_plan_group ====
-- Monter (expectedInstaller/expectedInstallerOtherText) je odslej vezan na cel dogodek
-- (PlanGroup), ne na vsak posamezni nalog -- določi ga organizator enkrat, ob planiranju.
ALTER TABLE "plan_groups" ADD COLUMN "expectedInstaller" "InstallerName";
ALTER TABLE "plan_groups" ADD COLUMN "expectedInstallerOtherText" TEXT;

-- Namesto tega naloge znotraj dogodka dobijo tip (Montaža/Demontaža/Intervencija/...), da ga
-- organizator lahko že vnaprej določi in ga monterju ni treba znova izbirati ob izpolnitvi.
ALTER TABLE "planned_tasks" ADD COLUMN "type" "WorkOrderType";
ALTER TABLE "planned_tasks" DROP COLUMN "expectedInstaller";
ALTER TABLE "planned_tasks" DROP COLUMN "expectedInstallerOtherText";
