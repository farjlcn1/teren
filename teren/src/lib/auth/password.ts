import { z } from "zod";

// Vsaj 8 znakov, vsaj 1 velika začetnica, vsaj 1 številka, vsaj 1 poseben znak.
// Enaka pravila preveri tudi API (PasswordPolicy), ki geslo tudi zgošči.
export const passwordSchema = z
  .string()
  .min(8, "Geslo mora imeti vsaj 8 znakov")
  .regex(/[A-Z]/, "Geslo mora vsebovati vsaj eno veliko črko")
  .regex(/[0-9]/, "Geslo mora vsebovati vsaj eno številko")
  .regex(/[^A-Za-z0-9]/, "Geslo mora vsebovati vsaj en poseben znak");
