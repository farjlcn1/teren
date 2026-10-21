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
