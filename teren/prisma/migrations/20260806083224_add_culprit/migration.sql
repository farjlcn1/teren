-- CreateEnum
CREATE TYPE "Culprit" AS ENUM ('SLEDENJE', 'STRANKA');

-- AlterTable
ALTER TABLE "work_orders" ADD COLUMN     "culprit" "Culprit";
