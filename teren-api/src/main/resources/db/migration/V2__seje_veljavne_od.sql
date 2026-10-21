-- Preklic sej: seje (podpisani piškotki aplikacije), izdane pred tem časom, ne veljajo več.
-- Nastavi se ob spremembi gesla, da ukradena ali stara seja preneha veljati.
ALTER TABLE "users" ADD COLUMN "sessionsValidFrom" TIMESTAMP(3);
