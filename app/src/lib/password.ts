import bcrypt from "bcryptjs";

// Minimalna zahteva: vsaj 8 znakov, vsaj 1 velika crka, vsaj 1 stevilka, vsaj 1 poseben znak.
const MIN_LENGTH = 8;
const UPPERCASE_RE = /[A-ZČĆŠŽĐ]/;
const DIGIT_RE = /[0-9]/;
const SPECIAL_RE = /[^A-Za-z0-9ČĆŠŽĐčćšžđ]/;

export function validatePassword(password: string): string[] {
  const errors: string[] = [];
  if (password.length < MIN_LENGTH) {
    errors.push(`Geslo mora imeti vsaj ${MIN_LENGTH} znakov.`);
  }
  if (!UPPERCASE_RE.test(password)) {
    errors.push("Geslo mora vsebovati vsaj eno veliko začetnico.");
  }
  if (!DIGIT_RE.test(password)) {
    errors.push("Geslo mora vsebovati vsaj eno številko.");
  }
  if (!SPECIAL_RE.test(password)) {
    errors.push("Geslo mora vsebovati vsaj en poseben znak.");
  }
  return errors;
}

export function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, 12);
}

export function comparePassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}
