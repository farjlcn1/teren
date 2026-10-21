# Teren – testno okolje (aplikacija + API)

Dva Docker kontejnerja, aplikacija **teren** (Next.js) in **teren-api** (Spring Boot), ter lokalna testna baza.
Do baze dostopa samo API; aplikacija vse podatke bere in piše prek njega.
Kasneje se okolje preseli na drug strežnik s pravo bazo (glej [Selitev](#selitev-na-drug-strežnik-s-pravo-bazo)).

```
                    teren.farjolcn.com (Cloudflare tunel)
                                  │
┌─────────────────────────────────▼──────────────── Docker (omrežje "teren") ──┐
│  teren   Next.js :3000  ── API_URL=http://api:8080, API_TOKEN ──►  api  Spring Boot :8080
│  (fotografije, podpisi: volumen uploads)                            │ JPA, Flyway
│                                                  db  Postgres 17 :5432  ◄┘
└──────────────────────────────────────────────────────────────────────────────┘
```

| Mapa / datoteka | Vsebina |
|---|---|
| `docker-compose.yml` | servisi `db`, `api`, `teren` + `tunnel` (profil) |
| `teren/` | aplikacija teren (Next.js) |
| `teren-api/` | Spring Boot API po internih smernicah kodiranja |
| `teren-server/` | compose za strežnik z zunanjo bazo (glej [Selitev](#selitev-na-drug-strežnik-s-pravo-bazo)) |
| `.env` | skrivnosti (geslo baze, `SESSION_SECRET`, `API_TOKEN` …), **ne sodi v git** |
| `.env.example` | predloga za `.env` |

## Zagon

```bash
docker compose up -d --build
```

Vrstni red zagona določa compose: `db` → `api` (Flyway ustvari ali posodobi shemo) → `teren`.

Prvi admin: če je v `.env` nastavljen `SEED_ADMIN_EMAIL` in v bazi še ni nobenega uporabnika, ga API ob zagonu
ustvari z vsemi pravicami in naključnim geslom, ki ga izpiše v log (shrani si ga in ga po prijavi zamenjaj):

```bash
docker compose logs api | grep "prvi admin"
```

| Naslov | Kaj |
|---|---|
| http://localhost:3000 | aplikacija teren |
| http://localhost:8080/swagger-ui.html | Swagger dokumentacija API-ja (*Authorize* → `API_TOKEN` iz `.env`) |
| http://localhost:8080/actuator/health | stanje API-ja |
| `localhost:5432`, baza `teren`, uporabnik `teren` | testna baza (geslo je `POSTGRES_PASSWORD` v `.env`) |

Vsi porti so vezani na `127.0.0.1`, zato od zunaj niso dosegljivi.

Pošiljanje e-pošte je izklopljeno, dokler v `.env` ni nastavljen `SMTP_HOST`.

## Domena teren.farjolcn.com (Cloudflare tunel)

1. V Cloudflare: **Zero Trust → Networks → Tunnels → Create a tunnel → Cloudflared**, ime npr. `teren-test`.
2. Iz prikazanega ukaza za namestitev kopiraj žeton (dolg niz za `--token`) v `.env` kot `CLOUDFLARE_TUNNEL_TOKEN`.
3. Pri **Public hostname** nastavi: subdomena `teren`, domena `farjolcn.com`, tip `HTTP`, URL `teren:3000`.
   Če Cloudflare javi, da DNS zapis že obstaja, v DNS nastavitvah `farjolcn.com` najprej izbriši
   obstoječi CNAME `teren`, ki kaže na stari tunel.
4. Zaženi tunel:

   ```bash
   docker compose --profile tunnel up -d
   ```

API prek tunela **ni** izpostavljen. Dosegljiv je samo aplikaciji teren in lokalno na tem računalniku.

## Testi API-ja

Testi tečejo na lastni začasni bazi v Dockerju (Testcontainers); shemo ustvari Flyway, tako kot v pravi bazi.

Na tem računalniku je nameščena samo Java JRE (brez prevajalnika), zato je za razvoj priporočljiv JDK:

```bash
sudo apt install -y openjdk-25-jdk-headless
```

```bash
cd teren-api && ./mvnw test
```

Brez nameščenega JDK lahko teste poženeš v Dockerju:

```bash
cd teren-api && docker run --rm --network host --user "$(id -u):$(id -g)" --group-add "$(getent group docker | cut -d: -f3)" -e HOME=/tmp/home -v "$HOME/.m2":/tmp/home/.m2 -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD":/workspace -w /workspace eclipse-temurin:25-jdk ./mvnw -B -Dmaven.repo.local=/tmp/home/.m2/repository test
```

## Logi

```bash
docker compose logs -f api teren
```

API na konzolo piše INFO/WARN/ERROR, v datoteko pa vse, tudi DEBUG (dnevna rotacija, 14 dni):

```bash
docker compose exec api tail -f /app/logs/teren-api.log
```

Nivo za `com.sledenje.teren` se nastavi z `API_LOG_LEVEL` v `.env`. Celotno konfiguracijo logiranja
lahko zamenjaš z zunanjo datoteko prek `LOGGING_CONFIG=/pot/do/logback.xml`.

## Struktura API-ja

Po smernicah: `com.sledenje.teren.<kontekst>.<nivo>.<komponenta>`. Konteksti so `client`, `vehicle`, `user`,
`workorder` in `plan`; spodaj je kot vzorec **client** (stranke):

```
com/sledenje/teren/
├── common/                          skupno vsem kontekstom
│   ├── domain/exception/            DomainException, ResourceNotFoundException (404),
│   │                                ResourceConflictException (409), ExternalServiceException (502)
│   ├── infrastructure/config/       OpenApiConfig, TerenApiProperties
│   ├── infrastructure/external/     ExternalListFetcher (GET JSON seznama za sinhronizacije)
│   ├── domain/model/                InstallerName, WorkOrderType (skupna nalogom in planu)
│   ├── infrastructure/persistence/  TimestampedJpaEntity (createdAt/updatedAt kot Prisma),
│   │                                PrismaBaselineMigrationStrategy (prehod sheme s Prisme na Flyway)
│   ├── infrastructure/security/     ApiTokenFilter (/api/** zahteva Authorization: Bearer API_TOKEN)
│   └── presentation/controller/     GlobalExceptionHandler (napake → ProblemDetail, brez stacktrace)
└── client/
    ├── domain/
    │   ├── model/                   Client, NewClient, ClientUpdate, ClientSyncConfig, SyncedClient …
    │   ├── service/                 ClientService, ClientSyncService                (interface)
    │   ├── repository/              ClientRepository, ClientSyncConfigRepository,
    │   │                            ExternalClientRepository (zunanji sistem)      (interface)
    │   └── exception/               ClientNotFoundException, ClientInUseException …
    ├── infrastructure/
    │   ├── api/service/             ClientServiceImpl, ClientSyncServiceImpl
    │   ├── api/repository/          ClientRepositoryImpl, ClientSyncConfigRepositoryImpl,
    │   │                            ExternalClientRepositoryImpl (RestClient)
    │   ├── persistence/             JPA entitete in repozitoriji
    │   └── util/                    ClientJpaConverter, ClientDtoConverter
    └── presentation/
        ├── controller/              ClientController      →  /api/v1/clients (CRUD, /import)
        │                            ClientSyncController  →  /api/v1/clients/sync-config, /sync
        └── dto/                     zahtevki in odgovori
```

Enako je zgrajen kontekst **vehicle** (`/api/v1/vehicles`, sinhronizacija na `/api/v1/vehicles/sync-config` in `/sync`).

Kontekst **user** (`/api/v1/users`, prijava na `/api/v1/auth/login`): gesla zgošči in preveri API (BCrypt, cena 12,
združljivo z obstoječimi hashi iz bcryptjs), zgoščeno geslo nikoli ne zapusti API-ja. Sejo (podpisan piškotek) še
vedno ustvari aplikacija; ob vsakem zahtevku prebere uporabnika s pravicami iz API-ja, da odvzem pravic velja takoj.
Omejevanje poskusov prijave (5 poskusov, nato 15 min) je v aplikaciji, pred klicem API-ja.

Kontekst **workorder** (`/api/v1/work-orders`): seznam s filtri (datum od–do, stranka, del registrske ali IMEI-ja,
vrsta, zahtevnost, monter, opcija, model naprave, krivec, avtor) in razvrščanjem, podrobnosti s fotografijami in
zgodovino urejanj, seznam IMEI-jev. Stranko in avtorja bere prek lastnih entitet samo za branje
(`WorkOrderClientJpaEntity`, `WorkOrderUserJpaEntity`) na tabelah `clients` in `users`. Datoteke (fotografije,
podpis) ostanejo na disku aplikacije; API hrani samo poti, PDF in Excel še vedno izdela aplikacija.

Vnos naloga (`POST /api/v1/work-orders`) je ena transakcija: dnevna številka (`DDMMLLLLNNN`, dan po
`APP_TIME_ZONE`, privzeto Europe/Ljubljana), nalog z monterji, opcijami in napravami, vozilo (če ga še ni) in
zapis v planu (planirani nalog ali dogodek istega monterja pri stranki). Aplikacija nato shrani fotografije in
podpis na disk in poti sporoči na `/attachments`. Urejanje (`PUT`) zgodovino sprememb izračuna v API-ju.

Kontekst **plan** (`/api/v1/plan-groups`, `/api/v1/planned-tasks`): dogodki v obdobju, vnos, urejanje, premik,
brisanje in planirani nalogi. Isti monter (pri »Ostalo« po imenu) nima prekrivajočih se dogodkov: ob vnosu,
urejanju ali premiku API prekrivajoče dogodke verižno zamakne za konec novega okna.

Prvega admina na prazni bazi ustvari `FirstAdminInitializer` (kontekst user) iz `SEED_ADMIN_EMAIL`.

Controller injektira samo `ClientService` (interface iz domain nivoja). Pretvorniki so statični, zato se ne injektirajo.

## Baza in shema – pomembno

- **Shemo upravlja Flyway v teren-api** (`teren-api/src/main/resources/db/migration`). `V1__shema_iz_prisme.sql`
  je shema vseh 11 nekdanjih Prisma migracij. Sprememba sheme = nova datoteka `V2__opis.sql` (nikoli ne spreminjaj
  že izvedene migracije). API ima `ddl-auto: validate`: ob zagonu preveri, da se entitete ujemajo s tabelami.
- **Ob zagonu API** (`PrismaBaselineMigrationStrategy`):
  - prazna baza → Flyway izvede vse migracije;
  - baza iz stare aplikacije (tabela `_prisma_migrations`) z vsemi Prisma migracijami → označi se kot V1 brez
    sprememb; če ji katera manjka, se API ne zažene (najprej jo posodobi s staro aplikacijo);
  - baza z `flyway_schema_history` → izvedejo se samo nove migracije;
  - neprazna baza brez obeh zgodovin (tuje tabele) → API se ne zažene in je ne spreminja.
- **Pasti pri preslikavi Prisma sheme v JPA** (že urejene v `application.yml` in `TimestampedJpaEntity`):
  - Stolpci so camelCase v narekovajih (`"createdAt"`), zato je nastavljena strategija poimenovanja brez snake_case.
  - Časi so `TIMESTAMP(3)` v UTC, zato je nastavljen `hibernate.jdbc.time_zone: UTC`. Test
    `readsClientWrittenByPrismaWithUtcTimestamps` pade, če se to odstrani (zamik 2 uri).
  - Enumi so Postgres tipi (`"ClientSource"`), zato je na stolpcu `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
  - ID-je in `updatedAt` generira aplikacija, ne baza. API za nove zapise uporablja UUID, obstoječi ostanejo cuid.

## Selitev na drug strežnik s pravo bazo

Compose za strežnik je v `teren-server/docker-compose.yml`: brez servisa `db`, povezava na bazo
(privzeto `192.168.0.30:5433`) iz `DB_*` v `.env`, API brez objavljenega porta.

1. Na strežniku: `git clone https://github.com/farjlcn1/teren`, nato `teren-server/docker-compose.yml`
   kopiraj v koren kot `docker-compose.yml`. `teren-server/.env` ni v gitu: prenesi ga s tega
   računalnika (`scp`) v koren kot `.env`.
2. Na strežniku baze ustvari uporabnika in bazo z geslom `DB_PASSWORD` iz `.env`; `pg_hba.conf` mora
   dovoliti povezave z IP-ja novega strežnika.
3. `docker compose up -d --build`. API v prazni bazi ustvari shemo in (s `SEED_ADMIN_EMAIL`) prvega admina;
   bazo iz stare aplikacije označi kot izhodišče brez sprememb (glej [Baza in shema](#baza-in-shema--pomembno)).
4. Za prenos testnih podatkov: `docker compose exec -T db pg_dump -U teren -Fc teren > teren.dump`
   na tem računalniku, nato `pg_restore` v prazno ciljno bazo **pred** prvim zagonom.

## Prehod aplikacije na API

Cilj: aplikacija teren do baze ne dostopa neposredno (Prisma), ampak vse gre prek teren-api.
Prehod poteka po kontekstih; po vsakem koraku aplikacija v celoti deluje.

| Korak | Kaj | Stanje |
|---|---|---|
| 1 | Žeton med aplikacijo in API-jem (`API_TOKEN`), odjemalec `teren/src/lib/api/`, **stranke** in sinhronizacija strank | narejeno |
| 2 | **Vozila** (registracije) in sinhronizacija vozil | narejeno |
| 3 | **Uporabniki**, prijava in seje | narejeno |
| 4a | **Delovni nalogi – branje**: seznam s filtri, podrobnosti, PDF, oba izvoza | narejeno |
| 4b | **Delovni nalogi – pisanje**: vnos, urejanje z zgodovino, fotografije, podpis, e-pošta | narejeno |
| 5 | **Plan**: dogodki in planirana opravila | narejeno |
| 6 | Shemo prevzame API (Flyway); aplikaciji se odvzameta Prisma in `DATABASE_URL`, prvega admina ustvari API | narejeno |

Kako aplikacija kliče API:

- `teren/src/lib/api/http.ts`: `apiRequest` doda `Authorization: Bearer $API_TOKEN`, napake API-ja
  (ProblemDetail) pretvori v `ApiError` s slovenskim sporočilom za obrazec.
- En modul na kontekst (`teren/src/lib/api/clients.ts` …), samo na strežniku (`server-only`).
- Pravice uporabnika (`canManageClients` …) preveri aplikacija pred klicem. API zaupa vsakemu, ki ima
  žeton, zato ne sme biti dosegljiv od zunaj (na strežniku nima objavljenega porta).
- Aplikacija nima dostopa do baze (ne `DATABASE_URL` ne Prisme); fotografije in podpise hrani na svojem disku.
