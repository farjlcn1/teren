# Teren – testno okolje (aplikacija + API)

Dva Docker kontejnerja, aplikacija **teren** (Next.js) in **teren-api** (Spring Boot), ter lokalna testna baza.
Kasneje se okolje preseli na drug strežnik s pravo bazo (glej [Selitev](#selitev-na-drug-strežnik-s-pravo-bazo)).

```
                    teren.farjolcn.com (Cloudflare tunel)
                                  │
┌─────────────────────────────────▼──────────────── Docker (omrežje "teren") ──┐
│  teren   Next.js :3000  ──── API_URL=http://api:8080 ────►  api  Spring Boot :8080
│     │  (Prisma, prehodno)                                         │ (JPA)
│     └──────────────────────────►  db  Postgres 17 :5432  ◄────────┘
└──────────────────────────────────────────────────────────────────────────────┘
```

| Mapa / datoteka | Vsebina |
|---|---|
| `docker-compose.yml` | servisi `db`, `teren`, `api` + `teren-seed` in `tunnel` (profila) |
| `teren/` | klon [farjlcn1/teren](https://github.com/farjlcn1/teren), nespremenjen |
| `teren-api/` | Spring Boot API po internih smernicah kodiranja |
| `.env` | skrivnosti (geslo baze, `SESSION_SECRET` …), **ne sodi v git** |
| `.env.example` | predloga za `.env` |

## Zagon

```bash
docker compose up -d --build
```

Vrstni red zagona določa compose: `db` → `teren` (požene Prisma migracije) → `api` (preveri shemo).

Prvi admin uporabnik (izpiše geslo, shrani si ga):

```bash
docker compose --profile seed run --rm teren-seed
```

> Seed admina ne nastavi pravice `canManagePlan`. Po prijavi jo dodaš v *Admin → Uporabniki*.

| Naslov | Kaj |
|---|---|
| http://localhost:3000 | aplikacija teren |
| http://localhost:8080/swagger-ui.html | Swagger dokumentacija API-ja |
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

Testi tečejo na lastni začasni bazi v Dockerju (Testcontainers) s shemo iz
`teren-api/src/test/resources/db/teren-schema.sql`.

Na tem računalniku je nameščena samo Java JRE (brez prevajalnika), zato je za razvoj priporočljiv JDK:

```bash
sudo apt install -y openjdk-25-jdk-headless
```

```bash
cd teren-api && ./mvnw test
```

Brez nameščenega JDK lahko teste poženeš v Dockerju:

```bash
cd teren-api && docker run --rm --network host --user "$(id -u):$(id -g)" --group-add "$(getent group docker | cut -d: -f3)" -e HOME=/tmp/home -v "$HOME/.m2":/tmp/home/.m2 -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD":/workspace -w /workspace eclipse-temurin:25-jdk ./mvnw -B test
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

Po smernicah: `com.sledenje.teren.<kontekst>.<nivo>.<komponenta>`. Trenutno je narejen kontekst
**client** (stranke) kot vzorec za ostale (`vehicle`, `user`, `workorder`, `plan`):

```
com/sledenje/teren/
├── common/                          skupno vsem kontekstom
│   ├── domain/exception/            DomainException, ResourceNotFoundException
│   ├── infrastructure/config/       OpenApiConfig, TerenApiProperties
│   ├── infrastructure/persistence/  TimestampedJpaEntity (createdAt/updatedAt kot Prisma)
│   └── presentation/controller/     GlobalExceptionHandler (napake → ProblemDetail, brez stacktrace)
└── client/
    ├── domain/
    │   ├── model/                   Client, ClientSource, NewClient
    │   ├── service/                 ClientService            (interface)
    │   ├── repository/              ClientRepository         (interface)
    │   └── exception/               ClientNotFoundException
    ├── infrastructure/
    │   ├── api/service/             ClientServiceImpl
    │   ├── api/repository/          ClientRepositoryImpl
    │   ├── persistence/             ClientJpaEntity, ClientJpaRepository
    │   └── util/                    ClientJpaConverter, ClientDtoConverter
    └── presentation/
        ├── controller/              ClientController  →  /api/v1/clients
        └── dto/                     ClientDto, CreateClientRequest
```

Controller injektira samo `ClientService` (interface iz domain nivoja). Pretvorniki so statični, zato se ne injektirajo.

## Baza in shema – pomembno

- **Shemo trenutno upravlja Prisma** (teren ob vsakem zagonu požene `prisma migrate deploy`).
  API ima `ddl-auto: validate`: ob zagonu preveri, da se entitete ujemajo s tabelami, in se ne zažene, če se ne.
- **Ob novi Prisma migraciji** v `teren` posodobi testno shemo API-ja:

  ```bash
  { echo "-- Shema baze Teren za teste API-ja (iz teren/prisma/migrations)."; for d in teren/prisma/migrations/*/; do echo "-- ==== $(basename "$d")"; cat "$d/migration.sql"; done; } > teren-api/src/test/resources/db/teren-schema.sql
  ```

- **Pasti pri preslikavi Prisma sheme v JPA** (že urejene v `application.yml` in `TimestampedJpaEntity`):
  - Stolpci so camelCase v narekovajih (`"createdAt"`), zato je nastavljena strategija poimenovanja brez snake_case.
  - Časi so `TIMESTAMP(3)` v UTC, zato je nastavljen `hibernate.jdbc.time_zone: UTC`. Test
    `readsClientWrittenByPrismaWithUtcTimestamps` pade, če se to odstrani (zamik 2 uri).
  - Enumi so Postgres tipi (`"ClientSource"`), zato je na stolpcu `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
  - ID-je in `updatedAt` generira aplikacija, ne baza. API za nove zapise uporablja UUID, obstoječi ostanejo cuid.

## Selitev na drug strežnik s pravo bazo

1. Na strežnik prenesi to mapo (oziroma kloniraj oba repozitorija) in `.env`.
2. V `docker-compose.yml` odstrani servis `db` in volumen `db_data`, pri `teren` in `api` pa odstrani `depends_on: db`.
3. Nastavi povezavo na pravo bazo (npr. `192.168.0.30:5433`):
   - `teren` → `DATABASE_URL: postgresql://<uporabnik>:<geslo>@192.168.0.30:5433/<baza>`
   - `api` → `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
4. Pri `api` odstrani `ports:`, da je API dosegljiv samo aplikaciji teren.
5. Na strežniku baze mora `pg_hba.conf` dovoliti povezave z IP-ja novega strežnika.
6. Za prenos testnih podatkov: `docker compose exec -T db pg_dump -U teren teren > teren.sql`.

## Naslednji koraki

- Avtentikacija in pravice v API-ju (pred kakršnokoli javno izpostavitvijo API-ja).
- Ostali konteksti: `vehicle`, `user`, `workorder`, `plan`.
- Preklop aplikacije teren s Prisme na klice API-ja (`API_URL` je že nastavljen).
- Ko teren ne bo več dostopal do baze: shemo prevzame API (Flyway, izhodišče je `teren-schema.sql`).
