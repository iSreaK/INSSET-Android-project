-- jv-Bench - Supabase schema + RLS + storage baseline
-- Keep this script simple and readable for student team maintenance.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    email text not null unique,
    username text not null,
    role text not null default 'USER' check (role in ('USER', 'ADMIN')),
    created_at timestamptz not null default now()
);

-- Migration-safe additions for existing projects.
alter table public.profiles add column if not exists username text;
update public.profiles
set username = split_part(email, '@', 1)
where username is null or btrim(username) = '';
alter table public.profiles alter column username set not null;

create unique index if not exists idx_profiles_username_lower_unique on public.profiles ((lower(username)));

create table if not exists public.benches (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text,
    latitude double precision not null,
    longitude double precision not null,
    image_url text,
    author_id uuid not null references public.profiles (id) on delete cascade,
    average_rating numeric(4,2) not null default 0.00 check (average_rating >= 0 and average_rating <= 10),
    review_count integer not null default 0 check (review_count >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.reviews (
    id uuid primary key default gen_random_uuid(),
    bench_id uuid not null references public.benches (id) on delete cascade,
    user_id uuid not null references public.profiles (id) on delete cascade,
    rating integer not null check (rating >= 0 and rating <= 10),
    comment text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint reviews_unique_user_per_bench unique (bench_id, user_id)
);

create index if not exists idx_benches_author_id on public.benches(author_id);
create index if not exists idx_reviews_bench_id on public.reviews(bench_id);
create index if not exists idx_reviews_user_id on public.reviews(user_id);

-- Simple helper for admin access checks in RLS policies.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and p.role = 'ADMIN'
    );
$$;

-- Keep updated_at consistent without complex trigger logic.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_benches_updated_at on public.benches;
create trigger trg_benches_updated_at
before update on public.benches
for each row
execute function public.set_updated_at();

drop trigger if exists trg_reviews_updated_at on public.reviews;
create trigger trg_reviews_updated_at
before update on public.reviews
for each row
execute function public.set_updated_at();

alter table public.profiles enable row level security;
alter table public.benches enable row level security;
alter table public.reviews enable row level security;

-- PROFILES
drop policy if exists "profiles_select_self_or_admin" on public.profiles;
create policy "profiles_select_self_or_admin"
on public.profiles for select
using (auth.uid() = id or public.is_admin());

drop policy if exists "profiles_insert_self" on public.profiles;
create policy "profiles_insert_self"
on public.profiles for insert
with check (auth.uid() = id);

drop policy if exists "profiles_update_self_or_admin" on public.profiles;
create policy "profiles_update_self_or_admin"
on public.profiles for update
using (auth.uid() = id or public.is_admin())
with check (
    public.is_admin()
    or (
        auth.uid() = id
        and role = (select p.role from public.profiles p where p.id = auth.uid())
    )
);

-- BENCHES
drop policy if exists "benches_read_all" on public.benches;
create policy "benches_read_all"
on public.benches for select
using (true);

drop policy if exists "benches_insert_authenticated_owner" on public.benches;
create policy "benches_insert_authenticated_owner"
on public.benches for insert
with check (auth.uid() is not null and auth.uid() = author_id);

drop policy if exists "benches_update_owner_or_admin" on public.benches;
create policy "benches_update_owner_or_admin"
on public.benches for update
using (author_id = auth.uid() or public.is_admin())
with check (author_id = auth.uid() or public.is_admin());

drop policy if exists "benches_delete_owner_or_admin" on public.benches;
create policy "benches_delete_owner_or_admin"
on public.benches for delete
using (author_id = auth.uid() or public.is_admin());

-- REVIEWS
drop policy if exists "reviews_read_all" on public.reviews;
create policy "reviews_read_all"
on public.reviews for select
using (true);

drop policy if exists "reviews_insert_authenticated_author" on public.reviews;
create policy "reviews_insert_authenticated_author"
on public.reviews for insert
with check (auth.uid() is not null and auth.uid() = user_id);

drop policy if exists "reviews_update_author_or_admin" on public.reviews;
create policy "reviews_update_author_or_admin"
on public.reviews for update
using (user_id = auth.uid() or public.is_admin())
with check (user_id = auth.uid() or public.is_admin());

drop policy if exists "reviews_delete_author_or_admin" on public.reviews;
create policy "reviews_delete_author_or_admin"
on public.reviews for delete
using (user_id = auth.uid() or public.is_admin());

-- STORAGE
insert into storage.buckets (id, name, public)
values ('bench-images', 'bench-images', true)
on conflict (id) do nothing;

drop policy if exists "bench_images_public_read" on storage.objects;
create policy "bench_images_public_read"
on storage.objects for select
using (bucket_id = 'bench-images');

drop policy if exists "bench_images_authenticated_upload" on storage.objects;
create policy "bench_images_authenticated_upload"
on storage.objects for insert
with check (bucket_id = 'bench-images' and auth.uid() is not null);

drop policy if exists "bench_images_owner_or_admin_update" on storage.objects;
create policy "bench_images_owner_or_admin_update"
on storage.objects for update
using (bucket_id = 'bench-images' and (owner = auth.uid() or public.is_admin()));

drop policy if exists "bench_images_owner_or_admin_delete" on storage.objects;
create policy "bench_images_owner_or_admin_delete"
on storage.objects for delete
using (bucket_id = 'bench-images' and (owner = auth.uid() or public.is_admin()));

-- Recommended object path convention (app side):
-- bench-images/<bench_id>/main.<ext>
