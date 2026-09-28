-- Monter (expectedInstaller/expectedInstallerOtherText) je odslej vezan na cel dogodek
-- (PlanGroup), ne na vsak posamezni nalog -- določi ga organizator enkrat, ob planiranju.
ALTER TABLE "plan_groups" ADD COLUMN "expectedInstaller" "InstallerName";
ALTER TABLE "plan_groups" ADD COLUMN "expectedInstallerOtherText" TEXT;

-- Namesto tega naloge znotraj dogodka dobijo tip (Montaža/Demontaža/Intervencija/...), da ga
-- organizator lahko že vnaprej določi in ga monterju ni treba znova izbirati ob izpolnitvi.
ALTER TABLE "planned_tasks" ADD COLUMN "type" "WorkOrderType";
ALTER TABLE "planned_tasks" DROP COLUMN "expectedInstaller";
ALTER TABLE "planned_tasks" DROP COLUMN "expectedInstallerOtherText";
