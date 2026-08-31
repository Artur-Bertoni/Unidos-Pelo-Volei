-- =============================================================================
-- Row Level Security
-- =============================================================================
-- Regra do MVP:
--   * SELECT liberado para qualquer usuario autenticado;
--   * INSERT / UPDATE / DELETE apenas para quem tem profiles.is_admin = true.
-- =============================================================================

-- Helper: o usuario da requisicao atual e administrador?
-- SECURITY DEFINER para poder ler profiles sem cair na propria RLS de profiles.
create or replace function public.is_admin()
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
select coalesce(
    (select p.is_admin from public.profiles p where p.id = auth.uid()),
    false
);
$$;

revoke execute on function public.is_admin() from public;
grant execute on function public.is_admin() to authenticated;

alter table public.profiles     enable row level security;
alter table public.players      enable row level security;
alter table public.teams        enable row level security;
alter table public.team_players enable row level security;
alter table public.rounds       enable row level security;
alter table public.matches      enable row level security;

-- -----------------------------------------------------------------------------
-- profiles
-- -----------------------------------------------------------------------------
drop policy if exists profiles_select_authenticated on public.profiles;
create policy profiles_select_authenticated on public.profiles
    for select to authenticated
    using (true);

drop policy if exists profiles_insert_admin on public.profiles;
create policy profiles_insert_admin on public.profiles
    for insert to authenticated
    with check (public.is_admin());

drop policy if exists profiles_update_admin on public.profiles;
create policy profiles_update_admin on public.profiles
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists profiles_delete_admin on public.profiles;
create policy profiles_delete_admin on public.profiles
    for delete to authenticated
    using (public.is_admin());

-- -----------------------------------------------------------------------------
-- Demais tabelas: mesmo padrao (leitura livre, escrita so para admin).
-- -----------------------------------------------------------------------------
do $$
declare
    t text;
begin
    foreach t in array array['players', 'teams', 'team_players', 'rounds', 'matches']
    loop
        execute format('drop policy if exists %I on public.%I', t || '_select_authenticated', t);
        execute format(
            'create policy %I on public.%I for select to authenticated using (true)',
            t || '_select_authenticated', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_insert_admin', t);
        execute format(
            'create policy %I on public.%I for insert to authenticated with check (public.is_admin())',
            t || '_insert_admin', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_update_admin', t);
        execute format(
            'create policy %I on public.%I for update to authenticated using (public.is_admin()) with check (public.is_admin())',
            t || '_update_admin', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_delete_admin', t);
        execute format(
            'create policy %I on public.%I for delete to authenticated using (public.is_admin())',
            t || '_delete_admin', t
        );
    end loop;
end;
$$;

-- -----------------------------------------------------------------------------
-- Grants: sem eles a RLS nem chega a ser avaliada.
-- -----------------------------------------------------------------------------
grant usage on schema public to authenticated;
grant select, insert, update, delete on
    public.profiles, public.players, public.teams,
    public.team_players, public.rounds, public.matches
    to authenticated;
grant select on public.standings to authenticated;

-- A view standings herda a RLS das tabelas base (security_invoker).
alter view public.standings set (security_invoker = true);
