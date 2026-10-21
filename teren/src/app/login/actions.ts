"use server";

import { z } from "zod";
import { redirect } from "next/navigation";
import { authenticate, type User } from "@/lib/api/users";
import { apiErrorMessage } from "@/lib/api/http";
import { createSession } from "@/lib/auth/session";
import { checkLoginRateLimit, recordLoginFailure, recordLoginSuccess } from "@/lib/auth/rate-limit";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export type LoginState = { error?: string } | undefined;

export async function login(_prevState: LoginState, formData: FormData): Promise<LoginState> {
  const parsed = loginSchema.safeParse({
    email: formData.get("email"),
    password: formData.get("password"),
  });
  if (!parsed.success) {
    return { error: "Vnesi veljaven email in geslo." };
  }

  const rememberMe = formData.get("rememberMe") === "on";
  const redirectTo = formData.get("redirectTo");
  const email = parsed.data.email.toLowerCase();

  const rateLimit = checkLoginRateLimit(email);
  if (rateLimit.blocked) {
    return { error: rateLimit.message };
  }

  // Geslo preveri API; null pomeni napačen email ali geslo ali onemogočenega uporabnika.
  let user: User | null;
  try {
    user = await authenticate(email, parsed.data.password);
  } catch (e) {
    return { error: apiErrorMessage(e) };
  }
  if (!user) {
    recordLoginFailure(email);
    return { error: "Napačen email ali geslo." };
  }

  recordLoginSuccess(email);
  await createSession(user.id, rememberMe);
  // Samo poti znotraj aplikacije ("/plan"), ne zunanji naslovi ("https://...", "//...", "/\\..." brskalnik bere kot "//").
  const target = typeof redirectTo === "string" && /^\/(?![/\\])/.test(redirectTo) ? redirectTo : "/";
  redirect(target);
}
