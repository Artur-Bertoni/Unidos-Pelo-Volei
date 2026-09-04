import { column, Schema, Table } from '@powersync/web';

const players = new Table(
  {
    nome: column.text,
    skill_level: column.integer,
    genero: column.text,
    ativo: column.integer,
    profile_id: column.text,
    foto_url: column.text,
    nascimento_dia: column.integer,
    nascimento_mes: column.integer,
    entrou_em: column.text,
    regime: column.text,
    created_at: column.text,
    updated_at: column.text,
  },
  { indexes: { por_perfil: ['profile_id'] } },
);

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
  papel: column.text,
  is_admin: column.integer,
  created_at: column.text,
  updated_at: column.text,
});

const vinculo_pedidos = new Table(
  {
    profile_id: column.text,
    player_id: column.text,
    profile_nome: column.text,
    status: column.text,
    criado_em: column.text,
    decidido_por: column.text,
    decidido_em: column.text,
  },
  { indexes: { por_perfil: ['profile_id'], por_situacao: ['status'] } },
);

const player_contatos = new Table(
  {
    player_id: column.text,
    profile_id: column.text,
    telefone: column.text,
    contato_emergencia: column.text,
    nascimento_ano: column.integer,
    atualizado_em: column.text,
  },
  { indexes: { por_atleta: ['player_id'] } },
);

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

const config_grupo = new Table({
  jogo_hora: column.text,
  jogo_local: column.text,
  atualizado_em: column.text,
});

const presencas = new Table(
  {
    player_id: column.text,
    profile_id: column.text,
    data: column.text,
    status: column.text,
    origem: column.text,
    registrado_por: column.text,
    atualizado_em: column.text,
  },
  { indexes: { por_data: ['data'], por_presente: ['player_id'] } },
);

const dispositivos = new Table({
  profile_id: column.text,
  token: column.text,
  plataforma: column.text,
  ativo: column.integer,
  visto_em: column.text,
  criado_em: column.text,
});

const avisos = new Table({
  tipo: column.text,
  titulo: column.text,
  corpo: column.text,
  referencia: column.text,
  criado_em: column.text,
});

const posts = new Table({
  autor_profile_id: column.text,
  autor_nome: column.text,
  titulo: column.text,
  corpo: column.text,
  imagem_url: column.text,
  emoji: column.text,
  fixado: column.integer,
  publicado_em: column.text,
  atualizado_em: column.text,
});

const post_reacoes = new Table(
  {
    post_id: column.text,
    profile_id: column.text,
    emoji: column.text,
    criado_em: column.text,
  },
  { indexes: { por_post: ['post_id'] } },
);

const eventos = new Table(
  {
    titulo: column.text,
    descricao: column.text,
    tipo: column.text,
    inicio: column.text,
    fim: column.text,
    local: column.text,
    criado_por: column.text,
    criado_em: column.text,
  },
  { indexes: { por_inicio: ['inicio'] } },
);

const paginas = new Table({
  slug: column.text,
  categoria: column.text,
  titulo: column.text,
  corpo: column.text,
  ordem: column.integer,
  atualizado_por: column.text,
  atualizado_em: column.text,
});

const config_financeiro = new Table({
  pix_chave: column.text,
  pix_nome: column.text,
  pix_cidade: column.text,
  mensalidade_centavos: column.integer,
  diaria_centavos: column.integer,
  atualizado_em: column.text,
});

const cobrancas = new Table({
  titulo: column.text,
  tipo: column.text,
  valor_centavos: column.integer,
  competencia: column.text,
  vence_em: column.text,
  criado_por: column.text,
  criado_em: column.text,
});

const pagamentos = new Table(
  {
    cobranca_id: column.text,
    player_id: column.text,
    profile_id: column.text,
    valor_centavos: column.integer,
    status: column.text,
    pago_em: column.text,
    registrado_por: column.text,
    observacao: column.text,
    criado_em: column.text,
  },
  { indexes: { por_cobranca: ['cobranca_id'] } },
);

const avaliacoes = new Table(
  {
    day_id: column.text,
    avaliador_player_id: column.text,
    avaliado_player_id: column.text,
    saque: column.integer,
    passe: column.integer,
    ataque: column.integer,
    bloqueio: column.integer,
    defesa: column.integer,
    atitude: column.integer,
    criado_em: column.text,
  },
  { insertOnly: true },
);

const avaliacao_registros = new Table(
  {
    day_id: column.text,
    avaliador_player_id: column.text,
    avaliado_player_id: column.text,
    profile_id: column.text,
    criado_em: column.text,
  },
  { indexes: { por_dia_avaliado: ['day_id'] } },
);

const player_evolucao = new Table({
  player_id: column.text,
  profile_id: column.text,
  total_avaliacoes: column.integer,
  saque_media: column.real,
  passe_media: column.real,
  ataque_media: column.real,
  bloqueio_media: column.real,
  defesa_media: column.real,
  atitude_media: column.real,
  atualizado_em: column.text,
});

const dicas = new Table({
  atributo: column.text,
  faixa_max: column.real,
  titulo: column.text,
  texto: column.text,
  ordem: column.integer,
});

export const AppSchema = new Schema({
  players,
  teams,
  team_players,
  rounds,
  matches,
  profiles,
  game_days,
  player_day_stats,
  vinculo_pedidos,
  player_contatos,
  config_grupo,
  presencas,
  dispositivos,
  avisos,
  posts,
  post_reacoes,
  eventos,
  paginas,
  config_financeiro,
  cobrancas,
  pagamentos,
  avaliacoes,
  avaliacao_registros,
  player_evolucao,
  dicas,
});
