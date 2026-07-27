-- CreateTable
CREATE TABLE "client_sync_config" (
    "id" TEXT NOT NULL DEFAULT 'default',
    "apiUrl" TEXT,
    "apiToken" TEXT,
    "lastSyncAt" TIMESTAMP(3),
    "lastSyncStatus" TEXT,

    CONSTRAINT "client_sync_config_pkey" PRIMARY KEY ("id")
);
