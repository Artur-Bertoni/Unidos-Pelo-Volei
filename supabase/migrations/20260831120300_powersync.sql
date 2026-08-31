-- =============================================================================
-- Replicacao logica para o PowerSync
-- =============================================================================
-- O PowerSync le o WAL do Postgres por meio de uma publication chamada
-- `powersync`. Sem ela a instancia conecta mas nao replica nada.
--
-- No Supabase o `wal_level` ja vem como `logical`, entao basta a publication.
-- =============================================================================

drop publication if exists powersync;

create publication powersync for table
    public.profiles,
    public.players,
    public.teams,
    public.team_players,
    public.rounds,
    public.matches;

-- REPLICA IDENTITY:
-- As sync rules deste projeto filtram apenas por chave primaria (profiles.id) ou
-- nao filtram (bucket global), entao a replica identity padrao (a PK) basta.
-- Se um dia uma sync rule passar a filtrar por outra coluna, o DELETE precisa
-- carregar a linha inteira no WAL. Nesse caso, habilite:
--
--   alter table public.matches replica identity full;

-- -----------------------------------------------------------------------------
-- Usuario dedicado do PowerSync (opcional, porem recomendado)
-- -----------------------------------------------------------------------------
-- O jeito mais rapido de conectar a instancia PowerSync e usar o proprio usuario
-- `postgres` do Supabase. Para um projeto de producao, prefira um usuario so
-- para replicacao. Rode o bloco abaixo trocando a senha e guarde-a no cofre da
-- instancia PowerSync (nunca no repositorio):
--
--   create role powersync_role with replication login password 'TROQUE_ESTA_SENHA';
--   grant usage on schema public to powersync_role;
--   grant select on all tables in schema public to powersync_role;
--   alter default privileges in schema public grant select on tables to powersync_role;
--
-- O papel de replicacao le direto do WAL, entao ele nao passa pela RLS. Quem
-- limita o que cada dispositivo recebe sao as sync rules (powersync/sync-rules.yaml).
