<p align="center">
  <img src="docs/logo.svg" alt="Logo Loopa" width="88" />
</p>

<h1 align="center">Loopa</h1>

<p align="center">
  <strong>Les retours qui font revenir vos clients.</strong><br />
  Plateforme de gestion des retours et échanges pour boutiques e-commerce (Shopify) : un portail client à vos couleurs,<br />
  un back-office pour décider en un clic, et des remboursements transformés en avoirs et en échanges.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-0e7c6b" />
  <img alt="Spring Boot 3.5" src="https://img.shields.io/badge/Spring%20Boot-3.5-0e7c6b" />
  <img alt="Spring Modulith" src="https://img.shields.io/badge/Spring-Modulith-0e7c6b" />
  <img alt="Nuxt 4" src="https://img.shields.io/badge/Nuxt-4-0e7c6b" />
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-strict-0e7c6b" />
  <img alt="PostgreSQL 17" src="https://img.shields.io/badge/PostgreSQL-17-0e7c6b" />
  <img alt="FR · EN · AR" src="https://img.shields.io/badge/langues-FR%20%C2%B7%20EN%20%C2%B7%20AR-ff6b4a" />
</p>

<p align="center">
  <a href="#-vidéo-de-présentation">Vidéo</a> ·
  <a href="#-captures-décran">Captures</a> ·
  <a href="#-fonctionnalités">Fonctionnalités</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-démarrage">Démarrage</a>
</p>

---

## 🎯 Objectif

Dans beaucoup de boutiques en ligne, un retour se gère encore par WhatsApp, téléphone et tableur : le client attend,
le marchand perd du temps, et presque chaque retour finit en remboursement. **Loopa** structure tout le parcours et le
rend rentable :

| Acteur | Ce que Loopa lui apporte |
|---|---|
| **Client final** | Un portail mobile aux couleurs de la boutique, sans compte : numéro de commande + code reçu, choix des articles, motif, photo si besoin, solution (avoir majoré, échange ou remboursement), suivi du colis. En français, anglais ou arabe. |
| **Marchand (propriétaire, administrateurs)** | Politique de retour paramétrable (délai, approbation automatique, bonus d'avoir, produits exclus, motifs), personnalisation du portail, équipe et rôles, abonnement, connexion Shopify, statistiques. |
| **Agents** | File des retours à traiter, fiche détaillée avec photos et historique, approbation / rejet motivé / réception en un clic, notes internes, raccourcis clavier. |

Pensé pour le e-commerce marocain : **paiement à la livraison** (remboursement par virement, avoir mis en avant),
transporteurs **Amana, Sendit, CTM, Cathedis**, interface **trilingue avec mise en page de droite à gauche**.

## 🎬 Vidéo de présentation

**[▶️ Regarder la présentation (MP4)](loopa-presentation.mp4)** — 1 min 15 : un client retourne deux articles depuis son
téléphone (photo d'un article endommagé, avoir majoré, échange approuvé automatiquement, numéro de suivi), puis la
marchande traite la demande dans le back-office (note interne, approbation, réception, résolution exécutée) et passe
en revue les réglages.

Captures et vidéo se régénèrent après une évolution de l'interface (API et fronts lancés, voir
[Démarrage](#-démarrage)) :

```bash
pip install playwright pillow          # + ffmpeg et la police Inter
python3 tools/screenshots/capture.py --video   # parcours réels → docs/screenshots + vidéos brutes
python3 tools/screenshots/make_video.py        # montage 1080p → loopa-presentation.mp4
```

## 📸 Captures d'écran

### Site vitrine

| Accueil | Tarifs |
|---|---|
| ![Accueil](docs/screenshots/01-vitrine-accueil.png) | ![Tarifs](docs/screenshots/02-vitrine-tarifs.png) |

### Portail client (mobile)

| Retrouver sa commande | Code à usage unique | Choix des articles | Motif et photo |
|---|---|---|---|
| ![Accueil du portail](docs/screenshots/03-portail-accueil.png) | ![Code](docs/screenshots/04-portail-code.png) | ![Articles](docs/screenshots/05-portail-articles.png) | ![Motif et photo](docs/screenshots/06-portail-motif-photo.png) |
| **Solution : l'avoir majoré en premier** | **Récapitulatif** | **Demande envoyée** | **Retour approuvé automatiquement** |
| ![Solution](docs/screenshots/07-portail-solution.png) | ![Récapitulatif](docs/screenshots/08-portail-recapitulatif.png) | ![Demande envoyée](docs/screenshots/09-portail-demande-envoyee.png) | ![Retour approuvé](docs/screenshots/10-portail-retour-approuve.png) |
| **Suivi du colis** | **En arabe, de droite à gauche** | | |
| ![Suivi](docs/screenshots/11-portail-suivi.png) | ![Portail en arabe](docs/screenshots/12-portail-arabe-rtl.png) | | |

### Back-office marchand

| Connexion | Tableau de bord |
|---|---|
| ![Connexion](docs/screenshots/13-admin-connexion.png) | ![Tableau de bord](docs/screenshots/14-admin-tableau-de-bord.png) |
| **Liste des retours** | **Fiche d'un retour : photos, solution, historique** |
| ![Liste des retours](docs/screenshots/15-admin-liste-retours.png) | ![Détail d'un retour](docs/screenshots/16-admin-detail-retour.png) |
| **Politique de retour et motifs** | **Personnalisation du portail, aperçu en direct** |
| ![Politique de retour](docs/screenshots/17-admin-politique.png) | ![Personnalisation](docs/screenshots/18-admin-personnalisation-portail.png) |
| **Équipe et rôles** | **Abonnement et quota** |
| ![Équipe](docs/screenshots/19-admin-equipe.png) | ![Abonnement](docs/screenshots/20-admin-abonnement.png) |
| **Intégration Shopify** | **Responsive : tableau de bord sur mobile** |
| ![Intégrations](docs/screenshots/21-admin-integrations.png) | ![Admin mobile](docs/screenshots/22-admin-mobile.png) |
| **Approbation groupée et export CSV** | **Mon compte : changement de mot de passe** |
| ![Approbation groupée](docs/screenshots/23-admin-approbation-groupee.png) | ![Mon compte](docs/screenshots/24-admin-mon-compte.png) |

## 🧱 Architecture

```mermaid
flowchart LR
    subgraph Fronts["Fronts Nuxt 4 (monorepo pnpm + Turborepo)"]
        P["📱 Portail client<br/>SSR · mobile-first<br/>/&lt;boutique&gt;"]
        A["🖥️ Back-office<br/>SPA · Nuxt UI"]
        V["🌐 Vitrine<br/>SSG · SEO"]
    end
    L["📦 Layers partagés<br/>ui · api · i18n"]
    P --- L
    A --- L
    V --- L
    subgraph Serveur["loopa-api — Spring Boot 3.5 · Java 21 · monolithe modulaire"]
        direction TB
        M["tenancy · catalog · policy · portal<br/><b>returns</b> · resolution · notification<br/>analytics · billing · storage · integration"]
    end
    P -- "jeton portail (après code)" --> M
    A -- "jeton marchand (JWT)" --> M
    M --> DB[("PostgreSQL 17<br/>Flyway · tenant_id")]
    M --> F[("Fichiers<br/>photos")]
    M -. "OAuth · GraphQL Admin<br/>webhooks HMAC" .- S["Shopify"]
    M -. emails .-> E["SMTP"]
```

- **Monolithe modulaire** ([ADR 0001](docs/adr/0001-monolithe-modulaire.md)) : un seul déploiement, 12 modules métier
  aux frontières vérifiées par Spring Modulith (`ModularityTest` fait échouer le build si un module touche aux classes
  internes d'un autre). Les modules communiquent par **événements de domaine** persistés dans une outbox.
- **Architecture hexagonale** dans le module cœur `returns` : agrégat `ReturnRequest` en Java pur (machine à 7 états),
  cas d'usage, ports, adaptateurs web et JPA.
- **Multi-boutique** : chaque table métier porte `tenant_id`, filtré automatiquement par Hibernate (`@TenantId`) à partir
  du jeton ; Row Level Security PostgreSQL prête dans la migration.
- **Fronts** : trois applications Nuxt qui partagent le design system, le client API typé et les traductions via des
  Nuxt Layers. Aucun composant n'appelle `fetch` directement.

### Parcours d'un retour (exemple)

```mermaid
sequenceDiagram
    actor Client
    participant Portail
    participant API as loopa-api
    participant Marchand as Back-office
    participant Shopify
    Client->>Portail: n° de commande + email
    Portail->>API: POST /api/portal/v1/sessions
    API-->>Client: code à 6 chiffres (email)
    Client->>Portail: code, articles, motifs, photo, solution
    Portail->>API: POST /api/portal/v1/returns (prix relus depuis la commande)
    API->>API: éligibilité, quota, approbation automatique ?
    API-->>Client: email « demande reçue » ou « approuvé »
    Marchand->>API: POST /returns/{id}/approve puis /receive
    API->>Shopify: carte cadeau, remboursement ou commande d'échange
    API-->>Client: email « retour terminé »
```

Le client ne choisit que les lignes, quantités, motifs et la solution : titres, prix et bonus sont toujours relus côté
serveur. Une résolution en échec laisse le retour « reçu », avec l'erreur visible et un bouton pour la relancer.

### Cycle de vie d'un retour

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED: demande du client
    SUBMITTED --> APPROVED: auto ou agent
    SUBMITTED --> REJECTED: motif obligatoire
    SUBMITTED --> CANCELLED: annulation client
    APPROVED --> IN_TRANSIT: n° de suivi
    APPROVED --> RECEIVED
    APPROVED --> CANCELLED
    IN_TRANSIT --> RECEIVED: colis reçu
    RECEIVED --> RESOLVED: résolution exécutée
    RECEIVED --> REJECTED: contrôle qualité KO
    RESOLVED --> [*]
    REJECTED --> [*]
    CANCELLED --> [*]
```

### Organisation du code

```
loopa/
├─ backend/                          API Spring Boot (Maven)
│  └─ src/main/java/app/loopa/
│     ├─ shared/                     Money, erreurs RFC 9457, contexte tenant, horloge
│     ├─ security/                   chaînes de sécurité, jetons JWT (HS256), Principals
│     ├─ tenancy/                    boutiques, connexion, équipe, personnalisation
│     ├─ catalog/                    copie locale des commandes
│     ├─ policy/                     politique de retour, motifs, éligibilité, approbation auto
│     ├─ returns/                    ★ cœur : domain / application / adapter (hexagonal)
│     ├─ portal/                     API du portail client (code à usage unique, parcours)
│     ├─ resolution/                 exécution des avoirs, échanges, remboursements
│     ├─ integration/                Shopify : OAuth, webhooks HMAC, synchro, GraphQL
│     ├─ notification/               emails trilingues
│     ├─ analytics/                  indicateurs du tableau de bord (SQL)
│     ├─ billing/                    plans et quotas
│     ├─ storage/                    photos déposées
│     └─ demo/                       boutique de démonstration (profil dev)
│  └─ src/main/resources/
│     ├─ db/migration/V1__init.sql   schéma complet
│     └─ demo/seed.json              données de démo (partagées avec l'API simulée)
├─ frontend/                         monorepo pnpm + Turborepo
│  ├─ apps/portal                    portail client       → http://localhost:3001/atlas
│  ├─ apps/admin                     back-office          → http://localhost:3002
│  ├─ apps/web                       vitrine              → http://localhost:3000
│  ├─ layers/ui                      design system : tokens, composants (StatusBadge, KpiCard, DailyChart…)
│  ├─ layers/api                     client HTTP, types, composables Pinia Colada
│  ├─ layers/i18n                    fr · en · ar (RTL)
│  └─ tools/mock-api                 API simulée (même contrat HTTP) pour développer sans Java
├─ infra/                            docker compose (PostgreSQL, Redis, MinIO, Mailpit)
├─ tools/                            génération des données de démo, captures et vidéo
├─ docs/                             ADR, captures d'écran
└─ .github/workflows/ci.yml          CI back + front
```

## 🧰 Stack

| Couche | Technologies |
|---|---|
| Backend | Java 21 (threads virtuels), Spring Boot 3.5 (Web MVC, Data JPA, Security, OAuth2 Resource Server/JWT, Validation, Mail, Actuator), **Spring Modulith** (modules, événements, outbox JDBC), Hibernate 6 multi-tenant, Flyway, PostgreSQL 17, springdoc OpenAPI, Micrometer/Prometheus |
| Fronts | **Nuxt 4**, Vue 3.5, TypeScript strict, **Nuxt UI 4** (Tailwind CSS 4, Reka UI), Pinia + **Pinia Colada**, @nuxtjs/i18n, Zod 4, VueUse, polices auto-hébergées (Fontsource) |
| Monorepo | pnpm workspaces + Turborepo, Nuxt Layers (`ui`, `api`, `i18n`) |
| Intégrations | Shopify (OAuth, webhooks HMAC-SHA256, GraphQL Admin API : cartes cadeaux, remboursements, commandes d'échange), SMTP |
| Tests | JUnit 5, AssertJ, Spring Modulith (`ApplicationModules.verify()`), Testcontainers · vue-tsc · Playwright |
| Infra | Docker (image multi-étapes, utilisateur non root), docker compose, GitHub Actions |

## 🚀 Démarrage

### Démo rapide, sans Java ni base de données

L'API simulée reproduit le contrat HTTP de `loopa-api` avec les mêmes données de démonstration :

```bash
cd frontend
pnpm install
pnpm mock          # API simulée → http://localhost:8080
pnpm dev           # dans un 2ᵉ terminal : vitrine :3000, portail :3001, back-office :3002
```

### Stack complète

Prérequis : Java 21, Maven 3.9, Node 22, pnpm 10, Docker.

```bash
docker compose -f infra/docker-compose.yml up -d          # PostgreSQL, Redis, MinIO, Mailpit

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev        # API → http://localhost:8080 (Swagger : /swagger-ui.html)

cd ../frontend
pnpm install && pnpm dev
```

Le profil `dev` charge la boutique de démonstration, renvoie le code du portail dans la réponse (`devCode`) et envoie
les emails à Mailpit (http://localhost:8025).

### Comptes de démonstration

| Rôle | Email | Mot de passe |
|---|---|---|
| Propriétaire | yasmine@atlas-store.ma | Loopa2026! |
| Administratrice | salma@atlas-store.ma | Loopa2026! |
| Agent | karim@atlas-store.ma | Loopa2026! |
| Client (portail `/atlas`) | commande **#1042**, nadia@exemple.ma ou 0612345678 | code affiché en démo |

## ✨ Fonctionnalités

| Domaine | Fonctionnalités | Rôles |
|---|---|---|
| **Portail client** | Identification sans compte (n° de commande + email ou téléphone, code à 6 chiffres valable 10 min, 5 essais), articles éligibles avec quantité, motifs, **photo obligatoire** selon le motif, échange avec préférence de taille, récapitulatif, suivi, saisie du n° de suivi, annulation | Client |
| **Solutions** | **Avoir majoré** (bonus shop-now affiché en premier), échange, remboursement (virement pour le paiement à la livraison) | Client, Marchand |
| **Règles** | Délai de retour, produits exclus, **approbation automatique** sous un plafond et sans motif à risque, motifs trilingues configurables | Administrateurs |
| **Traitement** | File par statut avec compteurs, recherche, pagination par curseur, fiche détaillée (photos, solution, client, suivi), approbation, **approbation groupée** (jusqu'à 100 retours, un échec n'annule pas les autres), **rejet motivé**, réception, relance de la résolution, notes internes, historique complet, raccourcis A / R | Agents, Administrateurs |
| **Export** | **CSV** des retours, filtrable par statut (séparateur « ; », UTF-8 avec BOM pour Excel, protection contre l'injection de formules) | Agents, Administrateurs |
| **Résolution** | À la réception : carte cadeau, remboursement ou commande d'échange créés dans Shopify (simulés pour la boutique de démo), erreurs visibles et relançables | Automatique |
| **Statistiques** | Retours sur 30 jours, valeur conservée (avoirs + échanges), délai médian d'approbation, approbations automatiques, bonus offerts, motifs, solutions, produits les plus retournés, histogramme quotidien | Équipe |
| **Personnalisation** | Nom, couleur, logo, message d'accueil, **aperçu en direct**, lien du portail | Administrateurs |
| **Équipe** | Invitation avec mot de passe temporaire, rôles propriétaire / administrateur / agent, désactivation | Administrateurs |
| **Abonnement** | Plans Starter, Growth, Scale, quota mensuel de retours appliqué au portail | Administrateurs |
| **Shopify** | Installation OAuth (state signé, HMAC vérifié), jeton chiffré AES-GCM, webhooks commandes / désinstallation / RGPD, synchronisation des 60 derniers jours | Administrateurs |
| **Notifications** | Emails au client à chaque étape, dans sa langue (fr, en, ar) ; **email à l'équipe** pour chaque nouvelle demande à traiter (pas pour les retours approuvés automatiquement) | Automatique |
| **Compte** | Changement de mot de passe (indispensable après une invitation avec mot de passe temporaire) | Équipe |
| **Langues** | Interfaces en **français, anglais et arabe** (RTL), montants et dates localisés | Tous |
| Feuille de route | Étiquettes transporteurs, SMS / WhatsApp, Billing API Shopify, WooCommerce et YouCan, anonymisation automatique sur `customers/redact` | — |

## 🔐 Sécurité

- **Jetons séparés** : marchand (scope `admin` ou `agent`, 8 h) et portail (scope `portal`, 30 min, **limité à une seule
  commande**). Un client ne peut lire ni modifier que les retours de sa commande.
- **Isolation des boutiques** par `tenant_id` filtré par Hibernate ; Row Level Security PostgreSQL prête à activer.
- **Codes à usage unique** hachés (SHA-256 salé), 10 minutes, 5 essais, 5 codes par heure et par commande ; réponses
  identiques que la commande ou le contact soit faux.
- **Shopify** : vérification HMAC de l'OAuth et des webhooks (comparaison à temps constant), `state` signé et daté,
  domaine `*.myshopify.com` validé, anti-rejeu des webhooks, jetons d'accès chiffrés AES-256-GCM.
- **Photos** : types et taille contrôlés (10 Mo), nom de stockage aléatoire, servies uniquement à l'équipe de la boutique
  (`Cache-Control: no-store`, `nosniff`).
- Erreurs normalisées **RFC 9457** sans trace technique, validation Bean Validation sur toutes les entrées, mots de passe
  hachés (bcrypt via `DelegatingPasswordEncoder`), CORS restreint aux fronts.

## 🏭 Production

Variables principales :

| Variable | Rôle |
|---|---|
| `LOOPA_DB_URL`, `LOOPA_DB_USER`, `LOOPA_DB_PASSWORD` | PostgreSQL |
| `LOOPA_JWT_SECRET` | secret des jetons (≥ 32 octets) |
| `LOOPA_ENCRYPTION_KEY` | clé AES-256 des jetons Shopify (32 octets en Base64) |
| `LOOPA_SHOPIFY_API_KEY`, `LOOPA_SHOPIFY_API_SECRET` | application Shopify (Partner Dashboard) |
| `LOOPA_API_URL`, `LOOPA_ADMIN_URL`, `LOOPA_PORTAL_URL` | URL publiques (OAuth, liens des emails) |
| `LOOPA_CORS_ORIGINS` | origines autorisées des fronts |
| `LOOPA_MAIL_HOST`, `LOOPA_MAIL_PORT`, `LOOPA_MAIL_USER`, `LOOPA_MAIL_PASSWORD`, `LOOPA_MAIL_FROM` | SMTP |
| `LOOPA_STORAGE_DIR` | dossier des photos déposées |
| `NUXT_PUBLIC_API_BASE` | URL de l'API pour les trois fronts |

```bash
docker build -t loopa-api backend      # image multi-étapes, JRE 21, utilisateur non root, healthcheck
cd frontend && pnpm build              # .output/ de chaque application Nuxt
```

## ✅ Qualité

| Niveau | Outil | Commande |
|---|---|---|
| Domaine et sécurité | JUnit 5, AssertJ (machine à états, signatures Shopify, identification client) | `mvn -f backend/pom.xml test` |
| Frontières des modules | Spring Modulith | `ModularityTest` (dans `mvn test`) |
| Types des fronts | vue-tsc (TypeScript strict) | `cd frontend && pnpm typecheck` |
| Parcours de bout en bout | Playwright (portail + back-office) | `python3 tools/screenshots/capture.py` |
| CI | GitHub Actions | `.github/workflows/ci.yml` |

## 🌍 Internationalisation

- Dictionnaires partagés par les trois fronts : `frontend/layers/i18n/i18n/locales/{fr,en,ar}.json`.
- **RTL** : attribut `dir` posé sur `<html>` selon la langue, propriétés logiques Tailwind (`ps-*`, `ms-*`, `start-*`,
  `text-start`), police IBM Plex Sans Arabic.
- Les montants et dates passent par `formatMoney` / `formatDate` (chiffres latins, usage courant au Maroc).
- La langue choisie par le client sur le portail est enregistrée avec son retour et utilisée pour ses emails.

## 🧪 Données de démonstration

`tools/demo-seed/generate.py` génère `backend/src/main/resources/demo/seed.json` (boutique « Atlas Store », 100
commandes, 84 retours répartis sur 80 jours, dates relatives pour que la démo reste à jour). Le même fichier alimente le
profil `dev` du back-end et l'API simulée (`POST /__reset` remet celle-ci à zéro).
