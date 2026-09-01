export type Genero = 'masculino' | 'feminino';

export const GENEROS: Genero[] = ['masculino', 'feminino'];

export const rotuloDoGenero = (genero: Genero): string =>
  genero === 'masculino' ? 'Masculino' : 'Feminino';

export const generoDe = (valor: string | null | undefined): Genero =>
  valor === 'feminino' ? 'feminino' : 'masculino';

export type MatchStatus = 'agendado' | 'finalizado';

export const statusDe = (valor: string | null | undefined): MatchStatus =>
  valor === 'finalizado' ? 'finalizado' : 'agendado';

export interface Player {
  id: string;
  nome: string;
  skillLevel: number;
  genero: Genero;
  ativo: boolean;
}

export interface Team {
  id: string;
  nome: string;
  corHex: string;
  sigla: string;
  ativo: boolean;
  ordem: number;
}

export interface Round {
  id: string;
  numero: number;
  fase: number;
}

export interface MatchCard {
  id: string;
  roundNumero: number;
  fase: number;
  quadra: number;
  scoreA: number;
  scoreB: number;
  status: MatchStatus;
  winnerId: string | null;
  teamA: Team;
  teamB: Team;
}

export interface RoundSchedule {
  round: Round;
  matches: MatchCard[];
  folgam: Team[];
}

export interface Standing {
  teamId: string;
  nome: string;
  sigla: string;
  corHex: string;
  jogos: number;
  vitorias: number;
  derrotas: number;
  saldoPontos: number;
  pontosPro: number;
}

export interface TeamRoster {
  team: Team;
  players: Player[];
}

export const forcaTotal = (roster: TeamRoster): number =>
  roster.players.reduce((soma, jogador) => soma + jogador.skillLevel, 0);

export const homensDo = (roster: TeamRoster): number =>
  roster.players.filter((jogador) => jogador.genero === 'masculino').length;

export const mulheresDo = (roster: TeamRoster): number =>
  roster.players.filter((jogador) => jogador.genero === 'feminino').length;

export interface PlayerPerformance {
  playerId: string;
  dias: number;
  jogos: number;
  vitorias: number;
  derrotas: number;
  pontosPro: number;
  pontosContra: number;
}

export const saldoDoDesempenho = (desempenho: PlayerPerformance): number =>
  desempenho.pontosPro - desempenho.pontosContra;

export interface ResumoDoDia {
  partidas: number;
  atletas: number;
  presencas: number;
}

export interface UserProfile {
  id: string;
  email: string | null;
  nome: string | null;
  isAdmin: boolean;
}
