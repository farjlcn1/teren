import "server-only";

// Odjemalec za teren-api. Uporablja se samo na strežniku (server components, server actions,
// route handlers), ker pošilja API_TOKEN; pravice uporabnika preveri aplikacija pred klicem.

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ApiRequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
};

function apiConfig() {
  const baseUrl = process.env.API_URL;
  const token = process.env.API_TOKEN;
  if (!baseUrl || !token) {
    throw new Error("API_URL in API_TOKEN morata biti nastavljena.");
  }
  return { baseUrl: baseUrl.replace(/\/+$/, ""), token };
}

// API napake vrača kot ProblemDetail (RFC 9457); "detail" je sporočilo za uporabnika.
async function problemDetail(res: Response): Promise<string> {
  try {
    const problem = (await res.json()) as { detail?: unknown };
    if (typeof problem.detail === "string" && problem.detail) return problem.detail;
  } catch {
    // odgovor ni JSON
  }
  return `API je vrnil napako ${res.status}.`;
}

export async function apiRequest<T>(path: string, { method = "GET", body }: ApiRequestOptions = {}): Promise<T> {
  const { baseUrl, token } = apiConfig();
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
    cache: "no-store",
  });

  // API je zavrnil API_TOKEN (glava WWW-Authenticate): napaka v nastavitvah, ne napačni podatki uporabnika.
  if (res.status === 401 && res.headers.has("WWW-Authenticate")) {
    throw new Error("API je zavrnil API_TOKEN. Preveri, da imata teren in api isti API_TOKEN v .env.");
  }
  if (!res.ok) {
    throw new ApiError(res.status, await problemDetail(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

// Sporočilo za obrazec: napake API-ja so že v slovenščini, ostalo (npr. API ni dosegljiv) ostane v logu.
export function apiErrorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  console.error("Klic API-ja ni uspel", e);
  return "Povezava z API-jem ni uspela. Poskusi znova.";
}
