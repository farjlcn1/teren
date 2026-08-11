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
