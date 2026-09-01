import {
  generoDe,
  statusDe,
  type MatchCard,
  type Player,
  type Standing,
  type Team,
  type UserProfile,
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

export const toPlayer = (row: Row): Player => ({
  id: texto(row, 'id'),
  nome: texto(row, 'nome'),
  skillLevel: inteiro(row, 'skill_level', 3),
  genero: generoDe(textoOuNulo(row, 'genero')),
  ativo: booleano(row, 'ativo', true),
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

export const toUserProfile = (row: Row): UserProfile => ({
  id: texto(row, 'id'),
  email: textoOuNulo(row, 'email'),
  nome: textoOuNulo(row, 'nome'),
  isAdmin: booleano(row, 'is_admin'),
});
