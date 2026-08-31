-- =============================================================================
-- Seed opcional: 9 times e 36 jogadores para testar snake draft e chaveamento
-- =============================================================================
-- Rode no SQL Editor do Supabase (ou automaticamente com `supabase db reset`
-- no caminho local com Docker). Roda como `postgres`, portanto ignora a RLS.
--
-- E idempotente: pode rodar quantas vezes quiser.
-- =============================================================================

insert into public.teams (nome, cor_hex, sigla, ordem) values
    ('Azul',     '#2F80ED', 'AZ', 1),
    ('Laranja',  '#E8590C', 'LR', 2),
    ('Preto',    '#3F444D', 'PR', 3),
    ('Rosa',     '#E8437F', 'RS', 4),
    ('Roxo',     '#8B5CF6', 'RX', 5),
    ('Verde',    '#16A34A', 'VD', 6),
    ('Cinza',    '#9CA3AF', 'CZ', 7),
    ('Vermelho', '#E23B3B', 'VM', 8),
    ('Amarelo',  '#EAB308', 'AM', 9)
on conflict do nothing;

insert into public.players (nome, skill_level, ativo) values
    ('Alex',     5, true), ('Bruno',    5, true), ('Caio',     5, true),
    ('Diego',    5, true), ('Eduardo',  4, true), ('Fabio',    4, true),
    ('Gabriel',  4, true), ('Henrique', 4, true), ('Igor',     4, true),
    ('Joao',     4, true), ('Kaique',   4, true), ('Lucas',    4, true),
    ('Marcelo',  3, true), ('Nathan',   3, true), ('Otavio',   3, true),
    ('Pedro',    3, true), ('Quirino',  3, true), ('Rafael',   3, true),
    ('Samuel',   3, true), ('Thiago',   3, true), ('Ulisses',  3, true),
    ('Vitor',    3, true), ('Wagner',   3, true), ('Xavier',   3, true),
    ('Yuri',     2, true), ('Zeca',     2, true), ('Andre',    2, true),
    ('Bernardo', 2, true), ('Cesar',    2, true), ('Daniel',   2, true),
    ('Elias',    2, true), ('Felipe',   2, true), ('Gustavo',  1, true),
    ('Hugo',     1, true), ('Ivan',     1, true), ('Jonas',    1, true),
    ('Kleber',   3, false), ('Leandro',  2, false)
on conflict do nothing;

-- -----------------------------------------------------------------------------
-- Depois de fazer o primeiro login pelo app, promova o seu usuario a admin:
--
--   update public.profiles set is_admin = true where email = 'voce@gmail.com';
--
-- Sem isso o app abre em modo somente leitura.
-- -----------------------------------------------------------------------------
