import {
  categoriaDe,
  EMOJI_PADRAO,
  fundamentoDe,
  FUNDAMENTOS,
  generoDe,
  papelDe,
  regimeDe,
  statusDe,
  statusPagamentoDe,
  statusPresencaDe,
  statusVinculoDe,
  tipoCobrancaDe,
  tipoEventoDe,
  type Cobranca,
  type ConfigFinanceiro,
  type ConfigGrupo,
  type Dica,
  type Evento,
  type Evolucao,
  type Fundamento,
  type MatchCard,
  type Pagamento,
  type Pagina,
  type Player,
  type PlayerContato,
  type Post,
  type Presenca,
  type Standing,
  type Team,
  type UserProfile,
  type VinculoPedido,
} from '../domain/models';

export type Row = Record<string, unknown>;

export const novoId = (): string =>
  typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

export const agoraIso = (): string => new Date().toISOString();

export const texto = (row: Row, nome: string): string => {
  const valor = row[nome];
  return typeof valor === 'string' ? valor : '';
};

export const textoOuNulo = (row: Row, nome: string): string | null => {
  const valor = row[nome];
  return typeof valor === 'string' && valor.length > 0 ? valor : null;
};

export const inteiro = (row: Row, nome: string, padrao = 0): number => {
  const valor = row[nome];
  return typeof valor === 'number' ? valor : padrao;
};

export const booleano = (row: Row, nome: string, padrao = false): boolean => {
  const valor = row[nome];
  if (typeof valor === 'number') return valor !== 0;
  if (typeof valor === 'boolean') return valor;
  return padrao;
};

export const inteiroOuNulo = (row: Row, nome: string): number | null => {
  const valor = row[nome];
  return typeof valor === 'number' ? valor : null;
};

export const toPlayer = (row: Row): Player => ({
  id: texto(row, 'id'),
  nome: texto(row, 'nome'),
  skillLevel: inteiro(row, 'skill_level', 3),
  genero: generoDe(textoOuNulo(row, 'genero')),
  ativo: booleano(row, 'ativo', true),
  profileId: textoOuNulo(row, 'profile_id'),
  fotoUrl: textoOuNulo(row, 'foto_url'),
  nascimentoDia: inteiroOuNulo(row, 'nascimento_dia'),
  nascimentoMes: inteiroOuNulo(row, 'nascimento_mes'),
  entrouEm: textoOuNulo(row, 'entrou_em'),
  regime: regimeDe(textoOuNulo(row, 'regime')),
});

export const toPresenca = (row: Row): Presenca => ({
  id: texto(row, 'id'),
  playerId: texto(row, 'player_id'),
  data: texto(row, 'data'),
  status: statusPresencaDe(textoOuNulo(row, 'status')) ?? 'vou',
  origem: textoOuNulo(row, 'origem') ?? 'atleta',
});

export const toConfigGrupo = (row: Row): ConfigGrupo => ({
  id: texto(row, 'id'),
  jogoHora: textoOuNulo(row, 'jogo_hora') ?? '09:00',
  jogoLocal: textoOuNulo(row, 'jogo_local'),
});

export const toPost = (row: Row): Post => ({
  id: texto(row, 'id'),
  autorNome: textoOuNulo(row, 'autor_nome'),
  titulo: texto(row, 'titulo'),
  corpo: texto(row, 'corpo'),
  fixado: booleano(row, 'fixado'),
  publicadoEm: textoOuNulo(row, 'publicado_em'),
  imagemUrl: textoOuNulo(row, 'imagem_url'),
  emoji: textoOuNulo(row, 'emoji')?.trim() || EMOJI_PADRAO,
  reacoes: inteiro(row, 'reacoes'),
  reagi: booleano(row, 'reagi'),
});

export const toEvento = (row: Row): Evento => ({
  id: texto(row, 'id'),
  titulo: texto(row, 'titulo'),
  descricao: textoOuNulo(row, 'descricao'),
  tipo: tipoEventoDe(textoOuNulo(row, 'tipo')),
  inicio: texto(row, 'inicio'),
  local: textoOuNulo(row, 'local'),
});

export const toPagina = (row: Row): Pagina => ({
  id: texto(row, 'id'),
  slug: texto(row, 'slug'),
  categoria: categoriaDe(textoOuNulo(row, 'categoria')),
  titulo: texto(row, 'titulo'),
  corpo: texto(row, 'corpo'),
  ordem: inteiro(row, 'ordem'),
});

export const toConfigFinanceiro = (row: Row): ConfigFinanceiro => ({
  id: texto(row, 'id'),
  pixChave: textoOuNulo(row, 'pix_chave'),
  pixNome: textoOuNulo(row, 'pix_nome'),
  pixCidade: textoOuNulo(row, 'pix_cidade'),
  mensalidadeCentavos: inteiro(row, 'mensalidade_centavos'),
  diariaCentavos: inteiro(row, 'diaria_centavos'),
});

export const toCobranca = (row: Row): Cobranca => ({
  id: texto(row, 'id'),
  titulo: texto(row, 'titulo'),
  tipo: tipoCobrancaDe(textoOuNulo(row, 'tipo')),
  valorCentavos: inteiro(row, 'valor_centavos'),
  competencia: textoOuNulo(row, 'competencia'),
  venceEm: textoOuNulo(row, 'vence_em'),
});

export const toPagamento = (row: Row): Pagamento => ({
  id: texto(row, 'id'),
  cobrancaId: texto(row, 'cobranca_id'),
  playerId: texto(row, 'player_id'),
  valorCentavos: inteiro(row, 'valor_centavos'),
  status: statusPagamentoDe(textoOuNulo(row, 'status')),
  pagoEm: textoOuNulo(row, 'pago_em'),
  observacao: textoOuNulo(row, 'observacao'),
});

export const toEvolucao = (row: Row): Evolucao => {
  const medias: Partial<Record<Fundamento, number>> = {};
  FUNDAMENTOS.forEach((fundamento) => {
    const valor = inteiroOuNulo(row, `${fundamento}_media`);
    if (valor !== null) medias[fundamento] = valor;
  });
  return {
    playerId: texto(row, 'player_id'),
    totalAvaliacoes: inteiro(row, 'total_avaliacoes'),
    medias,
  };
};

export const toDica = (row: Row): Dica => ({
  id: texto(row, 'id'),
  fundamento: fundamentoDe(textoOuNulo(row, 'atributo')) ?? 'atitude',
  faixaMax: inteiroOuNulo(row, 'faixa_max') ?? 5,
  titulo: texto(row, 'titulo'),
  texto: texto(row, 'texto'),
});

export const toTeam = (row: Row, prefixo = ''): Team => ({
  id: texto(row, `${prefixo}id`),
  nome: texto(row, `${prefixo}nome`),
  corHex: textoOuNulo(row, `${prefixo}cor_hex`) ?? '#6B7280',
  sigla: texto(row, `${prefixo}sigla`),
  ativo: booleano(row, `${prefixo}ativo`, true),
  ordem: inteiro(row, `${prefixo}ordem`),
});

export const toMatchCard = (row: Row): MatchCard => ({
  id: texto(row, 'id'),
  roundNumero: inteiro(row, 'numero'),
  fase: inteiro(row, 'fase', 1),
  quadra: inteiro(row, 'quadra', 1),
  scoreA: inteiro(row, 'score_a'),
  scoreB: inteiro(row, 'score_b'),
  status: statusDe(textoOuNulo(row, 'status')),
  winnerId: textoOuNulo(row, 'winner_id'),
  teamA: toTeam(row, 'a_'),
  teamB: toTeam(row, 'b_'),
});

export const toStanding = (row: Row): Standing => ({
  teamId: texto(row, 'team_id'),
  nome: texto(row, 'nome'),
  sigla: texto(row, 'sigla'),
  corHex: textoOuNulo(row, 'cor_hex') ?? '#6B7280',
  jogos: inteiro(row, 'jogos'),
  vitorias: inteiro(row, 'vitorias'),
  derrotas: inteiro(row, 'derrotas'),
  saldoPontos: inteiro(row, 'saldo_pontos'),
  pontosPro: inteiro(row, 'pontos_pro'),
});

export const toUserProfile = (row: Row): UserProfile => {
  const gravado = textoOuNulo(row, 'papel');
  const legado = booleano(row, 'is_admin') ? 'diretoria' : 'atleta';
  const resolvido = papelDe(gravado ?? legado);
  return {
    id: texto(row, 'id'),
    email: textoOuNulo(row, 'email'),
    nome: textoOuNulo(row, 'nome'),
    papel: resolvido,
    isAdmin: resolvido === 'diretoria',
  };
};

export const toVinculoPedido = (row: Row): VinculoPedido => ({
  id: texto(row, 'id'),
  profileId: texto(row, 'profile_id'),
  playerId: texto(row, 'player_id'),
  profileNome: textoOuNulo(row, 'profile_nome'),
  status: statusVinculoDe(textoOuNulo(row, 'status')),
  criadoEm: textoOuNulo(row, 'criado_em'),
});

export const toPlayerContato = (row: Row): PlayerContato => ({
  id: texto(row, 'id'),
  playerId: texto(row, 'player_id'),
  telefone: textoOuNulo(row, 'telefone'),
  contatoEmergencia: textoOuNulo(row, 'contato_emergencia'),
  nascimentoAno: inteiroOuNulo(row, 'nascimento_ano'),
});
