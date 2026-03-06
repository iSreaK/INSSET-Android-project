# jv-Bench Android (Java)

jv-Bench is a collaborative Android app to geolocate public benches, view bench details, and later add reviews/ratings/comments.

This repository is intentionally a **skeleton**:
- architecture and navigation are ready
- main entities are modeled
- Supabase integration points are prepared
- business features remain TODO for team implementation

## Stack
- Java
- XML + Fragments
- MVVM (simple, pedagogical)
- Navigation Component (single Activity)
- OpenStreetMap via osmdroid
- Supabase-ready repository layer (manual DI, no Hilt)

## Architecture
- `com.example.jvbench.di`: `App`, `AppContainer`
- `com.example.jvbench.core`: callbacks, location, permissions, navigation constants
- `com.example.jvbench.domain`: models (`Bench`, `Review`, `User`, `UserRole`, `GeoPoint`) + repository contracts
- `com.example.jvbench.data`: Supabase provider + repository skeletons + mapper placeholder
- `com.example.jvbench.ui`: `auth`, `map`, `benchform`, `benchdetail`, `reviewform`, `main`

## Already prepared
- Login/Register screens and ViewModels
- Map screen with osmdroid map initialization
- Bench form/detail screens and ViewModels
- Review form screen and ViewModel
- Navigation flow:
1. Login -> Register
2. Login -> Map
3. Map -> BenchForm
4. Map -> BenchDetail
5. BenchDetail -> ReviewForm

## Supabase setup (later)
1. Run SQL script in Supabase SQL Editor:
- `supabase/jvbench_schema.sql`
2. Configure Gradle properties (local, do not commit secrets):
- `SUPABASE_URL=https://<your-project>.supabase.co`
- `SUPABASE_ANON_KEY=<your-anon-key>`
3. BuildConfig fields are automatically injected from Gradle properties:
- `BuildConfig.SUPABASE_URL`
- `BuildConfig.SUPABASE_ANON_KEY`
4. Repositories now call Supabase REST/Auth endpoints:
- `SupabaseAuthRepository`
- `SupabaseBenchRepository`
- `SupabaseReviewRepository`
5. Bucket baseline:
- `bench-images`
- object path convention: `<bench_id>/main.<ext>`

No real secret is hardcoded.

## Team TODO split (4 members suggestion)
1. Auth and session flow
- finalize Supabase auth calls
- improve auth error handling
2. Map and bench markers
- bench marker rendering strategy
- map interactions and filtering
3. Bench creation and detail
- bench validation, image handling strategy, detail enrichment
4. Reviews and ratings
- review creation/listing, aggregate rating updates, moderation rules

## Backend notes (replaceable by Firebase later)
- Domain models + repository interfaces are backend-agnostic.
- Supabase-specific code is isolated in `data/remote/supabase` and `data/repository`.
- UI and ViewModels only depend on domain repository interfaces.
- To switch backend later, keep interfaces and replace data implementations.
