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

insert into public.players (grupo_id, nome, nota_saque, nota_passe, nota_ataque,
                            nota_bloqueio, nota_defesa, nota_atitude, genero, ativo)
select '00000000-0000-4000-8000-000000000001', n.nome,
       n.nivel, n.nivel, n.nivel, n.nivel, n.nivel, n.nivel, n.genero, n.ativo
from (values
    ('Alex', 5, 'masculino', true),
    ('Bruno', 5, 'masculino', true),
    ('Caio', 4, 'masculino', true),
    ('Diego', 4, 'masculino', true),
    ('Eduardo', 4, 'masculino', true),
    ('Fabio', 4, 'masculino', true),
    ('Gabriel', 3, 'masculino', true),
    ('Henrique', 3, 'masculino', true),
    ('Igor', 3, 'masculino', true),
    ('Joao', 3, 'masculino', true),
    ('Kaique', 3, 'masculino', true),
    ('Lucas', 3, 'masculino', true),
    ('Marcelo', 2, 'masculino', true),
    ('Nathan', 2, 'masculino', true),
    ('Otavio', 2, 'masculino', true),
    ('Pedro', 2, 'masculino', true),
    ('Rafael', 1, 'masculino', true),
    ('Samuel', 1, 'masculino', true),

    ('Ana', 5, 'feminino', true),
    ('Beatriz', 5, 'feminino', true),
    ('Carla', 4, 'feminino', true),
    ('Daniela', 4, 'feminino', true),
    ('Elisa', 4, 'feminino', true),
    ('Fernanda', 4, 'feminino', true),
    ('Giovana', 3, 'feminino', true),
    ('Helena', 3, 'feminino', true),
    ('Isabela', 3, 'feminino', true),
    ('Julia', 3, 'feminino', true),
    ('Karina', 3, 'feminino', true),
    ('Larissa', 3, 'feminino', true),
    ('Mariana', 2, 'feminino', true),
    ('Natalia', 2, 'feminino', true),
    ('Olivia', 2, 'feminino', true),
    ('Paula', 2, 'feminino', true),
    ('Renata', 1, 'feminino', true),
    ('Sofia', 1, 'feminino', true),

    ('Kleber', 3, 'masculino', false),
    ('Tatiana', 2, 'feminino', false)
) as n(nome, nivel, genero, ativo);
