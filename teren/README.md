# teren – aplikacija

Spletna aplikacija za delovne naloge, plan, stranke, vozila in uporabnike (Next.js).
Do baze ne dostopa: vse podatke bere in piše prek **teren-api** (`../teren-api`), ki se mu predstavi z
`API_TOKEN`. Fotografije in podpisi so na disku aplikacije (`UPLOADS_DIR`), API hrani samo poti.

Zagon, selitev na strežnik in načrt API-ja so opisani v [README v korenu repozitorija](../README.md);
aplikacija se običajno zažene skupaj z API-jem in bazo z `docker compose up -d --build` v korenu.

## Razvoj

Za `npm run dev` mora teči API (npr. iz korena `docker compose up -d db api`, dosegljiv na `localhost:8080`).
V `.env.local` nastavi (predloga je `.env.example`):

| Spremenljivka | Pomen |
|---|---|
| `API_URL` | naslov teren-api, lokalno `http://localhost:8080` |
| `API_TOKEN` | enak kot `API_TOKEN` v `.env` v korenu |
| `SESSION_SECRET` | ključ za podpis prijavnih piškotkov (`openssl rand -base64 32`) |
| `UPLOADS_DIR` | mapa za fotografije in podpise, npr. `./uploads` |
| `SMTP_*` | pošiljanje nalogov po e-pošti; prazno = izklopljeno |

```bash
npm ci
npm run dev
```

Aplikacija je na http://localhost:3000.

## Kako kliče API

- `src/lib/api/http.ts`: `apiRequest` doda `Authorization: Bearer $API_TOKEN` in napake API-ja (ProblemDetail)
  pretvori v `ApiError` s slovenskim sporočilom za obrazec.
- En modul na področje: `clients.ts`, `vehicles.ts`, `users.ts`, `work-orders.ts`, `plan.ts`; samo na strežniku.
- Pravice prijavljenega uporabnika (`canManageClients` …) preveri aplikacija pred klicem API-ja.
