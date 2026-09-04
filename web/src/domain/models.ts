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
  profileId: string | null;
  fotoUrl: string | null;
  nascimentoDia: number | null;
  nascimentoMes: number | null;
  entrouEm: string | null;
  regime: string;
}

export const vinculado = (player: Player): boolean => player.profileId !== null;

export const aniversarioDe = (player: Player): string | null => {
  const { nascimentoDia: dia, nascimentoMes: mes } = player;
  if (dia === null || mes === null) return null;
  return `${String(dia).padStart(2, '0')}/${String(mes).padStart(2, '0')}`;
};

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

export type Papel = 'diretoria' | 'atleta';

export const papelDe = (valor: string | null | undefined): Papel =>
  valor === 'diretoria' ? 'diretoria' : 'atleta';

export const rotuloDoPapel = (papel: Papel): string =>
  papel === 'diretoria' ? 'Diretoria' : 'Atleta';

export interface UserProfile {
  id: string;
  email: string | null;
  nome: string | null;
  papel: Papel;
  isAdmin: boolean;
}

export type StatusVinculo = 'pendente' | 'aprovado' | 'recusado';

const STATUS_DE_VINCULO: StatusVinculo[] = ['pendente', 'aprovado', 'recusado'];

export const statusVinculoDe = (valor: string | null | undefined): StatusVinculo =>
  STATUS_DE_VINCULO.find((status) => status === valor) ?? 'pendente';

export interface VinculoPedido {
  id: string;
  profileId: string;
  playerId: string;
  profileNome: string | null;
  status: StatusVinculo;
  criadoEm: string | null;
}

export interface PlayerContato {
  id: string;
  playerId: string;
  telefone: string | null;
  contatoEmergencia: string | null;
  nascimentoAno: number | null;
}

export type Regime = 'mensalista' | 'diarista' | 'isento';

export const REGIMES: Regime[] = ['mensalista', 'diarista', 'isento'];

export const REGIMES_DO_ATLETA: Regime[] = ['mensalista', 'diarista'];

const ROTULOS_DE_REGIME: Record<Regime, string> = {
  mensalista: 'Mensalista',
  diarista: 'Diarista',
  isento: 'Isento',
};

export const rotuloDoRegime = (regime: Regime): string => ROTULOS_DE_REGIME[regime];

export const regimeDe = (valor: string | null | undefined): Regime =>
  REGIMES.find((regime) => regime === valor) ?? 'mensalista';

export type StatusPresenca = 'vou' | 'talvez' | 'nao_vou';

export const STATUS_DE_PRESENCA: StatusPresenca[] = ['vou', 'talvez', 'nao_vou'];

const ROTULOS_DE_PRESENCA: Record<StatusPresenca, string> = {
  vou: 'Vou',
  talvez: 'Talvez',
  nao_vou: 'Não vou',
};

export const rotuloDaPresenca = (status: StatusPresenca): string => ROTULOS_DE_PRESENCA[status];

export const statusPresencaDe = (valor: string | null | undefined): StatusPresenca | null =>
  STATUS_DE_PRESENCA.find((status) => status === valor) ?? null;

export interface Presenca {
  id: string;
  playerId: string;
  data: string;
  status: StatusPresenca;
  origem: string;
}

export interface ResumoDaChamada {
  vou: number;
  talvez: number;
  naoVou: number;
  semResposta: number;
}

export interface ConfigGrupo {
  id: string;
  jogoHora: string;
  jogoLocal: string | null;
}

export const EMOJI_PADRAO = '👏';

export const EMOJIS_DE_REACAO: string[] = ['👏', '🔥', '❤️', '😂', '👍', '🏐', '🎉', '😮'];

export interface Post {
  id: string;
  autorNome: string | null;
  titulo: string;
  corpo: string;
  fixado: boolean;
  publicadoEm: string | null;
  imagemUrl: string | null;
  emoji: string;
  reacoes: number;
  reagi: boolean;
}

export type TipoEvento = 'jogo' | 'confraternizacao' | 'campeonato';

export const TIPOS_DE_EVENTO: TipoEvento[] = ['jogo', 'confraternizacao', 'campeonato'];

const ROTULOS_DE_EVENTO: Record<TipoEvento, string> = {
  jogo: 'Jogo',
  confraternizacao: 'Confraternização',
  campeonato: 'Campeonato',
};

export const rotuloDoEvento = (tipo: TipoEvento): string => ROTULOS_DE_EVENTO[tipo];

export const tipoEventoDe = (valor: string | null | undefined): TipoEvento =>
  TIPOS_DE_EVENTO.find((tipo) => tipo === valor) ?? 'jogo';

export interface Evento {
  id: string;
  titulo: string;
  descricao: string | null;
  tipo: TipoEvento;
  inicio: string;
  local: string | null;
}

export type TipoMarco = 'aniversario' | 'tempo_de_casa';

export interface Marco {
  playerId: string;
  nome: string;
  tipo: TipoMarco;
  dia: number;
  mes: number;
  anos: number | null;
}

export type CategoriaPagina = 'volei' | 'grupo' | 'campeonato';

const ROTULOS_DE_CATEGORIA: Record<CategoriaPagina, string> = {
  volei: 'Regras do vôlei',
  grupo: 'Regras do grupo',
  campeonato: 'Campeonatos',
};

export const rotuloDaCategoria = (categoria: CategoriaPagina): string =>
  ROTULOS_DE_CATEGORIA[categoria];

export const categoriaDe = (valor: string | null | undefined): CategoriaPagina =>
  valor === 'volei' || valor === 'campeonato' ? valor : 'grupo';

export interface Pagina {
  id: string;
  slug: string;
  categoria: CategoriaPagina;
  titulo: string;
  corpo: string;
  ordem: number;
}

export interface ConfigFinanceiro {
  id: string;
  pixChave: string | null;
  pixNome: string | null;
  pixCidade: string | null;
  mensalidadeCentavos: number;
  diariaCentavos: number;
}

export type TipoCobranca = 'mensalidade' | 'diaria' | 'avulsa';

export const tipoCobrancaDe = (valor: string | null | undefined): TipoCobranca =>
  valor === 'mensalidade' || valor === 'diaria' ? valor : 'avulsa';

export interface Cobranca {
  id: string;
  titulo: string;
  tipo: TipoCobranca;
  valorCentavos: number;
  competencia: string | null;
  venceEm: string | null;
}

export type StatusPagamento = 'pendente' | 'pago' | 'isento';

export const STATUS_DE_PAGAMENTO: StatusPagamento[] = ['pendente', 'pago', 'isento'];

const ROTULOS_DE_PAGAMENTO: Record<StatusPagamento, string> = {
  pendente: 'Em aberto',
  pago: 'Pago',
  isento: 'Isento',
};

export const rotuloDoPagamento = (status: StatusPagamento): string =>
  ROTULOS_DE_PAGAMENTO[status];

export const statusPagamentoDe = (valor: string | null | undefined): StatusPagamento =>
  STATUS_DE_PAGAMENTO.find((status) => status === valor) ?? 'pendente';

export interface Pagamento {
  id: string;
  cobrancaId: string;
  playerId: string;
  valorCentavos: number;
  status: StatusPagamento;
  pagoEm: string | null;
  observacao: string | null;
}

export interface ItemDoExtrato {
  pagamento: Pagamento;
  cobranca: Cobranca | undefined;
}

export type Fundamento = 'saque' | 'passe' | 'ataque' | 'bloqueio' | 'defesa' | 'atitude';

export const FUNDAMENTOS: Fundamento[] = [
  'saque',
  'passe',
  'ataque',
  'bloqueio',
  'defesa',
  'atitude',
];

const ROTULOS_DE_FUNDAMENTO: Record<Fundamento, string> = {
  saque: 'Saque',
  passe: 'Passe',
  ataque: 'Ataque',
  bloqueio: 'Bloqueio',
  defesa: 'Defesa',
  atitude: 'Atitude',
};

export const rotuloDoFundamento = (fundamento: Fundamento): string =>
  ROTULOS_DE_FUNDAMENTO[fundamento];

export const fundamentoDe = (valor: string | null | undefined): Fundamento | null =>
  FUNDAMENTOS.find((fundamento) => fundamento === valor) ?? null;

export type NotasDaAvaliacao = Record<Fundamento, number>;

export const notasIniciais = (): NotasDaAvaliacao => ({
  saque: 3,
  passe: 3,
  ataque: 3,
  bloqueio: 3,
  defesa: 3,
  atitude: 3,
});

export interface AvaliacaoPendente {
  dayId: string;
  avaliadoPlayerId: string;
  avaliadoNome: string;
}

export interface Evolucao {
  playerId: string;
  totalAvaliacoes: number;
  medias: Partial<Record<Fundamento, number>>;
}

export const evolucaoLiberada = (evolucao: Evolucao | null): boolean =>
  evolucao !== null && Object.keys(evolucao.medias).length > 0;

export const fundamentoMaisFraco = (evolucao: Evolucao | null): Fundamento | null => {
  if (!evolucao) return null;
  const entradas = Object.entries(evolucao.medias) as [Fundamento, number][];
  if (entradas.length === 0) return null;
  return entradas.reduce((menor, atual) => (atual[1] < menor[1] ? atual : menor))[0];
};

export interface Dica {
  id: string;
  fundamento: Fundamento;
  faixaMax: number;
  titulo: string;
  texto: string;
}

export const MINIMO_DE_AVALIACOES = 5;
