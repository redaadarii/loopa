# ADR 0001 — Monolithe modulaire Spring Modulith

- Statut : accepté
- Date : 2026-11-06

## Contexte

Loopa démarre avec une petite équipe (4 à 5 personnes) et un domaine encore mouvant (retours, échanges,
résolutions, intégrations e-commerce). Des microservices dès le départ multiplieraient l'infrastructure,
les transactions distribuées et le coût d'exploitation sans bénéfice à ce volume.

## Décision

Un seul déploiement Spring Boot (`loopa-api`), découpé en modules métier : `tenancy`, `catalog`, `policy`,
`returns` (cœur), `portal`, `resolution`, `integration`, `notification`, `analytics`, `billing`, `storage`, `demo`,
plus `shared` (module ouvert) et `security`.

- Chaque module n'expose que son package racine ; ses sous-packages sont internes.
- Les modules communiquent par événements de domaine, persistés dans l'outbox Spring Modulith.
- `ModularityTest` fait échouer le build en cas de dépendance interdite ou de cycle.
- Module cœur `returns` : architecture hexagonale (`domain` en Java pur, `application`, `adapter/in`, `adapter/out`).
- Modules plus simples : un sous-package `internal` (contrôleur, service, entités) ; seule leur façade publique
  (`XxxApi` + records) est visible des autres modules.
- Les écouteurs d'événements sont asynchrones et fixent le tenant à partir de l'événement avant toute transaction.

## Conséquences

- Une seule base, des transactions locales, un déploiement simple.
- Un module peut être extrait en service plus tard (`integration` ou `notification` en premier) en remplaçant
  les événements internes par un broker (Kafka), sans toucher au module `returns`.
