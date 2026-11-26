create extension if not exists pgcrypto;

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon nologin noinherit;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated nologin noinherit;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'service_role') then
        create role service_role nologin noinherit bypassrls;
    end if;
end;
$$;

grant usage on schema public to anon, authenticated, service_role;

create schema if not exists auth;
grant usage on schema auth to anon, authenticated, service_role;

create table if not exists auth.users (
    id                 uuid primary key default gen_random_uuid(),
    email              text,
    raw_user_meta_data jsonb       not null default '{}'::jsonb,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

create or replace function auth.uid()
    returns uuid
    language sql
    stable
as $$
    select coalesce(
        nullif(current_setting('request.jwt.claim.sub', true), ''),
        nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub'
    )::uuid
$$;

create or replace function auth.jwt()
    returns jsonb
    language sql
    stable
as $$
    select coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb
$$;

create schema if not exists storage;
grant usage on schema storage to anon, authenticated, service_role;

create table if not exists storage.buckets (
    id                 text primary key,
    name               text        not null,
    public             boolean     not null default false,
    file_size_limit    bigint,
    allowed_mime_types text[],
    created_at         timestamptz not null default now()
);

create table if not exists storage.objects (
    id           uuid primary key default gen_random_uuid(),
    bucket_id    text        not null references storage.buckets (id),
    name         text        not null,
    owner        uuid,
    content_type text,
    tamanho      bigint,
    conteudo     bytea,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    unique (bucket_id, name)
);

alter table storage.buckets enable row level security;
alter table storage.objects enable row level security;

drop policy if exists buckets_leitura on storage.buckets;
create policy buckets_leitura on storage.buckets
    for select to anon, authenticated
    using (true);

grant select on storage.buckets to anon, authenticated;
grant select on storage.objects to anon;
grant select, insert, update, delete on storage.objects to authenticated;
