delete from public.player_day_stats where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.game_days       where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.matches         where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.rounds          where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.team_players    where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.players         where grupo_id = '00000000-0000-4000-8000-000000000001';
delete from public.teams           where grupo_id = '00000000-0000-4000-8000-000000000001';

insert into public.teams (grupo_id, nome, cor_hex, sigla, ordem)
select '00000000-0000-4000-8000-000000000001', m.nome, m.cor_hex, m.sigla, m.ordem
from public.modelos_de_time m;

insert into public.players (grupo_id, nome, skill_level, genero, ativo) values
    ('00000000-0000-4000-8000-000000000001', 'Alex',     5, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Bruno',    5, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Caio',     4, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Diego',    4, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Eduardo',  4, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Fabio',    4, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Gabriel',  3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Henrique', 3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Igor',     3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Joao',     3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Kaique',   3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Lucas',    3, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Marcelo',  2, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Nathan',   2, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Otavio',   2, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Pedro',    2, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Rafael',   1, 'masculino', true),
    ('00000000-0000-4000-8000-000000000001', 'Samuel',   1, 'masculino', true),

    ('00000000-0000-4000-8000-000000000001', 'Ana',      5, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Beatriz',  5, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Carla',    4, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Daniela',  4, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Elisa',    4, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Fernanda', 4, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Giovana',  3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Helena',   3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Isabela',  3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Julia',    3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Karina',   3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Larissa',  3, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Mariana',  2, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Natalia',  2, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Olivia',   2, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Paula',    2, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Renata',   1, 'feminino',  true),
    ('00000000-0000-4000-8000-000000000001', 'Sofia',    1, 'feminino',  true),

    ('00000000-0000-4000-8000-000000000001', 'Kleber',   3, 'masculino', false),
    ('00000000-0000-4000-8000-000000000001', 'Tatiana',  2, 'feminino',  false);
