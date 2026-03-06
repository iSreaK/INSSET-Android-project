# jv-Bench Android (Java)

Application Android pour localiser des bancs, consulter leurs details, puis ajouter des avis.

## Etat actuel
- Projet en Java (pas Kotlin).
- Build debug fonctionnel.
- Navigation principale en place:
  - Carte (ecran d'accueil)
  - Compte (navbar basse)
- Auth Supabase operationnelle:
  - login / register
  - session simple
  - page compte conditionnelle (connecte vs non connecte)
- UI de base en francais + theme couleur + logo app.

## Stack technique
- Java
- Fragments + XML
- Architecture MVVM simple
- Navigation Component (single activity)
- OpenStreetMap via osmdroid
- Supabase REST/Auth
- DI manuelle (`AppContainer`)

## Structure
- `app/src/main/java/com/example/jvbench/ui`: ecrans (auth, map, account, bench, review)
- `app/src/main/java/com/example/jvbench/domain`: modeles + interfaces repositories
- `app/src/main/java/com/example/jvbench/data`: impl repositories + remote Supabase
- `app/src/main/java/com/example/jvbench/di`: bootstrap applicatif
- `supabase/jvbench_schema.sql`: schema SQL de reference

## Lancer le projet (terminal)
1. Se placer a la racine projet:
   - `cd Android/Projet/INSSET-Android-project`
2. Compiler:
   - `gradle assembleDebug`
3. Installer sur device/emulateur connecte:
   - `gradle installDebug`

## Configuration Supabase
Configurer `gradle.properties` (racine projet):
- `SUPABASE_URL=...`
- `SUPABASE_ANON_KEY=...`

Important:
- Ne pas desactiver le provider Email si vous utilisez login/register par email.
- Si confirmation email active, utiliser une vraie boite mail de test.

## Work in Progress
1. Carte metier
- interactions map avancees
- filtres/recherche

2. Bancs (coeur produit)
- CRUD complet
- gestion image propre (upload/affichage/erreurs)

3. Avis et details
- flux avis/notes complet
- enrichissement detail banc

4. Securite et robustesse
- RLS/policies Supabase a verrouiller
- tests minimaux (auth/navigation/repositories)
- reduction des warnings outillage Gradle/AGP

## Notes
- Ce repo reste evolutif: certaines parties sont deja utilisables, d'autres sont encore en implementation.
