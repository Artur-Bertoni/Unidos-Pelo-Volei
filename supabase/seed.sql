delete from public.player_day_stats;
delete from public.game_days;
delete from public.matches;
delete from public.rounds;
delete from public.team_players;
delete from public.players;
delete from public.teams;

insert into public.teams (nome, cor_hex, sigla, ordem) values
    ('Azul',     '#2F80ED', 'AZ', 1),
    ('Laranja',  '#E8590C', 'LR', 2),
    ('Preto',    '#3F444D', 'PR', 3),
    ('Rosa',     '#E8437F', 'RS', 4),
    ('Roxo',     '#8B5CF6', 'RX', 5),
    ('Verde',    '#16A34A', 'VD', 6),
    ('Cinza',    '#9CA3AF', 'CZ', 7),
    ('Vermelho', '#E23B3B', 'VM', 8),
    ('Amarelo',  '#EAB308', 'AM', 9);

insert into public.players (nome, skill_level, genero, ativo) values
    ('Alex',     5, 'masculino', true), ('Bruno',    5, 'masculino', true),
    ('Caio',     4, 'masculino', true), ('Diego',    4, 'masculino', true),
    ('Eduardo',  4, 'masculino', true), ('Fabio',    4, 'masculino', true),
    ('Gabriel',  3, 'masculino', true), ('Henrique', 3, 'masculino', true),
    ('Igor',     3, 'masculino', true), ('Joao',     3, 'masculino', true),
    ('Kaique',   3, 'masculino', true), ('Lucas',    3, 'masculino', true),
    ('Marcelo',  2, 'masculino', true), ('Nathan',   2, 'masculino', true),
    ('Otavio',   2, 'masculino', true), ('Pedro',    2, 'masculino', true),
    ('Rafael',   1, 'masculino', true), ('Samuel',   1, 'masculino', true),

    ('Ana',      5, 'feminino',  true), ('Beatriz',  5, 'feminino',  true),
    ('Carla',    4, 'feminino',  true), ('Daniela',  4, 'feminino',  true),
    ('Elisa',    4, 'feminino',  true), ('Fernanda', 4, 'feminino',  true),
    ('Giovana',  3, 'feminino',  true), ('Helena',   3, 'feminino',  true),
    ('Isabela',  3, 'feminino',  true), ('Julia',    3, 'feminino',  true),
    ('Karina',   3, 'feminino',  true), ('Larissa',  3, 'feminino',  true),
    ('Mariana',  2, 'feminino',  true), ('Natalia',  2, 'feminino',  true),
    ('Olivia',   2, 'feminino',  true), ('Paula',    2, 'feminino',  true),
    ('Renata',   1, 'feminino',  true), ('Sofia',    1, 'feminino',  true),

    ('Kleber',   3, 'masculino', false), ('Tatiana',  2, 'feminino',  false);
