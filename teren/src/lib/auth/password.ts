import bcrypt from "bcryptjs";
import { z } from "zod";

const BCRYPT_ROUNDS = 12;

export function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, BCRYPT_ROUNDS);
}

export function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

// Vsaj 8 znakov, vsaj 1 velika začetnica, vsaj 1 številka, vsaj 1 poseben znak.
export const passwordSchema = z
  .string()
  .min(8, "Geslo mora imeti vsaj 8 znakov")
  .regex(/[A-Z]/, "Geslo mora vsebovati vsaj eno veliko črko")
  .regex(/[0-9]/, "Geslo mora vsebovati vsaj eno številko")
  .regex(/[^A-Za-z0-9]/, "Geslo mora vsebovati vsaj en poseben znak");
