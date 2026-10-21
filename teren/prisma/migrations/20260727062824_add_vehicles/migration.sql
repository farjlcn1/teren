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
