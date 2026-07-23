# Teren — aplikacija za delovne naloge

Spletna aplikacija (PWA) za zaključevanje delovnih nalogov monterjev sledilne opreme na terenu.

## Namestitev na Raspberry Pi

Predpogoj: Docker in Docker Compose sta že nameščena, Cloudflare Tunnel je nastavljen (glej dogovorjeno arhitekturo).

1. Kloniraj repozitorij v `~/nalogi-app` (ali obstoječo mapo, ki si jo že pripravil):

   ```bash
   git clone https://github.com/farjlcn1/teren.git ~/nalogi-app
   cd ~/nalogi-app
   ```

2. Ustvari `.env` datoteko iz predloge in izpolni prave vrednosti:

   ```bash
   cp .env.example .env
   nano .env
   ```

   - `POSTGRES_PASSWORD` — poljubno močno geslo za bazo
   - `SESSION_SECRET` — generiraj z `openssl rand -hex 32`
   - `SMTP_*` — podatki za pošiljanje e-pošte (SMTP strežnik podjetja)
   - `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` / `SEED_ADMIN_NAME` — prvi admin uporabnik, ustvarjen ob prvem zagonu (mora ustrezati zahtevam za geslo: 8+ znakov, 1 velika črka, 1 številka, 1 poseben znak)

3. Zgradi in zaženi:

   ```bash
   docker compose up -d --build
   ```

   Ob prvem zagonu se samodejno izvedejo migracije baze in ustvari začetni admin uporabnik (glej izpis z `docker compose logs -f app`).

4. Preveri, da je `cloudflared` config (`~/.cloudflared/config.yml`) usmerjen na `http://localhost:3000` — aplikacija posluša na vratih 3000.

5. Odpri `https://tvoja-domena.com` v brskalniku in se prijavi z `SEED_ADMIN_EMAIL`/`SEED_ADMIN_PASSWORD`.

## Posodobitev aplikacije (po spremembah kode)

```bash
cd ~/nalogi-app
git pull
docker compose up -d --build
```

Migracije se ob vsakem zagonu samodejno preverijo in po potrebi izvedejo (`prisma migrate deploy`).

## Prvi koraki po namestitvi

1. Prijavi se kot admin.
2. Pojdi na **Stranke** in dodaj vsaj eno stranko (dropdown na mobilnem obrazcu je prazen dokler ni strank).
3. Pojdi na **Uporabniki** in ustvari uporabnike za monterje (brez posebnih pravic) ter po potrebi za pisarno (npr. `canViewAllOrders` + `canExportData` + `canSendEmail`).

## Struktura projekta

- `app/` — Next.js aplikacija (mobilni obrazec + admin pregled)
- `app/prisma/schema.prisma` — shema baze
- `docker-compose.yml` — PostgreSQL + aplikacija
- `.env.example` — predloga za potrebne skrivnosti/nastavitve

## Backup

Priporočeno redno arhiviranje `./postgres-data` (baza) in `./uploads` (slike, podpisi) na lokacijo izven strežnika.
