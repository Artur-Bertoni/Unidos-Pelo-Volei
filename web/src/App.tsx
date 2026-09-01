import { useRef, useState } from 'react';
import { encerrarDia, lerElencosPassados } from './data/gameDays';
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
import type { ResumoDoDia, Team, TeamRoster } from './domain/models';
import { generateSchedule } from './domain/roundRobin';
import {
  distribute,
  HistoricoDeDuplas,
  JOGADORES_POR_TIME,
  type ElencoPassado,
} from './domain/teamDraft';
import { chavesFaltando, configurado, env } from './lib/env';
import { entrarComGoogle, sair } from './lib/supabase';
import {
  AppHeader,
  Aviso,
  Carregando,
} from './ui/components/Componentes';
import { IconeGrupos, IconeTrofeu, IconeVolei } from './ui/components/Icons';
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
  usePartida,
  usePerfil,
  useRodadas,
  useSessao,
  useSinal,
  useTodosOsTimes,
} from './ui/hooks';
import { ClassificacaoScreen } from './ui/screens/Classificacao';
import { JogadoresScreen, filtrarJogadores, type FiltroPresenca } from './ui/screens/Jogadores';
import { JogosScreen } from './ui/screens/Jogos';
import { ConfiguracaoPendenteScreen, LoginScreen } from './ui/screens/Login';
import { PlacarScreen } from './ui/screens/Placar';
import { SorteioScreen } from './ui/screens/Sorteio';
import { TeamEditorDialog, TeamHistoryDialog, TimesScreen } from './ui/screens/Times';

const PESO_ELENCO_ATUAL = 1.5;
const PESO_PREVIA_RECENTE = 1.5;
const PREVIAS_LEMBRADAS = 3;

type Aba = 'jogos' | 'classificacao' | 'times';
type Destino =
  | { tipo: 'abas' }
  | { tipo: 'jogadores' }
  | { tipo: 'distribuicao' }
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

  return <Home usuarioId={session.user.id} />;
}

function Home({ usuarioId }: { usuarioId: string }) {
  const perfil = usePerfil(usuarioId);
  const isAdmin = perfil?.isAdmin === true;
  const sinal = useSinal();
  const formato = useFormato();

  const [aba, setAba] = useState<Aba>('jogos');
  const [destino, setDestino] = useState<Destino>({ tipo: 'abas' });
  const [historicoDoTime, setHistoricoDoTime] = useState<Team | null>(null);
  const [editandoTime, setEditandoTime] = useState<Team | null>(null);
  const [criandoTime, setCriandoTime] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [previa, setPrevia] = useState<TeamRoster[] | null>(null);
  const previasRecentes = useRef<string[][][]>([]);

  const { salvando, erro, limparErro, executar } = useAcao();

  const { rodadas, carregando: carregandoRodadas } = useRodadas();
  const { linhas: classificacao } = useClassificacao();
  const { times, carregando: carregandoTimes } = useTodosOsTimes();
  const elencos = useElencos();
  const presentes = useJogadoresAtivos();

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
        <TelaDeJogadores onVoltar={() => setDestino({ tipo: 'abas' })} executar={executar} />
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
      </div>

      <nav className="abas" role="tablist">
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
      </nav>

      {historicoDoTime && (
        <HistoricoDoTime time={historicoDoTime} onFechar={() => setHistoricoDoTime(null)} />
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

function HistoricoDoTime({ time, onFechar }: { time: Team; onFechar: () => void }) {
  const elenco = useElencoDoTime(time.id);
  const partidas = useHistoricoDoTime(time.id);
  return <TeamHistoryDialog time={time} elenco={elenco} partidas={partidas} onFechar={onFechar} />;
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
  onVoltar,
  executar,
}: {
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
