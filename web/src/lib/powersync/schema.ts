import { column, Schema, Table } from '@powersync/web';

const players = new Table({
  nome: column.text,
  skill_level: column.integer,
  genero: column.text,
  ativo: column.integer,
  created_at: column.text,
  updated_at: column.text,
});

const teams = new Table({
  nome: column.text,
  cor_hex: column.text,
  sigla: column.text,
  ativo: column.integer,
  ordem: column.integer,
  created_at: column.text,
  updated_at: column.text,
});

const team_players = new Table(
  {
    team_id: column.text,
    player_id: column.text,
  },
  { indexes: { por_time: ['team_id'], por_jogador: ['player_id'] } },
);

const rounds = new Table({
  numero: column.integer,
  fase: column.integer,
  created_at: column.text,
});

const matches = new Table(
  {
    round_id: column.text,
    quadra: column.integer,
    team_a_id: column.text,
    team_b_id: column.text,
    score_a: column.integer,
    score_b: column.integer,
    status: column.text,
    winner_id: column.text,
    created_at: column.text,
    updated_at: column.text,
  },
  { indexes: { por_rodada: ['round_id'] } },
);

const profiles = new Table({
  email: column.text,
  nome: column.text,
  is_admin: column.integer,
  created_at: column.text,
  updated_at: column.text,
});

const game_days = new Table({
  encerrado_em: column.text,
  partidas: column.integer,
  created_at: column.text,
});

const player_day_stats = new Table(
  {
    day_id: column.text,
    player_id: column.text,
    team_id: column.text,
    team_nome: column.text,
    team_cor_hex: column.text,
    jogos: column.integer,
    vitorias: column.integer,
    derrotas: column.integer,
    pontos_pro: column.integer,
    pontos_contra: column.integer,
    created_at: column.text,
  },
  { indexes: { por_dia: ['day_id'], por_atleta: ['player_id'] } },
);

export const AppSchema = new Schema({
  players,
  teams,
  team_players,
  rounds,
  matches,
  profiles,
  game_days,
  player_day_stats,
});
