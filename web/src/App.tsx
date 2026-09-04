import { useRef, useState } from 'react';
import { encerrarDia, lerElencosPassados } from './data/gameDays';
import {
  cancelarPedido,
  decidirPedido,
  pedirVinculo,
  salvarContato,
  salvarFicha,
  vincularJogador,
} from './data/membro';
import {
  apagarResultados,
  atualizarPlacar,
  finalizarPartida,
  reabrirPartida,
  substituirChaveamento,
} from './data/matches';
import {
  atualizarJogador,
  criarJogador,
  definirPresenca,
  definirPresencaDeTodos,
  excluirJogador,
} from './data/players';
import {
  atualizarTime,
  criarTime,
  definirAtivos,
  definirTimeAtivo,
  excluirTime,
  lerTimesAtivos,
  lerTodosOsTimes,
  substituirElencos,
} from './data/teams';
import type {
  Evento,
  Pagina,
  ResumoDoDia,
  StatusPagamento,
  Team,
  TeamRoster,
} from './domain/models';
import { generateSchedule } from './domain/roundRobin';
import {
  distribute,
  HistoricoDeDuplas,
  JOGADORES_POR_TIME,
  type ElencoPassado,
} from './domain/teamDraft';
import {
  alternarReacao,
  definirStatusDoPagamento,
  enviarAvaliacao,
  excluirEvento,
  excluirPost,
  gerarDiaria,
  gerarMensalidade,
  proximoSabado,
  publicarPost,
  responderChamada,
  salvarConfigFinanceiro,
  salvarEvento,
  salvarPagina,
  trazerConfirmados,
} from './data/grupo';
import { chavesFaltando, configurado, env } from './lib/env';
import { enviarImagemDoMural } from './lib/mural';
import { supabase } from './lib/supabase';
import { entrarComGoogle, sair } from './lib/supabase';
import {
  AppHeader,
  Aviso,
  Carregando,
} from './ui/components/Componentes';
import { IconeGrupos, IconePessoa, IconeTrofeu, IconeVolei } from './ui/components/Icons';
import {
  useAcao,
  useClassificacao,
  useDesempenho,
  useElencoDoTime,
  useElencos,
  useFormato,
  useHistoricoDoTime,
  useJogadores,
  useJogadoresAtivos,
  useMeuContato,
  useMeuJogador,
  useMeuPedido,
  usePartida,
  useAvaliacoesPendentes,
  useCobrancas,
  useConfigFinanceiro,
  useConfigGrupo,
  useDicas,
  useEventos,
  useEvolucao,
  useMeuExtrato,
  usePaginas,
  usePedidosPendentes,
  usePerfil,
  usePosts,
  usePresencas,
  useRodadas,
  useSessao,
  useSinal,
  useTodosOsTimes,
} from './ui/hooks';
import { ClassificacaoScreen } from './ui/screens/Classificacao';
import { AprovacoesScreen, EuScreen, type PedidoNaFila } from './ui/screens/Eu';
import {
  AvaliacaoScreen,
  CartaoDaEvolucao,
  CartaoDoExtrato,
  ConfigFinanceiroDialogo,
  PainelFinanceiroScreen,
  type LinhaDoPainel,
} from './ui/screens/Financeiro';
import {
  EventoDialogo,
  GrupoScreen,
  PaginaScreen,
  PostDialogo,
  marcosDe,
  resumoDaChamada,
  type SecaoDoGrupo,
} from './ui/screens/Grupo';
import { JogadoresScreen, filtrarJogadores, type FiltroPresenca } from './ui/screens/Jogadores';
import { JogosScreen } from './ui/screens/Jogos';
import { ConfiguracaoPendenteScreen, LoginScreen } from './ui/screens/Login';
import { jaViuOTour, marcarTourComoVisto, TourScreen } from './ui/screens/Tour';
import { VinculosScreen, type ContaDoGrupo } from './ui/screens/Vinculos';
import { PlacarScreen } from './ui/screens/Placar';
import { SorteioScreen } from './ui/screens/Sorteio';
import { TeamEditorDialog, TeamHistoryDialog, TimesScreen } from './ui/screens/Times';

const PESO_ELENCO_ATUAL = 1.5;
const PESO_PREVIA_RECENTE = 1.5;
const PREVIAS_LEMBRADAS = 3;

type Aba = 'social' | 'jogos' | 'classificacao' | 'times' | 'eu';
type Destino =
  | { tipo: 'abas' }
  | { tipo: 'jogadores' }
  | { tipo: 'distribuicao' }
  | { tipo: 'aprovacoes' }
  | { tipo: 'vinculos' }
  | { tipo: 'painel-financeiro' }
  | { tipo: 'avaliacao' }
  | { tipo: 'pagina'; pagina: Pagina }
  | { tipo: 'partida'; matchId: string };

const subtituloDoFormato = (times: number, quadras: number): string => {
  if (times === 0) return 'Configure os times para começar';
  if (quadras === 0) return `${times} times - chaveamento pendente`;
  return `Jogos de Sábado - ${times} Times (${quadras} Quadras)`;
};

const mensagemDoDia = (resumo: ResumoDoDia): string => {
  if (resumo.presencas === 0) return 'Ninguém estava marcado como presente: o dia foi apenas limpo.';
  if (resumo.atletas === 0) {
    return `Dia encerrado: ${resumo.presencas} presenças guardadas, sem times montados. A lista de presença já está zerada.`;
  }
  return `Dia encerrado: ${resumo.partidas} ${resumo.partidas === 1 ? 'partida' : 'partidas'} no desempenho de ${resumo.atletas} atletas e ${resumo.presencas} presenças guardadas. A lista de presença já está zerada.`;
};

export default function App() {
  if (!configurado()) {
    return <ConfiguracaoPendenteScreen chavesFaltando={chavesFaltando()} ambiente={env.nome} />;
  }
  return <Raiz />;
}

function Raiz() {
  const { carregando, session } = useSessao();
  const [entrando, setEntrando] = useState(false);
  const [erroLogin, setErroLogin] = useState<string | null>(null);

  if (carregando) return <Carregando />;

  if (!session) {
    return (
      <LoginScreen
        entrando={entrando}
        erro={erroLogin}
        onEntrar={() => {
          setEntrando(true);
          setErroLogin(null);
          entrarComGoogle().catch((falha: unknown) => {
            setErroLogin(falha instanceof Error ? falha.message : 'Não foi possível entrar com o Google.');
            setEntrando(false);
          });
        }}
      />
    );
  }

  return <ComTour usuarioId={session.user.id} />;
}

function ComTour({ usuarioId }: { usuarioId: string }) {
  const [mostrarTour, setMostrarTour] = useState(() => !jaViuOTour());
  const [veioDoTour, setVeioDoTour] = useState(false);

  if (mostrarTour) {
    return (
      <div className="app">
        <TourScreen
          onConcluir={() => {
            marcarTourComoVisto();
            setVeioDoTour(true);
            setMostrarTour(false);
          }}
        />
      </div>
    );
  }

  return <Home usuarioId={usuarioId} abaInicial={veioDoTour ? 'eu' : 'social'} />;
}

function Home({ usuarioId, abaInicial }: { usuarioId: string; abaInicial: Aba }) {
  const perfil = usePerfil(usuarioId);
  const isAdmin = perfil?.isAdmin === true;
  const sinal = useSinal();
  const formato = useFormato();

  const [aba, setAba] = useState<Aba>(abaInicial);
  const [destino, setDestino] = useState<Destino>({ tipo: 'abas' });
  const [historicoDoTime, setHistoricoDoTime] = useState<Team | null>(null);
  const [editandoTime, setEditandoTime] = useState<Team | null>(null);
  const [criandoTime, setCriandoTime] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [previa, setPrevia] = useState<TeamRoster[] | null>(null);
  const [buscaDeNome, setBuscaDeNome] = useState('');
  const previasRecentes = useRef<string[][][]>([]);

  const { salvando, erro, limparErro, executar } = useAcao();

  const { rodadas, carregando: carregandoRodadas } = useRodadas();
  const { linhas: classificacao } = useClassificacao();
  const { times, carregando: carregandoTimes } = useTodosOsTimes();
  const elencos = useElencos();
  const presentes = useJogadoresAtivos();

  const { jogadores: todosOsJogadores } = useJogadores();
  const desempenho = useDesempenho();
  const meuJogador = useMeuJogador(usuarioId);
  const meuPedido = useMeuPedido(usuarioId);
  const meuContato = useMeuContato(meuJogador?.id);
  const pendentes = usePedidosPendentes();

  const jogadoresPorId = new Map(todosOsJogadores.map((jogador) => [jogador.id, jogador]));
  const termoDeBusca = buscaDeNome.trim().toLowerCase();
  const candidatos = todosOsJogadores
    .filter((jogador) => jogador.profileId === null)
    .filter((jogador) => termoDeBusca === '' || jogador.nome.toLowerCase().includes(termoDeBusca));
  const fila: PedidoNaFila[] = pendentes.map((pedido) => ({
    pedido,
    jogador: jogadoresPorId.get(pedido.playerId),
  }));

  const [secaoDoGrupo, setSecaoDoGrupo] = useState<SecaoDoGrupo>('mural');
  const [criandoPost, setCriandoPost] = useState(false);
  const [eventoEmEdicao, setEventoEmEdicao] = useState<Evento | null>(null);
  const [criandoEvento, setCriandoEvento] = useState(false);
  const [configurandoFinanceiro, setConfigurandoFinanceiro] = useState(false);
  const [painel, setPainel] = useState<LinhaDoPainel[]>([]);
  const [carregandoPainel, setCarregandoPainel] = useState(false);
  const [contas, setContas] = useState<ContaDoGrupo[]>([]);
  const [carregandoContas, setCarregandoContas] = useState(false);

  const sabado = proximoSabado();
  const configGrupo = useConfigGrupo();
  const presencas = usePresencas(sabado);
  const posts = usePosts(usuarioId);
  const eventos = useEventos();
  const paginas = usePaginas();
  const configFinanceiro = useConfigFinanceiro();
  const cobrancas = useCobrancas();
  const extrato = useMeuExtrato();
  const evolucao = useEvolucao();
  const dicas = useDicas();
  const avaliacoesPendentes = useAvaliacoesPendentes(meuJogador?.id);

  const minhaResposta =
    presencas.find((presenca) => presenca.playerId === meuJogador?.id)?.status ?? null;
  const marcos = marcosDe(todosOsJogadores);
  const resumo = resumoDaChamada(presencas, todosOsJogadores.length);

  const carregarContas = (): Promise<void> =>
    executar(async () => {
      setCarregandoContas(true);
      try {
        const { data, error } = await supabase.from('profiles').select();
        if (error) throw new Error('Não foi possível listar as contas. Precisa de internet.');
        const nomeDe = (conta: ContaDoGrupo): string =>
          (conta.nome ?? conta.email ?? '').toLowerCase();
        setContas(
          (data ?? [])
            .map(
              (linha): ContaDoGrupo => ({
                id: String(linha.id),
                nome: linha.nome === null ? null : String(linha.nome),
                email: linha.email === null ? null : String(linha.email),
                isAdmin: Boolean(linha.is_admin),
              }),
            )
            .sort((a, b) => nomeDe(a).localeCompare(nomeDe(b))),
        );
      } finally {
        setCarregandoContas(false);
      }
    });

  const carregarPainel = (): Promise<void> =>
    executar(async () => {
      setCarregandoPainel(true);
      try {
        const { data, error } = await supabase.from('pagamentos').select();
        if (error) throw new Error('Não foi possível carregar o financeiro do grupo. Precisa de internet.');
        const porCobranca = new Map(cobrancas.map((cobranca) => [cobranca.id, cobranca]));
        setPainel(
          (data ?? [])
            .map((linha): LinhaDoPainel => ({
              pagamentoId: String(linha.id),
              nome: jogadoresPorId.get(String(linha.player_id))?.nome ?? 'Jogador removido',
              cobranca: porCobranca.get(String(linha.cobranca_id))?.titulo ?? 'Cobrança',
              valorCentavos: Number(linha.valor_centavos),
              status: String(linha.status) as StatusPagamento,
            }))
            .sort(
              (a, b) =>
                Number(a.status !== 'pendente') - Number(b.status !== 'pendente') ||
                a.nome.localeCompare(b.nome),
            ),
        );
      } finally {
        setCarregandoPainel(false);
      }
    });

  const homensPresentes = presentes.filter((j) => j.genero === 'masculino').length;
  const mulheresPresentes = presentes.filter((j) => j.genero === 'feminino').length;
  const temElencos = elencos.some((elenco) => elenco.players.length > 0);

  const calcularDistribuicao = (): Promise<void> =>
    executar(async () => {
      const jogadores = await Promise.resolve(presentes);
      const timesAtivos = await lerTimesAtivos();
      if (timesAtivos.length === 0) throw new Error('Ative ao menos um time para hoje.');
      if (jogadores.length === 0) throw new Error('Marque quem veio jogar antes de distribuir.');

      const doHistorico = await lerElencosPassados();
      const doElencoAtual: ElencoPassado[] = elencos
        .filter((elenco) => elenco.players.length > 1)
        .map((elenco) => ({
          jogadores: elenco.players.map((jogador) => jogador.id),
          peso: PESO_ELENCO_ATUAL,
        }));
      const dasPrevias: ElencoPassado[] = previasRecentes.current.flatMap((elencosAnteriores, indice) =>
        elencosAnteriores
          .filter((ids) => ids.length > 1)
          .map((ids) => ({ jogadores: ids, peso: PESO_PREVIA_RECENTE / (1.0 + indice) })),
      );

      const historico = HistoricoDeDuplas.de([...doHistorico, ...doElencoAtual, ...dasPrevias]);
      const distribuicao = distribute(jogadores, timesAtivos, historico);

      previasRecentes.current = [
        distribuicao.map((roster) => roster.players.map((jogador) => jogador.id)),
        ...previasRecentes.current,
      ].slice(0, PREVIAS_LEMBRADAS);

      setPrevia(distribuicao);
    });

  const mensagemVisivel = erro ?? aviso;

  if (destino.tipo === 'jogadores') {
    return (
      <div className="app">
        <TelaDeJogadores
          isAdmin={isAdmin}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          executar={executar}
        />
        {mensagemVisivel && (
          <Aviso
            mensagem={mensagemVisivel}
            onFechar={() => {
              limparErro();
              setAviso(null);
            }}
          />
        )}
      </div>
    );
  }

  if (destino.tipo === 'distribuicao') {
    return (
      <div className="app">
        <SorteioScreen
          previa={previa}
          salvando={salvando}
          onVoltar={() => {
            setPrevia(null);
            setDestino({ tipo: 'abas' });
          }}
          onRecalcular={() => void calcularDistribuicao()}
          onAplicar={() => {
            if (!previa) return;
            void executar(async () => {
              await substituirElencos(previa);
              setPrevia(null);
            });
          }}
        />
        {mensagemVisivel && (
          <Aviso mensagem={mensagemVisivel} onFechar={() => { limparErro(); setAviso(null); }} />
        )}
      </div>
    );
  }

  if (destino.tipo === 'aprovacoes') {
    return (
      <div className="app">
        <AprovacoesScreen
          fila={fila}
          salvando={salvando}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          onDecidir={(pedido, aprovado) =>
            void executar(() => decidirPedido(pedido, aprovado, usuarioId))
          }
          onAbrirVinculos={() => setDestino({ tipo: 'vinculos' })}
        />
        {mensagemVisivel && (
          <Aviso
            mensagem={mensagemVisivel}
            onFechar={() => {
              limparErro();
              setAviso(null);
            }}
          />
        )}
      </div>
    );
  }

  if (destino.tipo === 'vinculos') {
    return (
      <div className="app">
        <VinculosScreen
          jogadores={todosOsJogadores}
          contas={contas}
          carregandoContas={carregandoContas}
          salvando={salvando}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          onCarregarContas={() => void carregarContas()}
          onVincular={(playerId, profileId) =>
            void executar(() => vincularJogador(playerId, profileId))
          }
          onDesvincular={(playerId) => void executar(() => vincularJogador(playerId, null))}
        />
        {mensagemVisivel && (
          <Aviso
            mensagem={mensagemVisivel}
            onFechar={() => {
              limparErro();
              setAviso(null);
            }}
          />
        )}
      </div>
    );
  }

  if (destino.tipo === 'painel-financeiro') {
    return (
      <div className="app">
        <PainelFinanceiroScreen
          linhas={painel}
          carregando={carregandoPainel}
          salvando={salvando}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          onRecarregar={() => void carregarPainel()}
          onDefinirStatus={(pagamentoId, status) =>
            void executar(async () => {
              await definirStatusDoPagamento(pagamentoId, status, usuarioId);
              setPainel((atual) =>
                atual.map((linha) =>
                  linha.pagamentoId === pagamentoId ? { ...linha, status } : linha,
                ),
              );
            })
          }
          onGerarMensalidade={() =>
            void executar(async () => {
              const valor = configFinanceiro?.mensalidadeCentavos ?? 0;
              if (valor <= 0) throw new Error('Defina o valor da mensalidade antes de gerar.');
              const quantos = await gerarMensalidade(valor, usuarioId);
              setAviso(
                quantos === 0
                  ? 'A mensalidade deste mês já tinha sido gerada.'
                  : `Mensalidade gerada para ${quantos} mensalistas.`,
              );
            })
          }
          onGerarDiaria={() =>
            void executar(async () => {
              const valor = configFinanceiro?.diariaCentavos ?? 0;
              if (valor <= 0) throw new Error('Defina o valor da diária antes de gerar.');
              const quantos = await gerarDiaria(valor, usuarioId);
              setAviso(
                quantos === 0
                  ? 'Nenhum diarista presente hoje, ou a diária de hoje já foi gerada.'
                  : `Diária gerada para ${quantos} diaristas presentes.`,
              );
            })
          }
          onConfigurar={() => setConfigurandoFinanceiro(true)}
        />
        {configurandoFinanceiro && (
          <ConfigFinanceiroDialogo
            config={configFinanceiro}
            onSalvar={(chave, nome, cidade, mensalidade, diaria) => {
              if (configFinanceiro) {
                void executar(() =>
                  salvarConfigFinanceiro(
                    configFinanceiro.id,
                    chave,
                    nome,
                    cidade,
                    mensalidade,
                    diaria,
                  ),
                );
              }
              setConfigurandoFinanceiro(false);
            }}
            onFechar={() => setConfigurandoFinanceiro(false)}
          />
        )}
        {mensagemVisivel && (
          <Aviso
            mensagem={mensagemVisivel}
            onFechar={() => {
              limparErro();
              setAviso(null);
            }}
          />
        )}
      </div>
    );
  }

  if (destino.tipo === 'avaliacao') {
    return (
      <div className="app">
        <AvaliacaoScreen
          pendentes={avaliacoesPendentes}
          salvando={salvando}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          onEnviar={(pendente, notas) =>
            void executar(async () => {
              if (!meuJogador) return;
              await enviarAvaliacao(
                pendente.dayId,
                meuJogador.id,
                pendente.avaliadoPlayerId,
                notas,
              );
              setAviso(`Avaliação de ${pendente.avaliadoNome} enviada. Ela é anônima.`);
            })
          }
        />
        {mensagemVisivel && (
          <Aviso
            mensagem={mensagemVisivel}
            onFechar={() => {
              limparErro();
              setAviso(null);
            }}
          />
        )}
      </div>
    );
  }

  if (destino.tipo === 'pagina') {
    const atual = paginas.find((pagina) => pagina.id === destino.pagina.id) ?? destino.pagina;
    return (
      <div className="app">
        <PaginaScreen
          pagina={atual}
          isAdmin={isAdmin}
          salvando={salvando}
          onVoltar={() => setDestino({ tipo: 'abas' })}
          onSalvar={(titulo, corpo) =>
            void executar(() => salvarPagina(atual.id, titulo, corpo, usuarioId))
          }
        />
      </div>
    );
  }

  if (destino.tipo === 'partida') {
    return (
      <div className="app">
        <TelaDePlacar
          matchId={destino.matchId}
          isAdmin={isAdmin}
          onVoltar={() => setDestino({ tipo: 'abas' })}
        />
      </div>
    );
  }

  return (
    <div className="app">
      <AppHeader
        subtitulo={subtituloDoFormato(formato.times, formato.quadras)}
        sinal={sinal}
        onSair={() => void sair()}
      />

      <div className="conteudo">
        {aba === 'jogos' && (
          <JogosScreen
            rodadas={rodadas}
            carregando={carregandoRodadas}
            isAdmin={isAdmin}
            gerando={salvando}
            encerrando={salvando}
            temElencos={temElencos}
            presentes={presentes.length}
            onAbrirPartida={(matchId) => setDestino({ tipo: 'partida', matchId })}
            onGerarChaveamento={(quadras) =>
              void executar(async () => {
                const ativos = await lerTimesAtivos();
                if (ativos.length < 2) {
                  throw new Error('Deixe pelo menos 2 times ativos para gerar o chaveamento.');
                }
                await substituirChaveamento(generateSchedule(ativos, quadras));
              })
            }
            onEncerrarDia={() =>
              void executar(async () => {
                const resumo = await encerrarDia();
                setAviso(mensagemDoDia(resumo));
              })
            }
          />
        )}

        {aba === 'classificacao' && (
          <ClassificacaoScreen
            linhas={classificacao}
            isAdmin={isAdmin}
            onApagarResultados={() => void executar(apagarResultados)}
          />
        )}

        {aba === 'times' && (
          <TimesScreen
            times={times}
            elencos={elencos}
            carregando={carregandoTimes}
            presentes={presentes.length}
            homensPresentes={homensPresentes}
            mulheresPresentes={mulheresPresentes}
            isAdmin={isAdmin}
            onAbrirTime={setHistoricoDoTime}
            onEditarTime={setEditandoTime}
            onAlternarAtivoTime={(time) => void executar(() => definirTimeAtivo(time.id, !time.ativo))}
            onAjustarTimes={() =>
              void executar(async () => {
                const todos = await lerTodosOsTimes();
                if (todos.length === 0) throw new Error('Cadastre os times antes de ajustar.');
                if (presentes.length < JOGADORES_POR_TIME) {
                  throw new Error(
                    `Marque ao menos ${JOGADORES_POR_TIME} presentes para montar um time.`,
                  );
                }
                const alvo = Math.min(
                  Math.floor(presentes.length / JOGADORES_POR_TIME),
                  todos.length,
                );
                const porPrioridade = [...todos].sort((a, b) => {
                  if (a.ativo !== b.ativo) return a.ativo ? -1 : 1;
                  if (a.ordem !== b.ordem) return a.ordem - b.ordem;
                  return a.nome.toLowerCase().localeCompare(b.nome.toLowerCase());
                });
                await definirAtivos(
                  new Map(porPrioridade.map((time, indice) => [time.id, indice < alvo])),
                );
              })
            }
            onNovoTime={() => setCriandoTime(true)}
            onAbrirJogadores={() => setDestino({ tipo: 'jogadores' })}
            onDistribuir={() => {
              setDestino({ tipo: 'distribuicao' });
              void calcularDistribuicao();
            }}
          />
        )}

        {aba === 'social' && (
          <GrupoScreen
            secao={secaoDoGrupo}
            isAdmin={isAdmin}
            posts={posts}
            eventos={eventos}
            marcos={marcos}
            paginas={paginas}
            membros={todosOsJogadores}
            presencas={presencas}
            resumo={resumo}
            dataDoSabado={sabado}
            salvando={salvando}
            onSecao={setSecaoDoGrupo}
            onNovoPost={() => setCriandoPost(true)}
            onExcluirPost={(postId) => void executar(() => excluirPost(postId))}
            onReagir={(postId, emoji) =>
              void executar(() => alternarReacao(postId, usuarioId, emoji))
            }
            onNovoEvento={() => setCriandoEvento(true)}
            onEditarEvento={setEventoEmEdicao}
            onExcluirEvento={(eventoId) => void executar(() => excluirEvento(eventoId))}
            onAbrirPagina={(pagina) => setDestino({ tipo: 'pagina', pagina })}
            onResponderPor={(playerId, status) =>
              void executar(() => responderChamada(playerId, sabado, status, false, usuarioId))
            }
            onTrazerConfirmados={() =>
              void executar(async () => {
                const quantos = await trazerConfirmados(sabado);
                setAviso(
                  quantos === 0
                    ? 'Ninguém confirmou presença para este sábado ainda.'
                    : `${quantos} confirmados viraram presença na lista de hoje.`,
                );
              })
            }
          />
        )}

        {aba === 'eu' && (
          <EuScreen
            perfil={perfil}
            dataDoSabado={sabado}
            jogoHora={configGrupo?.jogoHora ?? null}
            jogoLocal={configGrupo?.jogoLocal ?? null}
            minhaResposta={minhaResposta}
            onResponderChamada={(status) =>
              void executar(async () => {
                if (!meuJogador) return;
                await responderChamada(meuJogador.id, sabado, status, true, usuarioId);
              })
            }
            extras={
              <>
                <CartaoDoExtrato
                  extrato={extrato}
                  config={configFinanceiro}
                  isAdmin={isAdmin}
                  onAbrirPainel={() => {
                    void carregarPainel();
                    setDestino({ tipo: 'painel-financeiro' });
                  }}
                />
                <CartaoDaEvolucao
                  evolucao={evolucao}
                  dicas={dicas}
                  pendentes={avaliacoesPendentes}
                  onAvaliar={() => setDestino({ tipo: 'avaliacao' })}
                />
              </>
            }
            meuJogador={meuJogador}
            meuPedido={meuPedido}
            meuContato={meuContato}
            meuDesempenho={meuJogador ? desempenho.get(meuJogador.id) : undefined}
            candidatos={candidatos}
            fila={fila}
            busca={buscaDeNome}
            salvando={salvando}
            onBuscar={setBuscaDeNome}
            onPedirVinculo={(playerId) =>
              void executar(async () => {
                await pedirVinculo(usuarioId, perfil?.nome ?? perfil?.email ?? null, playerId);
                setBuscaDeNome('');
              })
            }
            onCancelarPedido={() => {
              if (!meuPedido) return;
              void executar(() => cancelarPedido(meuPedido.id));
            }}
            onSalvarFicha={(nome, dia, mes, telefone, emergencia, ano, regime) => {
              if (!meuJogador) return;
              void executar(async () => {
                await salvarFicha(meuJogador.id, nome, dia, mes, regime);
                await salvarContato(meuJogador.id, usuarioId, telefone, emergencia, ano);
              });
            }}
            onAbrirAprovacoes={() => setDestino({ tipo: 'aprovacoes' })}
          />
        )}
      </div>

      <nav className="abas" role="tablist">
        <button
          type="button"
          role="tab"
          className="aba"
          aria-selected={aba === 'social'}
          onClick={() => setAba('social')}
        >
          <IconeGrupos />
          Social
        </button>
        <button
          type="button"
          role="tab"
          className="aba"
          aria-selected={aba === 'jogos'}
          onClick={() => setAba('jogos')}
        >
          <IconeVolei />
          Jogos
        </button>
        <button
          type="button"
          role="tab"
          className="aba"
          aria-selected={aba === 'classificacao'}
          onClick={() => setAba('classificacao')}
        >
          <IconeTrofeu />
          Classificação
        </button>
        <button
          type="button"
          role="tab"
          className="aba"
          aria-selected={aba === 'times'}
          onClick={() => setAba('times')}
        >
          <IconeGrupos />
          Times
        </button>
        <button
          type="button"
          role="tab"
          className="aba"
          aria-selected={aba === 'eu'}
          onClick={() => setAba('eu')}
        >
          <span style={{ position: 'relative', display: 'flex' }}>
            <IconePessoa tamanho={22} />
            {avaliacoesPendentes.length > 0 && (
              <span
                aria-label="Tem companheiro esperando a sua nota"
                style={{
                  position: 'absolute',
                  top: -1,
                  right: -3,
                  width: 9,
                  height: 9,
                  borderRadius: '50%',
                  background: 'var(--verde-claro)',
                  border: '1px solid var(--fundo-cabecalho)',
                }}
              />
            )}
          </span>
          Eu
        </button>
      </nav>

      {criandoPost && (
        <PostDialogo
          salvando={salvando}
          onSalvar={(titulo, corpo, fixado, imagem, emoji) => {
            void executar(async () => {
              const imagemUrl = imagem === null ? null : await enviarImagemDoMural(imagem);
              await publicarPost(
                usuarioId,
                perfil?.nome ?? null,
                titulo,
                corpo,
                fixado,
                imagemUrl,
                emoji,
              );
            });
            setCriandoPost(false);
          }}
          onFechar={() => setCriandoPost(false)}
        />
      )}

      {(criandoEvento || eventoEmEdicao !== null) && (
        <EventoDialogo
          evento={eventoEmEdicao}
          salvando={salvando}
          onSalvar={(id, titulo, descricao, tipo, inicio, local) => {
            void executar(() =>
              salvarEvento(id, titulo, descricao, tipo, inicio, local, usuarioId),
            );
            setCriandoEvento(false);
            setEventoEmEdicao(null);
          }}
          onFechar={() => {
            setCriandoEvento(false);
            setEventoEmEdicao(null);
          }}
        />
      )}

      {historicoDoTime && (
        <HistoricoDoTime
          time={historicoDoTime}
          isAdmin={isAdmin}
          onFechar={() => setHistoricoDoTime(null)}
        />
      )}

      {(criandoTime || editandoTime) && (
        <TeamEditorDialog
          time={editandoTime}
          onSalvarNovo={(nome, cor, sigla) => {
            void executar(async () => {
              const todos = await lerTodosOsTimes();
              const proximaOrdem = todos.reduce((maior, time) => Math.max(maior, time.ordem), 0) + 1;
              await criarTime(nome, cor, sigla, proximaOrdem);
            });
            setCriandoTime(false);
          }}
          onSalvarExistente={(time) => {
            void executar(() => atualizarTime(time));
            setEditandoTime(null);
          }}
          onExcluir={(teamId) => {
            void executar(() => excluirTime(teamId));
            setEditandoTime(null);
          }}
          onFechar={() => {
            setCriandoTime(false);
            setEditandoTime(null);
          }}
        />
      )}

      {mensagemVisivel && (
        <Aviso
          mensagem={mensagemVisivel}
          onFechar={() => {
            limparErro();
            setAviso(null);
          }}
        />
      )}
    </div>
  );
}

function HistoricoDoTime({
  time,
  isAdmin,
  onFechar,
}: {
  time: Team;
  isAdmin: boolean;
  onFechar: () => void;
}) {
  const elenco = useElencoDoTime(time.id);
  const partidas = useHistoricoDoTime(time.id);
  return (
    <TeamHistoryDialog
      time={time}
      isAdmin={isAdmin}
      elenco={elenco}
      partidas={partidas}
      onFechar={onFechar}
    />
  );
}

function TelaDePlacar({
  matchId,
  isAdmin,
  onVoltar,
}: {
  matchId: string;
  isAdmin: boolean;
  onVoltar: () => void;
}) {
  const partida = usePartida(matchId);
  const { salvando, erro, executar } = useAcao();
  const [erroLocal, setErroLocal] = useState<string | null>(null);

  return (
    <PlacarScreen
      partida={partida}
      isAdmin={isAdmin}
      salvando={salvando}
      erro={erroLocal ?? erro}
      onVoltar={onVoltar}
      onSomar={(ladoA, delta) => {
        if (!partida) return;
        const novoA = Math.max(partida.scoreA + (ladoA ? delta : 0), 0);
        const novoB = Math.max(partida.scoreB + (ladoA ? 0 : delta), 0);
        void executar(() => atualizarPlacar(matchId, novoA, novoB));
      }}
      onDefinirPlacar={(ladoA, valor) => {
        if (!partida) return;
        const novoA = ladoA ? valor : partida.scoreA;
        const novoB = ladoA ? partida.scoreB : valor;
        if (novoA === partida.scoreA && novoB === partida.scoreB) return;
        void executar(() => atualizarPlacar(matchId, novoA, novoB));
      }}
      onFinalizar={() => {
        if (!partida) return;
        if (partida.scoreA === partida.scoreB) {
          setErroLocal('Empate nao finaliza: ajuste o placar antes.');
          return;
        }
        setErroLocal(null);
        void executar(() =>
          finalizarPartida(matchId, partida.scoreA, partida.scoreB, partida.teamA.id, partida.teamB.id),
        );
      }}
      onReabrir={() => void executar(() => reabrirPartida(matchId))}
    />
  );
}

function TelaDeJogadores({
  isAdmin,
  onVoltar,
  executar,
}: {
  isAdmin: boolean;
  onVoltar: () => void;
  executar: (acao: () => Promise<void>) => Promise<void>;
}) {
  const { jogadores, carregando } = useJogadores();
  const desempenho = useDesempenho();
  const [busca, setBusca] = useState('');
  const [filtro, setFiltro] = useState<FiltroPresenca>('todos');

  const presentes = jogadores.filter((jogador) => jogador.ativo);

  return (
    <JogadoresScreen
      jogadores={filtrarJogadores(jogadores, busca, filtro)}
      desempenho={desempenho}
      carregando={carregando}
      isAdmin={isAdmin}
      total={jogadores.length}
      presentes={presentes.length}
      homensPresentes={presentes.filter((j) => j.genero === 'masculino').length}
      mulheresPresentes={presentes.filter((j) => j.genero === 'feminino').length}
      busca={busca}
      filtro={filtro}
      onVoltar={onVoltar}
      onBuscar={setBusca}
      onFiltrar={setFiltro}
      onAlternarPresenca={(jogador) => void executar(() => definirPresenca(jogador.id, !jogador.ativo))}
      onMarcarTodosPresentes={() => void executar(() => definirPresencaDeTodos(true))}
      onLimparPresencas={() => void executar(() => definirPresencaDeTodos(false))}
      onCriar={(nome, nivel, genero, ativo) =>
        void executar(() => criarJogador(nome, Math.min(Math.max(nivel, 1), 5), genero, ativo))
      }
      onSalvar={(jogador) =>
        void executar(() =>
          atualizarJogador({ ...jogador, skillLevel: Math.min(Math.max(jogador.skillLevel, 1), 5) }),
        )
      }
      onExcluir={(playerId) => void executar(() => excluirJogador(playerId))}
    />
  );
}
