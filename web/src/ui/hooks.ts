import { useQuery, useStatus } from '@powersync/react';
import type { Session } from '@supabase/supabase-js';
import { useEffect, useMemo, useState } from 'react';
import type { Row } from '../data/mappers';
import {
  booleano,
  inteiro,
  texto,
  textoOuNulo,
  toCobranca,
  toConfigFinanceiro,
  toConfigGrupo,
  toDica,
  toEvento,
  toEvolucao,
  toMatchCard,
  toPagamento,
  toPagina,
  toPlayer,
  toPlayerContato,
  toPost,
  toPresenca,
  toStanding,
  toTeam,
  toUserProfile,
  toVinculoPedido,
} from '../data/mappers';
import {
  ACTIVE_PLAYERS_SQL,
  ALL_TEAMS_SQL,
  AVALIACOES_PENDENTES_SQL,
  COBRANCAS_SQL,
  CONFIG_FINANCEIRO_SQL,
  CONFIG_GRUPO_SQL,
  DICAS_SQL,
  EVENTOS_SQL,
  EVOLUCAO_SQL,
  FORMATO_SQL,
  MEUS_PAGAMENTOS_SQL,
  PAGINAS_SQL,
  POSTS_SQL,
  PRESENCAS_SQL,
  MATCHES_SQL,
  MATCH_SQL,
  MEU_CONTATO_SQL,
  MEU_JOGADOR_SQL,
  MEU_PEDIDO_SQL,
  PEDIDOS_PENDENTES_SQL,
  PERFORMANCE_SQL,
  PLAYERS_SQL,
  PROFILE_SQL,
  ROSTERS_SQL,
  ROSTER_SQL,
  ROUNDS_SQL,
  STANDINGS_SQL,
  TEAMS_SQL,
  TEAM_HISTORY_SQL,
} from '../data/queries';
import {
  generoDe,
  type AvaliacaoPendente,
  type Cobranca,
  type ConfigFinanceiro,
  type ConfigGrupo,
  type Dica,
  type Evento,
  type Evolucao,
  type ItemDoExtrato,
  type MatchCard,
  type Pagina,
  type Player,
  type PlayerContato,
  type PlayerPerformance,
  type Post,
  type Presenca,
  type RoundSchedule,
  type Standing,
  type Team,
  type TeamRoster,
  type UserProfile,
  type VinculoPedido,
} from '../domain/models';
import { supabase } from '../lib/supabase';
import type { SinalSync } from './components/Componentes';

export interface EstadoDaSessao {
  carregando: boolean;
  session: Session | null;
}

export function useSessao(): EstadoDaSessao {
  const [estado, setEstado] = useState<EstadoDaSessao>({ carregando: true, session: null });

  useEffect(() => {
    let vivo = true;
    void supabase.auth.getSession().then(({ data: { session } }) => {
      if (vivo) setEstado({ carregando: false, session });
    });
    const { data } = supabase.auth.onAuthStateChange((_evento, session) => {
      if (vivo) setEstado({ carregando: false, session });
    });
    return () => {
      vivo = false;
      data.subscription.unsubscribe();
    };
  }, []);

  return estado;
}

export function usePerfil(usuarioId: string | undefined): UserProfile | null {
  const { data } = useQuery<Row>(PROFILE_SQL, [usuarioId ?? '']);
  return data.length > 0 ? toUserProfile(data[0]) : null;
}

export function useMeuJogador(profileId: string | undefined): Player | null {
  const { data } = useQuery<Row>(MEU_JOGADOR_SQL, [profileId ?? '']);
  return data.length > 0 ? toPlayer(data[0]) : null;
}

export function useMeuPedido(profileId: string | undefined): VinculoPedido | null {
  const { data } = useQuery<Row>(MEU_PEDIDO_SQL, [profileId ?? '']);
  return data.length > 0 ? toVinculoPedido(data[0]) : null;
}

export function usePedidosPendentes(): VinculoPedido[] {
  const { data } = useQuery<Row>(PEDIDOS_PENDENTES_SQL);
  return useMemo(() => data.map(toVinculoPedido), [data]);
}

export function useMeuContato(playerId: string | undefined): PlayerContato | null {
  const { data } = useQuery<Row>(MEU_CONTATO_SQL, [playerId ?? '']);
  return data.length > 0 ? toPlayerContato(data[0]) : null;
}

export function useConfigGrupo(): ConfigGrupo | null {
  const { data } = useQuery<Row>(CONFIG_GRUPO_SQL);
  return data.length > 0 ? toConfigGrupo(data[0]) : null;
}

export function usePresencas(data: string): Presenca[] {
  const { data: linhas } = useQuery<Row>(PRESENCAS_SQL, [data]);
  return useMemo(() => linhas.map(toPresenca), [linhas]);
}

export function usePosts(profileId: string | undefined): Post[] {
  const { data } = useQuery<Row>(POSTS_SQL, [profileId ?? '']);
  return useMemo(() => data.map(toPost), [data]);
}

export function useEventos(): Evento[] {
  const { data } = useQuery<Row>(EVENTOS_SQL);
  return useMemo(() => data.map(toEvento), [data]);
}

export function usePaginas(): Pagina[] {
  const { data } = useQuery<Row>(PAGINAS_SQL);
  return useMemo(() => data.map(toPagina), [data]);
}

export function useConfigFinanceiro(): ConfigFinanceiro | null {
  const { data } = useQuery<Row>(CONFIG_FINANCEIRO_SQL);
  return data.length > 0 ? toConfigFinanceiro(data[0]) : null;
}

export function useCobrancas(): Cobranca[] {
  const { data } = useQuery<Row>(COBRANCAS_SQL);
  return useMemo(() => data.map(toCobranca), [data]);
}

export function useMeuExtrato(): ItemDoExtrato[] {
  const { data } = useQuery<Row>(MEUS_PAGAMENTOS_SQL);
  const cobrancas = useCobrancas();
  return useMemo(() => {
    const porId = new Map(cobrancas.map((cobranca) => [cobranca.id, cobranca]));
    return data.map((linha) => {
      const pagamento = toPagamento(linha);
      return { pagamento, cobranca: porId.get(pagamento.cobrancaId) };
    });
  }, [data, cobrancas]);
}

export function useEvolucao(): Evolucao | null {
  const { data } = useQuery<Row>(EVOLUCAO_SQL);
  return data.length > 0 ? toEvolucao(data[0]) : null;
}

export function useDicas(): Dica[] {
  const { data } = useQuery<Row>(DICAS_SQL);
  return useMemo(() => data.map(toDica), [data]);
}

export function useAvaliacoesPendentes(playerId: string | undefined): AvaliacaoPendente[] {
  const { data } = useQuery<Row>(AVALIACOES_PENDENTES_SQL, [playerId ?? '', playerId ?? '']);
  return useMemo(
    () =>
      data.map((linha) => ({
        dayId: texto(linha, 'day_id'),
        avaliadoPlayerId: texto(linha, 'avaliado_player_id'),
        avaliadoNome: texto(linha, 'avaliado_nome'),
      })),
    [data],
  );
}

export function useSinal(): SinalSync {
  const status = useStatus();
  if (status.connected) return 'online';
  if (status.connecting) return 'conectando';
  return 'offline';
}

export interface Formato {
  times: number;
  quadras: number;
}

export function useFormato(): Formato {
  const { data } = useQuery<Row>(FORMATO_SQL);
  if (data.length === 0) return { times: 0, quadras: 0 };
  return { times: inteiro(data[0], 'times'), quadras: inteiro(data[0], 'quadras') };
}

export function useJogadores(): { jogadores: Player[]; carregando: boolean } {
  const { data, isLoading } = useQuery<Row>(PLAYERS_SQL);
  const jogadores = useMemo(() => data.map(toPlayer), [data]);
  return { jogadores, carregando: isLoading };
}

export function useJogadoresAtivos(): Player[] {
  const { data } = useQuery<Row>(ACTIVE_PLAYERS_SQL);
  return useMemo(() => data.map(toPlayer), [data]);
}

export function useDesempenho(): Map<string, PlayerPerformance> {
  const { data } = useQuery<Row>(PERFORMANCE_SQL);
  return useMemo(
    () =>
      new Map(
        data.map((linha) => [
          texto(linha, 'player_id'),
          {
            playerId: texto(linha, 'player_id'),
            dias: inteiro(linha, 'dias'),
            jogos: inteiro(linha, 'jogos'),
            vitorias: inteiro(linha, 'vitorias'),
            derrotas: inteiro(linha, 'derrotas'),
            pontosPro: inteiro(linha, 'pontos_pro'),
            pontosContra: inteiro(linha, 'pontos_contra'),
          },
        ]),
      ),
    [data],
  );
}

export function useTodosOsTimes(): { times: Team[]; carregando: boolean } {
  const { data, isLoading } = useQuery<Row>(ALL_TEAMS_SQL);
  const times = useMemo(() => data.map((linha) => toTeam(linha)), [data]);
  return { times, carregando: isLoading };
}

export function useTimesAtivos(): Team[] {
  const { data } = useQuery<Row>(TEAMS_SQL);
  return useMemo(() => data.map((linha) => toTeam(linha)), [data]);
}

export function useElencos(): TeamRoster[] {
  const { data } = useQuery<Row>(ROSTERS_SQL);
  return useMemo(() => {
    const porTime = new Map<string, TeamRoster>();
    data.forEach((linha) => {
      const teamId = texto(linha, 'team_id');
      const roster = porTime.get(teamId) ?? {
        team: {
          id: teamId,
          nome: texto(linha, 'team_nome'),
          corHex: textoOuNulo(linha, 'team_cor_hex') ?? '#6B7280',
          sigla: texto(linha, 'team_sigla'),
          ativo: booleano(linha, 'team_ativo', true),
          ordem: inteiro(linha, 'team_ordem'),
        },
        players: [],
      };
      const playerId = textoOuNulo(linha, 'player_id');
      if (playerId) {
        roster.players.push({
          id: playerId,
          nome: texto(linha, 'player_nome'),
          skillLevel: inteiro(linha, 'player_skill_level', 3),
          genero: generoDe(textoOuNulo(linha, 'player_genero')),
          ativo: booleano(linha, 'player_ativo', true),
          profileId: null,
          fotoUrl: null,
          nascimentoDia: null,
          nascimentoMes: null,
          entrouEm: null,
          regime: 'mensalista',
        });
      }
      porTime.set(teamId, roster);
    });
    return [...porTime.values()];
  }, [data]);
}

export function useElencoDoTime(teamId: string): Player[] {
  const { data } = useQuery<Row>(ROSTER_SQL, [teamId]);
  return useMemo(() => data.map(toPlayer), [data]);
}

export function useRodadas(): { rodadas: RoundSchedule[]; carregando: boolean } {
  const { data: linhasDeRodadas, isLoading: carregandoRodadas } = useQuery<Row>(ROUNDS_SQL);
  const { data: linhasDePartidas, isLoading: carregandoPartidas } = useQuery<Row>(MATCHES_SQL);
  const elencos = useElencos();

  const rodadas = useMemo(() => {
    const times = elencos.map((elenco) => elenco.team);
    const partidas = linhasDePartidas.map(toMatchCard);
    const porRodada = new Map<number, MatchCard[]>();
    partidas.forEach((partida) => {
      const lista = porRodada.get(partida.roundNumero) ?? [];
      lista.push(partida);
      porRodada.set(partida.roundNumero, lista);
    });

    return linhasDeRodadas.map((linha): RoundSchedule => {
      const numero = inteiro(linha, 'numero');
      const daRodada = porRodada.get(numero) ?? [];
      const jogando = new Set(daRodada.flatMap((p) => [p.teamA.id, p.teamB.id]));
      return {
        round: { id: texto(linha, 'id'), numero, fase: inteiro(linha, 'fase', 1) },
        matches: daRodada,
        folgam: times.filter((time) => !jogando.has(time.id)),
      };
    });
  }, [linhasDeRodadas, linhasDePartidas, elencos]);

  return { rodadas, carregando: carregandoRodadas || carregandoPartidas };
}

export function useClassificacao(): { linhas: Standing[]; carregando: boolean } {
  const { data, isLoading } = useQuery<Row>(STANDINGS_SQL);
  const linhas = useMemo(() => data.map(toStanding), [data]);
  return { linhas, carregando: isLoading };
}

export function usePartida(matchId: string): MatchCard | null {
  const { data } = useQuery<Row>(MATCH_SQL, [matchId]);
  return useMemo(() => (data.length > 0 ? toMatchCard(data[0]) : null), [data]);
}

export function useHistoricoDoTime(teamId: string): MatchCard[] {
  const { data } = useQuery<Row>(TEAM_HISTORY_SQL, [teamId, teamId]);
  return useMemo(() => data.map(toMatchCard), [data]);
}

/** Executa uma ação assíncrona controlando "salvando" e erro, como o executar() dos ViewModels. */
export function useAcao(): {
  salvando: boolean;
  erro: string | null;
  limparErro: () => void;
  executar: (acao: () => Promise<void>) => Promise<void>;
} {
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const executar = async (acao: () => Promise<void>): Promise<void> => {
    setSalvando(true);
    try {
      await acao();
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : 'Não foi possível salvar.');
    } finally {
      setSalvando(false);
    }
  };

  return { salvando, erro, limparErro: () => setErro(null), executar };
}
