alter table auth.users add column if not exists google_sub text;

create unique index if not exists users_google_sub_idx on auth.users (google_sub) where google_sub is not null;
create index if not exists users_email_idx on auth.users (lower(email));

create table if not exists auth.sessoes (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid        not null references auth.users (id) on delete cascade,
    token_hash  text        not null unique,
    agente      text,
    criada_em   timestamptz not null default now(),
    usada_em    timestamptz not null default now(),
    expira_em   timestamptz not null,
    revogada_em timestamptz
);

create index if not exists sessoes_user_idx on auth.sessoes (user_id);

revoke all on auth.users from anon, authenticated;
revoke all on auth.sessoes from anon, authenticated;
