import { useState } from 'react';
import {
  aniversarioDe,
  REGIMES,
  regimeDe,
  rotuloDoRegime,
  saldoDoDesempenho,
  type Genero,
  type Player,
  type PlayerPerformance,
  type Regime,
} from '../../domain/models';
import {
  CampoBusca,
  CampoTexto,
  Cartao,
  Dialogo,
  DialogoConfirmacao,
  EstadoVazio,
  Estrelas,
  Interruptor,
  RotuloPequeno,
  Selo,
  SeletorGenero,
} from '../components/Componentes';
import {
  IconeMais,
  IconeMarcarTodos,
  IconePessoaFora,
  IconeVoltar,
} from '../components/Icons';

export type FiltroPresenca = 'todos' | 'presentes' | 'ausentes';

const ROTULOS: Record<FiltroPresenca, string> = {
  todos: 'Todos',
  presentes: 'Presentes',
  ausentes: 'Ausentes',
};

const paraBusca = (texto: string): string =>
  texto
    .trim()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();

export function filtrarJogadores(
  jogadores: Player[],
  busca: string,
  filtro: FiltroPresenca,
): Player[] {
  const alvo = paraBusca(busca);
  return jogadores.filter((jogador) => {
    const presencaCombina =
      filtro === 'todos' ? true : filtro === 'presentes' ? jogador.ativo : !jogador.ativo;
    return presencaCombina && (alvo.length === 0 || paraBusca(jogador.nome).includes(alvo));
  });
}

const comSinal = (valor: number): string => (valor > 0 ? `+${valor}` : String(valor));

const resumoDoDesempenho = (desempenho: PlayerPerformance): string => {
  const dias = `${desempenho.dias} ${desempenho.dias === 1 ? 'dia' : 'dias'}`;
  if (desempenho.jogos === 0) return `${dias} de presença`;
  return `${dias} • ${desempenho.jogos} jogos • ${desempenho.vitorias}V ${desempenho.derrotas}D • saldo ${comSinal(saldoDoDesempenho(desempenho))}`;
};

export const JogadoresScreen = ({
  jogadores,
  desempenho,
  carregando,
  total,
  presentes,
  homensPresentes,
  mulheresPresentes,
  busca,
  filtro,
  onVoltar,
  onBuscar,
  onFiltrar,
  isAdmin,
  onAlternarPresenca,
  onMarcarTodosPresentes,
  onLimparPresencas,
  onCriar,
  onSalvar,
  onExcluir,
}: {
  jogadores: Player[];
  desempenho: Map<string, PlayerPerformance>;
  carregando: boolean;
  total: number;
  presentes: number;
  homensPresentes: number;
  mulheresPresentes: number;
  busca: string;
  filtro: FiltroPresenca;
  onVoltar: () => void;
  onBuscar: (texto: string) => void;
  onFiltrar: (filtro: FiltroPresenca) => void;
  isAdmin: boolean;
  onAlternarPresenca: (jogador: Player) => void;
  onMarcarTodosPresentes: () => void;
  onLimparPresencas: () => void;
  onCriar: (nome: string, nivel: number, genero: Genero, ativo: boolean) => void;
  onSalvar: (jogador: Player) => void;
  onExcluir: (playerId: string) => void;
}) => {
  const [editando, setEditando] = useState<Player | null>(null);
  const [criando, setCriando] = useState(false);
  const [confirmandoLimpeza, setConfirmandoLimpeza] = useState(false);

  const descricaoDaListaVazia = (): string => {
    if (busca.trim().length > 0) return 'Nenhum jogador do grupo combina com a busca.';
    if (filtro === 'presentes') return 'Ninguém marcado como presente ainda.';
    if (filtro === 'ausentes') return 'Todo mundo está marcado como presente.';
    return 'Nenhum jogador para mostrar.';
  };

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Jogadores</span>
          <span className="subtitulo">
            {presentes} presentes de {total} • {homensPresentes}H / {mulheresPresentes}M
          </span>
        </div>
      </div>

      <div className="coluna" style={{ padding: '0 16px', gap: 8, flex: 'none' }}>
        <CampoBusca valor={busca} onMudar={onBuscar} dica="Buscar jogador pelo nome" />
        {isAdmin && (
        <div className="linha" style={{ gap: 8 }}>
          {(Object.keys(ROTULOS) as FiltroPresenca[]).map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="chip"
              aria-pressed={opcao === filtro}
              onClick={() => onFiltrar(opcao)}
            >
              {ROTULOS[opcao]}
            </button>
          ))}
        </div>
        )}
        {isAdmin && (
        <div className="linha" style={{ gap: 8 }}>
          <button
            type="button"
            className="botao botao-contorno"
            disabled={presentes >= total}
            onClick={onMarcarTodosPresentes}
          >
            <span style={{ color: presentes < total ? 'var(--verde)' : 'var(--texto-terciario)', display: 'flex' }}>
              <IconeMarcarTodos />
            </span>
            Marcar todos
          </button>
          <button
            type="button"
            className="botao botao-contorno"
            disabled={presentes === 0}
            onClick={() => setConfirmandoLimpeza(true)}
          >
            <span style={{ color: presentes > 0 ? 'var(--vermelho)' : 'var(--texto-terciario)', display: 'flex' }}>
              <IconePessoaFora tamanho={16} />
            </span>
            Limpar presenças
          </button>
        </div>
        )}
      </div>

      <div className="linha-entre" style={{ padding: '14px 20px 0 16px', flex: 'none' }}>
        <RotuloPequeno>Jogador</RotuloPequeno>
        {isAdmin && <RotuloPequeno cor="var(--texto-secundario)">Presente?</RotuloPequeno>}
      </div>

      <div className="conteudo">
        {total === 0 && !carregando && (
          <EstadoVazio
            titulo="Nenhum jogador"
            descricao="Toque no botão + para cadastrar o primeiro jogador do grupo."
          />
        )}
        {total > 0 && jogadores.length === 0 && !carregando && (
          <EstadoVazio titulo="Nada nessa lista" descricao={descricaoDaListaVazia()} />
        )}

        <div className="lista lista-compacta" style={{ paddingBottom: 96 }}>
          {jogadores.map((jogador) => (
            <LinhaJogador
              key={jogador.id}
              jogador={jogador}
              desempenho={desempenho.get(jogador.id)}
              isAdmin={isAdmin}
              onClick={() => { if (isAdmin) setEditando(jogador); }}
              onAlternarPresenca={() => onAlternarPresenca(jogador)}
            />
          ))}
        </div>
      </div>

      {isAdmin && (
        <button type="button" className="fab" aria-label="Novo jogador" onClick={() => setCriando(true)}>
          <IconeMais tamanho={24} />
        </button>
      )}

      {confirmandoLimpeza && (
        <DialogoConfirmacao
          titulo="Limpar presenças?"
          mensagem={`Os ${presentes} jogadores marcados como presentes ficam ausentes. Marque de novo quem chegou para jogar hoje.`}
          textoConfirmar="Limpar"
          onConfirmar={() => {
            onLimparPresencas();
            setConfirmandoLimpeza(false);
          }}
          onCancelar={() => setConfirmandoLimpeza(false)}
        />
      )}

      {criando && (
        <PlayerEditorDialog
          jogador={null}
          onSalvarNovo={(nome, nivel, genero, ativo) => {
            onCriar(nome, nivel, genero, ativo);
            setCriando(false);
          }}
          onSalvarExistente={() => undefined}
          onExcluir={() => undefined}
          onFechar={() => setCriando(false)}
        />
      )}

      {editando && (
        <PlayerEditorDialog
          jogador={editando}
          onSalvarNovo={() => undefined}
          onSalvarExistente={(jogador) => {
            onSalvar(jogador);
            setEditando(null);
          }}
          onExcluir={(playerId) => {
            onExcluir(playerId);
            setEditando(null);
          }}
          onFechar={() => setEditando(null)}
        />
      )}
    </div>
  );
};

const fichaResumida = (jogador: Player): string =>
  aniversarioDe(jogador) ? `aniversário ${aniversarioDe(jogador)}` : '';

const LinhaJogador = ({
  jogador,
  desempenho,
  isAdmin,
  onClick,
  onAlternarPresenca,
}: {
  jogador: Player;
  desempenho: PlayerPerformance | undefined;
  isAdmin: boolean;
  onClick: () => void;
  onAlternarPresenca: () => void;
}) => (
  <Cartao apagado={!jogador.ativo}>
    <div className="linha" style={{ padding: '10px 12px 10px 14px', gap: 8 }}>
      <button type="button" className="expandir coluna" style={{ gap: 3, textAlign: 'left' }} onClick={onClick}>
        <div className="linha">
          <span
            style={{
              fontSize: 15,
              fontWeight: 600,
              color: jogador.ativo ? 'var(--texto-primario)' : 'var(--texto-terciario)',
            }}
          >
            {jogador.nome}
          </span>
          <Selo
            texto={jogador.genero === 'masculino' ? 'H' : 'M'}
            corTexto="var(--selo-fase-texto)"
            corFundo="var(--selo-fase-fundo)"
          />
        </div>
        {isAdmin && <Estrelas nivel={jogador.skillLevel} />}
        {fichaResumida(jogador) && (
          <span className="subtitulo" style={{ fontSize: 11, color: 'var(--texto-terciario)' }}>
            {fichaResumida(jogador)}
          </span>
        )}
        {desempenho && desempenho.dias > 0 && (
          <span className="subtitulo" style={{ fontSize: 11 }}>
            {resumoDoDesempenho(desempenho)}
          </span>
        )}
      </button>
      {isAdmin ? (
        <Interruptor
          ligado={jogador.ativo}
          onMudar={onAlternarPresenca}
          descricao={`${jogador.nome} presente hoje`}
        />
      ) : (
        jogador.profileId !== null && (
          <Selo texto="Tem acesso" corTexto="var(--verde-claro)" corFundo="var(--selo-vitoria-fundo)" />
        )
      )}
    </div>
  </Cartao>
);

const PlayerEditorDialog = ({
  jogador,
  onSalvarNovo,
  onSalvarExistente,
  onExcluir,
  onFechar,
}: {
  jogador: Player | null;
  onSalvarNovo: (nome: string, nivel: number, genero: Genero, ativo: boolean) => void;
  onSalvarExistente: (jogador: Player) => void;
  onExcluir: (playerId: string) => void;
  onFechar: () => void;
}) => {
  const [nome, setNome] = useState(jogador?.nome ?? '');
  const [nivel, setNivel] = useState(jogador?.skillLevel ?? 3);
  const [genero, setGenero] = useState<Genero>(jogador?.genero ?? 'masculino');
  const [ativo, setAtivo] = useState(jogador?.ativo ?? true);
  const [regime, setRegime] = useState<Regime>(regimeDe(jogador?.regime));
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false);
  const valido = nome.trim().length > 0;

  if (jogador && confirmandoExclusao) {
    return (
      <DialogoConfirmacao
        titulo={`Excluir ${jogador.nome}?`}
        mensagem="O jogador sai do grupo e o desempenho dele nos dias já encerrados é apagado. Para tirar alguém só de hoje, use o Presente? na listagem."
        textoConfirmar="Excluir"
        onConfirmar={() => onExcluir(jogador.id)}
        onCancelar={() => setConfirmandoExclusao(false)}
      />
    );
  }

  return (
    <Dialogo
      titulo={jogador ? 'Editar jogador' : 'Novo jogador'}
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={!valido}
            style={{ color: valido ? 'var(--verde)' : 'var(--texto-terciario)' }}
            onClick={() =>
              jogador
                ? onSalvarExistente({ ...jogador, nome, skillLevel: nivel, genero, ativo, regime })
                : onSalvarNovo(nome, nivel, genero, ativo)
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={nome} rotulo="Nome" onMudar={setNome} />
      <div className="campo">
        <span className="campo-rotulo">Nível de habilidade</span>
        <Estrelas nivel={nivel} tamanho={32} onMudar={setNivel} />
      </div>
      <div className="campo">
        <span className="campo-rotulo">Gênero</span>
        <SeletorGenero genero={genero} onMudar={setGenero} />
      </div>
      {jogador && (
        <div className="campo">
          <span className="campo-rotulo">Cobrança</span>
          <div className="linha" style={{ gap: 6 }}>
            {REGIMES.map((opcao) => (
              <button
                key={opcao}
                type="button"
                className="chip"
                aria-pressed={regime === opcao}
                onClick={() => setRegime(opcao)}
              >
                {rotuloDoRegime(opcao)}
              </button>
            ))}
          </div>
        </div>
      )}
      <div className="linha-entre">
        <span className="subtitulo" style={{ fontSize: 13 }}>
          Presente hoje
        </span>
        <Interruptor ligado={ativo} onMudar={setAtivo} descricao="Presente hoje" />
      </div>
      {jogador && (
        <button
          type="button"
          className="botao-texto"
          style={{ color: 'var(--vermelho)', alignSelf: 'flex-start', padding: 0 }}
          onClick={() => setConfirmandoExclusao(true)}
        >
          Excluir jogador
        </button>
      )}
    </Dialogo>
  );
};
