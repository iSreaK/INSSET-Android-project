# jv-Bench

> Application Android Java pour localiser des bancs publics sur une carte, consulter leurs détails et y ajouter des avis.

Projet réalisé dans le cadre du module **Développement Mobile Android** — Master CCM-M1, INSSET (2025-2026).

---
## Fonctionnalités

### Obligatoire (sujet)
-  Application Android avec interface graphique
-  Géolocalisation Android (récupération GPS, permissions runtime, affichage cartographique)
-  Service cartographique (OpenStreetMap via osmdroid)
-  Backend cloud (Supabase REST / Auth / Storage / Realtime)
-  Architecture modulaire (carte et backend remplaçables sans refactor massif)

### Option 1 — Élémentaire
-  Authentification email/password
-  Affichage de la position de l'utilisateur sur la carte (overlay point bleu)
-  Ajout de points d'intérêt (titre, description, position GPS, image)
-  Sauvegarde sur Supabase
-  Affichage des points sur la carte avec clustering
-  Écran de détail avec consultation des informations

### Option 2 — Intermédiaire
-  Authentification complète (login / register / logout / refresh token)
-  Système de rôles `USER` / `MODERATOR` / `ADMINISTRATOR` avec panel d'administration
-  Upload d'images sur Supabase Storage (avec lecture EXIF GPS)
-  Filtrage des bancs par distance (Haversine, slider 50-1000 m)
-  Mise à jour temps réel des bancs (WebSocket Supabase Realtime)
-  BroadcastReceiver de connectivité (`NetworkStateReceiver`)

### Option 3 — Avancé (partiel)
-  Géofencing à l'arrivée d'un banc (rayon 15 m, notification)
-  Service Android en arrière-plan (Foreground Service typé `location`)
-  Synchronisation périodique (refresh des bancs toutes les 30 min)
-  Navigation externe (Intent `geo:` vers Google Maps / Waze / OsmAnd)
-  Abstraction des services (interface `MapService`, swap de provider sans toucher l'UI)
-  (Partiel) Firebase Cloud Messaging (notifications locales utilisées à la place — backend Supabase)

---

## Architecture

Le projet suit le pattern **MVVM** (Model-View-ViewModel) combiné au **Repository Pattern** et à une **DI manuelle** via une composition root.

```
   UI (Fragments + ViewModels)
            │
            │  expose LiveData<UiState>
            ▼
   Domain (Modèles + interfaces Repository)
            │
            │  implémenté par
            ▼
   Data (Implémentations Supabase + Mappers + ApiClient)
            │
            │  HTTPS + WebSocket
            ▼
   Supabase (Auth · REST · Storage · Realtime · RLS)
```

### Principes appliqués

- **Single-Activity** : une seule `MainActivity` qui héberge un `NavHostFragment`. Tous les écrans sont des Fragments.
- **Couche Domain pure Java** : aucune dépendance Android, donc testable hors device.
- **Abstraction MapService** : l'UI ne connaît ni osmdroid ni Google Maps. Pour swapper, on remplace une seule ligne dans `AppContainer`.
- **Sécurité multi-niveaux** : l'UI cache les actions interdites (UX), Supabase **applique les Row Level Security** côté base de données (vraie sécurité).

---

## Choix techniques

| Domaine | Choix | Justification |
|---|---|---|
| Langage | **Java 17** | Maîtrise commune de l'équipe ; perte assumée des 2 points bonus Kotlin |
| Build | Gradle (Groovy DSL), AGP 8.5.2, compileSdk 35, minSdk 24 | Versions modernes, supporte Android 7+ |
| UI | Fragments + XML, Material3 | Single-activity + Navigation Component |
| Navigation | `androidx.navigation` 2.8.2 | `nav_graph.xml` centralisé, arguments typés |
| Carte | **osmdroid** 6.1.20 + osmbonuspack 6.9.0 | Gratuit, données OpenStreetMap, clustering inclus |
| Géolocalisation | `FusedLocationProviderClient` (Play Services) | Compromis précision/batterie optimal |
| Réseau | **OkHttp** 4.12.0 + `org.json` | REST direct, pas de SDK propriétaire à arracher en cas de changement de backend |
| Realtime | WebSocket OkHttp + protocole Phoenix Channels Supabase | Push instantané des changements sur les bancs |
| Backend | **Supabase** (Postgres + REST + Auth + Storage + Realtime + RLS) | Open source, RLS native, scalable |
| Images | Glide 4.16 + AndroidX ExifInterface 1.4 | Cache mémoire/disque automatique, lecture EXIF GPS |
| Concurrence | `ExecutorService` + `ResultCallback<T>` | Simple, explicite, pas de dépendance externe |
| DI | Manuelle (`AppContainer`) | Composition root unique, zéro magie, idéale pour 4 développeurs |

---

## Équipe

| Membre | Périmètre principal |
|---|---|
| **Clément** | Carte · Géolocalisation · MapService (abstraction) · Markers · MyLocationOverlay · Realtime |
| **Julien** | Authentification · Session · Profil · Sécurité Supabase / RLS |
| **Kerrian** | CRUD Bancs · Upload image · EXIF GPS · Supabase Storage |
| **Lilian** | Détail banc · Avis · Notation · Moyenne · `review_count` |


