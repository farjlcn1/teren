-- CreateEnum
CREATE TYPE "WorkOrderType" AS ENUM ('MONTAZA', 'DEMONTAZA', 'INTERVENCIJA', 'PREMONTAZA', 'OSTALO');

-- CreateEnum
CREATE TYPE "Difficulty" AS ENUM ('OSNOVNA', 'ZAHTEVNA');

-- CreateEnum
CREATE TYPE "InstallerName" AS ENUM ('SIMON', 'VITO', 'SERGEJ', 'GREGOR', 'KLEMEN', 'OSTALO');

-- CreateEnum
CREATE TYPE "OptionType" AS ENUM ('DIN1', 'DIN2', 'DIN3', 'DIN4', 'DIN5', 'ANI1', 'ANI2', 'ANI3', 'ALL_CAN', 'FMSCAN', 'TACHO');

-- CreateEnum
CREATE TYPE "WorkOrderStatus" AS ENUM ('COMPLETED', 'SENT');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "passwordHash" TEXT NOT NULL,
    "fullName" TEXT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "canManageUsers" BOOLEAN NOT NULL DEFAULT false,
    "canManageClients" BOOLEAN NOT NULL DEFAULT false,
    "canViewAllOrders" BOOLEAN NOT NULL DEFAULT false,
    "canExportData" BOOLEAN NOT NULL DEFAULT false,
    "canSendEmail" BOOLEAN NOT NULL DEFAULT false,
    "canEditOrders" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Client" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "address" TEXT,
    "contact" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Client_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WorkOrder" (
    "id" TEXT NOT NULL,
    "ident" TEXT NOT NULL,
    "orderDate" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "type" "WorkOrderType" NOT NULL,
    "difficulty" "Difficulty" NOT NULL,
    "clientId" TEXT NOT NULL,
    "vehiclePlate" TEXT NOT NULL,
    "vehicleBrand" TEXT NOT NULL,
    "vehicleModel" TEXT NOT NULL,
    "vehicleYear" TEXT NOT NULL,
    "imei" TEXT NOT NULL,
    "imeiPrev" TEXT,
    "comment" TEXT,
    "signatureUrl" TEXT,
    "status" "WorkOrderStatus" NOT NULL DEFAULT 'COMPLETED',
    "createdById" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "WorkOrder_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WorkOrderInstaller" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "name" "InstallerName" NOT NULL,
    "otherText" TEXT,

    CONSTRAINT "WorkOrderInstaller_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WorkOrderOption" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "optionType" "OptionType" NOT NULL,
    "comment" TEXT,

    CONSTRAINT "WorkOrderOption_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WorkOrderPhoto" (
    "id" TEXT NOT NULL,
    "workOrderId" TEXT NOT NULL,
    "fileUrl" TEXT NOT NULL,
    "uploadedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "WorkOrderPhoto_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "DailySequence" (
    "orderDate" TIMESTAMP(3) NOT NULL,
    "lastNumber" INTEGER NOT NULL,

    CONSTRAINT "DailySequence_pkey" PRIMARY KEY ("orderDate")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

-- CreateIndex
CREATE UNIQUE INDEX "WorkOrder_ident_key" ON "WorkOrder"("ident");

-- CreateIndex
CREATE INDEX "WorkOrder_orderDate_idx" ON "WorkOrder"("orderDate");

-- CreateIndex
CREATE INDEX "WorkOrder_clientId_idx" ON "WorkOrder"("clientId");

-- CreateIndex
CREATE INDEX "WorkOrder_vehiclePlate_idx" ON "WorkOrder"("vehiclePlate");

-- CreateIndex
CREATE INDEX "WorkOrder_imei_idx" ON "WorkOrder"("imei");

-- CreateIndex
CREATE INDEX "WorkOrderInstaller_workOrderId_idx" ON "WorkOrderInstaller"("workOrderId");

-- CreateIndex
CREATE INDEX "WorkOrderOption_workOrderId_idx" ON "WorkOrderOption"("workOrderId");

-- CreateIndex
CREATE INDEX "WorkOrderOption_optionType_idx" ON "WorkOrderOption"("optionType");

-- CreateIndex
CREATE INDEX "WorkOrderPhoto_workOrderId_idx" ON "WorkOrderPhoto"("workOrderId");

-- AddForeignKey
ALTER TABLE "WorkOrder" ADD CONSTRAINT "WorkOrder_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES "Client"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WorkOrder" ADD CONSTRAINT "WorkOrder_createdById_fkey" FOREIGN KEY ("createdById") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WorkOrderInstaller" ADD CONSTRAINT "WorkOrderInstaller_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "WorkOrder"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WorkOrderOption" ADD CONSTRAINT "WorkOrderOption_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "WorkOrder"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WorkOrderPhoto" ADD CONSTRAINT "WorkOrderPhoto_workOrderId_fkey" FOREIGN KEY ("workOrderId") REFERENCES "WorkOrder"("id") ON DELETE CASCADE ON UPDATE CASCADE;
