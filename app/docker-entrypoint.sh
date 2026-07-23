#!/bin/sh
set -e

echo "Poganjam migracije baze ..."
node_modules/.bin/prisma migrate deploy

echo "Preverjam zacetnega admin uporabnika ..."
node_modules/.bin/tsx prisma/seed.ts

echo "Zaganjam aplikacijo ..."
exec node server.js
