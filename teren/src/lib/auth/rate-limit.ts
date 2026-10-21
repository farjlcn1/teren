// Preprost, v pomnilniku hranjen števec neuspelih prijav -- brez tega bi lahko nekdo neomejeno
// avtomatizirano preizkušal gesla. Namerno preverjeno PRED klicem API-ja, da se ista zaklenitev
// uporabi ne glede na to, ali email sploh pripada obstoječemu uporabniku (ne razkrije obstoja
// računa). V pomnilniku (ne v bazi) je dovolj -- aplikacija teče kot en sam proces, zaklenitev
// pa se ob morebitnem restartu preprosto ponastavi, kar je sprejemljivo za ta namen.
//
// Dva števca: po emailu (ugibanje gesla enega računa) in po IP-ju (eno geslo na veliko računih,
// "password spraying"). IP poda Cloudflare v glavi cf-connecting-ip; brez nje (lokalno) se
// omejuje samo po emailu.
const EMAIL_MAX_ATTEMPTS = 5;
const IP_MAX_ATTEMPTS = 20;
const LOCKOUT_MS = 15 * 60 * 1000;

type Counter = { count: number; lockedUntil: number | null };

const emailAttempts = new Map<string, Counter>();
const ipAttempts = new Map<string, Counter>();

function lockedMinutesLeft(entry: Counter | undefined): number | null {
  if (!entry?.lockedUntil || entry.lockedUntil <= Date.now()) return null;
  return Math.ceil((entry.lockedUntil - Date.now()) / 60000);
}

function recordFailure(attempts: Map<string, Counter>, key: string, maxAttempts: number): void {
  const entry = attempts.get(key) ?? { count: 0, lockedUntil: null };
  entry.count += 1;
  if (entry.count >= maxAttempts) {
    entry.lockedUntil = Date.now() + LOCKOUT_MS;
    entry.count = 0;
  }
  attempts.set(key, entry);
}

export function checkLoginRateLimit(email: string, ip: string | null): { blocked: boolean; message?: string } {
  const minutesLeft = lockedMinutesLeft(emailAttempts.get(email)) ?? (ip ? lockedMinutesLeft(ipAttempts.get(ip)) : null);
  if (minutesLeft !== null) {
    return { blocked: true, message: `Preveč neuspelih poskusov prijave. Poskusi znova čez ${minutesLeft} min.` };
  }
  return { blocked: false };
}

export function recordLoginFailure(email: string, ip: string | null): void {
  recordFailure(emailAttempts, email, EMAIL_MAX_ATTEMPTS);
  if (ip) recordFailure(ipAttempts, ip, IP_MAX_ATTEMPTS);
}

// Uspešna prijava ponastavi samo števec emaila; IP lahko še naprej poskuša tuje račune.
export function recordLoginSuccess(email: string): void {
  emailAttempts.delete(email);
}
