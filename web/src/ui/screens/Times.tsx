import { useState } from 'react';
import { JOGADORES_POR_TIME } from '../../domain/teamDraft';
import {
  forcaTotal,
  type MatchCard,
  type Player,
  type Team,
  type TeamRoster,
} from '../../domain/models';
import {
  CampoTexto,
  Cartao,
  CORES_DE_TIME,
  Dialogo,
  DialogoConfirmacao,
  EstadoVazio,
  Interruptor,
  PontoDoTime,
  RotuloPequeno,
  ScoreBox,
  Selo,
  SeloResultado,
  SeletorCor,
  TeamCircle,
} from '../components/Componentes';
import {
  IconeEditar,
  IconeFechar,
  IconeInterruptor,
  IconeMais,
  IconePessoa,
  IconeSortear,
} from '../components/Icons';

interface DicaDoDia {
  texto: string;
  ajustavel: boolean;
}

export function dicaDeTimes(
  presentes: number,
  timesCadastrados: number,
  timesAtivos: number,
): DicaDoDia | null {
  const timesQueCabem = Math.floor(presentes / JOGADORES_POR_TIME);
  const cabem = Math.min(timesQueCabem, timesCadastrados);
  const sobrando = timesAtivos - cabem;

  if (presentes === 0) return { texto: 'Marque em Jogadores quem veio jogar hoje.', ajustavel: false };
  if (timesQueCabem === 0) {
    return { texto: 'Menos de 4 presentes: ainda não dá para um time.', ajustavel: false };
  }
  if (timesQueCabem > timesCadastrados) {
    return {
      texto: `Dá para ${timesQueCabem} times de 4: cadastre mais ${timesQueCabem - timesCadastrados}.`,
      ajustavel: sobrando !== 0,
    };
  }
  if (sobrando > 0) return { texto: `Dá para ${cabem} times de 4: desative ${sobrando}.`, ajustavel: true };
  if (sobrando < 0) return { texto: `Dá para ${cabem} times de 4: ative mais ${-sobrando}.`, ajustavel: true };
  return null;
}

export const TimesScreen = ({
  times,
  elencos,
  carregando,
  presentes,
  homensPresentes,
  mulheresPresentes,
  isAdmin,
  onAbrirTime,
  onEditarTime,
  onAlternarAtivoTime,
  onAjustarTimes,
  onNovoTime,
  onAbrirJogadores,
  onDistribuir,
}: {
  times: Team[];
  elencos: TeamRoster[];
  carregando: boolean;
  presentes: number;
  homensPresentes: number;
  mulheresPresentes: number;
  isAdmin: boolean;
  onAbrirTime: (time: Team) => void;
  onEditarTime: (time: Team) => void;
  onAlternarAtivoTime: (time: Team) => void;
  onAjustarTimes: () => void;
  onNovoTime: () => void;
  onAbrirJogadores: () => void;
  onDistribuir: () => void;
}) => {
  const timesAtivos = times.filter((time) => time.ativo).length;
  const dica = isAdmin ? dicaDeTimes(presentes, times.length, timesAtivos) : null;

  return (
    <div className="lista">
      <div className="coluna" style={{ gap: 2 }}>
        <strong style={{ fontSize: 18 }}>Equipes Participantes</strong>
        <span className="subtitulo">
          {presentes} presentes ({homensPresentes}H / {mulheresPresentes}M) • {timesAtivos} times ativos
        </span>
        {dica && (
          <div className="linha" style={{ marginTop: 4 }}>
            <span className="expandir" style={{ color: 'var(--dourado)', fontSize: 12 }}>
              {dica.texto}
            </span>
            {dica.ajustavel && (
              <button
                type="button"
                className="botao-texto"
                style={{ color: 'var(--verde)', fontSize: 12 }}
                onClick={onAjustarTimes}
              >
                Ajustar
              </button>
            )}
          </div>
        )}
      </div>

      <div className="linha" style={{ gap: 8 }}>
        <BotaoAcao texto="Jogadores" onClick={onAbrirJogadores}>
          <IconePessoa tamanho={16} />
        </BotaoAcao>
        {isAdmin && (
          <>
            <BotaoAcao texto="Distribuir" onClick={onDistribuir}>
              <IconeSortear />
            </BotaoAcao>
            <BotaoAcao texto="Novo time" onClick={onNovoTime}>
              <IconeMais tamanho={16} />
            </BotaoAcao>
          </>
        )}
      </div>

      {times.length === 0 && !carregando ? (
        <EstadoVazio
          titulo="Nenhum time cadastrado"
          descricao={
            isAdmin
              ? 'Toque em Novo time para criar as equipes coloridas.'
              : 'Um administrador ainda não cadastrou as equipes.'
          }
        />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 12,
          }}
        >
          {times.map((time) => {
            const elenco = elencos.find((item) => item.team.id === time.id);
            return (
              <CartaoTime
                key={time.id}
                time={time}
                jogadores={elenco?.players.length ?? 0}
                forca={elenco ? forcaTotal(elenco) : 0}
                onClick={() => onAbrirTime(time)}
                onEditar={isAdmin ? () => onEditarTime(time) : undefined}
                onAlternarAtivo={isAdmin ? () => onAlternarAtivoTime(time) : undefined}
              />
            );
          })}
        </div>
      )}
    </div>
  );
};

const BotaoAcao = ({
  texto,
  onClick,
  children,
}: {
  texto: string;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button type="button" className="botao botao-contorno" style={{ padding: '10px 8px' }} onClick={onClick}>
    <span style={{ color: 'var(--verde)', display: 'flex' }}>{children}</span>
    <span style={{ fontSize: 12, whiteSpace: 'nowrap' }}>{texto}</span>
  </button>
);

const CartaoTime = ({
  time,
  jogadores,
  forca,
  onClick,
  onEditar,
  onAlternarAtivo,
}: {
  time: Team;
  jogadores: number;
  forca: number;
  onClick: () => void;
  onEditar?: () => void;
  onAlternarAtivo?: () => void;
}) => (
  <div style={{ position: 'relative' }}>
    <Cartao apagado={!time.ativo}>
      <button
        type="button"
        className="coluna"
        style={{
          width: '100%',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          padding: '34px 6px 14px',
          opacity: time.ativo ? 1 : 0.45,
          aspectRatio: '0.86',
        }}
        onClick={onClick}
      >
        <TeamCircle sigla={time.sigla} corHex={time.corHex} tamanho={52} />
        <span
          style={{
            fontSize: 13,
            fontWeight: 700,
            textAlign: 'center',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            maxWidth: '100%',
          }}
        >
          {time.nome}
        </span>
        <span className="terciario" style={{ fontSize: 10, textAlign: 'center' }}>
          {!time.ativo ? 'fora de hoje' : jogadores > 0 ? `${jogadores} jog. • força ${forca}` : 'sem elenco'}
        </span>
      </button>
    </Cartao>

    {onAlternarAtivo && (
      <button
        type="button"
        style={{
          position: 'absolute',
          top: 6,
          left: 6,
          padding: 6,
          color: time.ativo ? 'var(--verde)' : 'var(--texto-terciario)',
          display: 'flex',
        }}
        aria-label={time.ativo ? 'Tirar o time de hoje' : 'Colocar o time em hoje'}
        onClick={onAlternarAtivo}
      >
        <IconeInterruptor ligado={time.ativo} />
      </button>
    )}

    {onEditar && (
      <button
        type="button"
        style={{ position: 'absolute', top: 8, right: 6, padding: 6, color: 'var(--texto-terciario)', display: 'flex' }}
        aria-label="Editar time"
        onClick={onEditar}
      >
        <IconeEditar />
      </button>
    )}
  </div>
);

export const TeamEditorDialog = ({
  time,
  onSalvarNovo,
  onSalvarExistente,
  onExcluir,
  onFechar,
}: {
  time: Team | null;
  onSalvarNovo: (nome: string, corHex: string, sigla: string) => void;
  onSalvarExistente: (time: Team) => void;
  onExcluir: (teamId: string) => void;
  onFechar: () => void;
}) => {
  const [nome, setNome] = useState(time?.nome ?? '');
  const [sigla, setSigla] = useState(time?.sigla ?? '');
  const [cor, setCor] = useState(time?.corHex ?? CORES_DE_TIME[0]);
  const [ativo, setAtivo] = useState(time?.ativo ?? true);
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false);

  const valido = nome.trim().length > 0 && sigla.trim().length === 2;

  if (time && confirmandoExclusao) {
    return (
      <DialogoConfirmacao
        titulo={`Excluir o time ${time.nome}?`}
        mensagem="O time sai da lista e os jogos dele no chaveamento de hoje são apagados. Para tirar o time só de hoje, desative-o."
        textoConfirmar="Excluir"
        onConfirmar={() => onExcluir(time.id)}
        onCancelar={() => setConfirmandoExclusao(false)}
      />
    );
  }

  return (
    <Dialogo
      onFechar={onFechar}
      titulo={
        <div className="linha" style={{ gap: 10 }}>
          <TeamCircle sigla={sigla || '??'} corHex={cor} tamanho={36} />
          {time ? 'Editar time' : 'Novo time'}
        </div>
      }
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
              time
                ? onSalvarExistente({ ...time, nome, corHex: cor, sigla, ativo })
                : onSalvarNovo(nome, cor, sigla)
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={nome} rotulo="Nome" onMudar={setNome} />
      <CampoTexto
        valor={sigla}
        rotulo="Sigla (2 letras)"
        maiusculas
        maxLength={2}
        onMudar={(valor) => setSigla(valor.slice(0, 2))}
      />
      <div className="campo">
        <span className="campo-rotulo">Cor</span>
        <SeletorCor corSelecionada={cor} onMudar={setCor} />
      </div>
      {time && (
        <>
          <div className="linha-entre">
            <span className="subtitulo" style={{ fontSize: 13 }}>
              Time ativo hoje
            </span>
            <Interruptor ligado={ativo} onMudar={setAtivo} descricao="Time ativo hoje" />
          </div>
          <button
            type="button"
            className="botao-texto"
            style={{ color: 'var(--vermelho)', alignSelf: 'flex-start', padding: 0 }}
            onClick={() => setConfirmandoExclusao(true)}
          >
            Excluir time
          </button>
        </>
      )}
    </Dialogo>
  );
};

export const TeamHistoryDialog = ({
  time,
  elenco,
  partidas,
  onFechar,
}: {
  time: Team;
  elenco: Player[];
  partidas: MatchCard[];
  onFechar: () => void;
}) => (
  <div className="fundo-modal" onClick={onFechar} role="presentation">
    <div
      className="dialogo"
      style={{ padding: 0, gap: 0 }}
      onClick={(e) => e.stopPropagation()}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="linha"
        style={{ background: 'var(--fundo-cabecalho)', padding: 16, gap: 12, flex: 'none' }}
      >
        <TeamCircle sigla={time.sigla} corHex={time.corHex} tamanho={40} />
        <div className="expandir coluna">
          <strong style={{ fontSize: 17 }}>Time {time.nome}</strong>
          <span className="subtitulo">Elenco e agenda de jogos</span>
        </div>
        <button type="button" className="botao-icone" aria-label="Fechar" onClick={onFechar}>
          <IconeFechar />
        </button>
      </div>

      {elenco.length === 0 && partidas.length === 0 ? (
        <EstadoVazio
          titulo="Time vazio"
          descricao="Este time ainda não tem elenco sorteado nem jogos marcados."
        />
      ) : (
        <div className="coluna" style={{ padding: 12, gap: 10, overflowY: 'auto' }}>
          <SecaoElenco elenco={elenco} />
          <RotuloPequeno>Agenda de jogos</RotuloPequeno>
          {partidas.length === 0 && (
            <span className="subtitulo">Este time ainda não aparece em nenhuma rodada.</span>
          )}
          {partidas.map((partida) => (
            <LinhaHistorico key={partida.id} partida={partida} timeId={time.id} />
          ))}
        </div>
      )}
    </div>
  </div>
);

const SecaoElenco = ({ elenco }: { elenco: Player[] }) => (
  <Cartao>
    <div className="coluna" style={{ padding: 12, gap: 8 }}>
      <div className="linha-entre">
        <RotuloPequeno>Elenco do dia</RotuloPequeno>
        {elenco.length > 0 && (
          <span style={{ color: 'var(--verde-claro)', fontSize: 11, fontWeight: 700 }}>
            {elenco.filter((j) => j.genero === 'masculino').length}H /{' '}
            {elenco.filter((j) => j.genero === 'feminino').length}M • força{' '}
            {elenco.reduce((soma, j) => soma + j.skillLevel, 0)}
          </span>
        )}
      </div>
      {elenco.length === 0 ? (
        <span className="subtitulo">Sem elenco vinculado. Sorteie os times na aba Times.</span>
      ) : (
        elenco.map((jogador) => (
          <div key={jogador.id} className="linha">
            <span className="expandir" style={{ fontSize: 13, fontWeight: 600 }}>
              {jogador.nome}
            </span>
            <Selo
              texto={jogador.genero === 'masculino' ? 'H' : 'M'}
              corTexto="var(--selo-fase-texto)"
              corFundo="var(--selo-fase-fundo)"
            />
            <span className="terciario" style={{ fontSize: 11 }}>
              nível {jogador.skillLevel}
            </span>
          </div>
        ))
      )}
    </div>
  </Cartao>
);

const LinhaHistorico = ({ partida, timeId }: { partida: MatchCard; timeId: string }) => {
  const souOTimeA = partida.teamA.id === timeId;
  const meuTime = souOTimeA ? partida.teamA : partida.teamB;
  const adversario = souOTimeA ? partida.teamB : partida.teamA;
  const meuPlacar = souOTimeA ? partida.scoreA : partida.scoreB;
  const placarAdversario = souOTimeA ? partida.scoreB : partida.scoreA;
  const finalizada = partida.status === 'finalizado';
  const jaTemPlacar = finalizada || meuPlacar > 0 || placarAdversario > 0;

  return (
    <Cartao>
      <div className="coluna" style={{ padding: 12, gap: 10 }}>
        <div className="linha-entre">
          <RotuloPequeno>
            {`Fase ${partida.fase} • Rodada ${partida.roundNumero} • Quadra ${partida.quadra}`}
          </RotuloPequeno>
          {finalizada && <SeloResultado venceu={partida.winnerId === timeId} />}
        </div>
        <div className="linha-entre">
          <div className="linha expandir" style={{ gap: 6 }}>
            <PontoDoTime corHex={meuTime.corHex} tamanho={9} />
            <span style={{ fontSize: 13, fontWeight: 600 }}>{meuTime.nome}</span>
          </div>
          <div className="linha" style={{ gap: 6 }}>
            <ScoreBox valor={jaTemPlacar ? meuPlacar : null} />
            <span className="terciario" style={{ fontSize: 12 }}>
              x
            </span>
            <ScoreBox valor={jaTemPlacar ? placarAdversario : null} />
          </div>
          <div className="linha" style={{ gap: 6, justifyContent: 'flex-end' }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>{adversario.nome}</span>
            <PontoDoTime corHex={adversario.corHex} tamanho={9} />
          </div>
        </div>
      </div>
    </Cartao>
  );
};
