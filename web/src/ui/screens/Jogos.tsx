import { useState } from 'react';
import type { MatchCard, RoundSchedule } from '../../domain/models';
import {
  Cartao,
  Contador,
  DialogoConfirmacao,
  EstadoVazio,
  ScoreBox,
  Selo,
  SeloFase,
  TeamBadge,
} from '../components/Componentes';
import { IconeCama, IconeDiaEncerrado, IconeExpandir } from '../components/Icons';

export const listarNomes = (nomes: string[]): string => {
  if (nomes.length === 0) return '';
  if (nomes.length === 1) return nomes[0];
  return `${nomes.slice(0, -1).join(', ')} e ${nomes[nomes.length - 1]}`;
};

export const JogosScreen = ({
  rodadas,
  carregando,
  isAdmin,
  gerando,
  encerrando,
  temElencos,
  presentes,
  onAbrirPartida,
  onGerarChaveamento,
  onEncerrarDia,
}: {
  rodadas: RoundSchedule[];
  carregando: boolean;
  isAdmin: boolean;
  gerando: boolean;
  encerrando: boolean;
  temElencos: boolean;
  presentes: number;
  onAbrirPartida: (matchId: string) => void;
  onGerarChaveamento: (quadras: number) => void;
  onEncerrarDia: () => void;
}) => {
  const [expandida, setExpandida] = useState<number | null>(1);

  return (
    <div className="lista">
      {isAdmin && (
        <>
          <CartaoChaveamento
            gerando={gerando}
            temChaveamento={rodadas.length > 0}
            onGerar={onGerarChaveamento}
          />
          <CartaoEncerrarDia
            encerrando={encerrando}
            temElencos={temElencos}
            temChaveamento={rodadas.length > 0}
            temPresentes={presentes > 0}
            onEncerrar={onEncerrarDia}
          />
        </>
      )}

      {rodadas.length === 0 && !carregando && (
        <EstadoVazio
          titulo="Nenhuma rodada ainda"
          descricao={
            isAdmin
              ? 'Cadastre os times e gere o chaveamento para começar.'
              : 'O chaveamento ainda não foi gerado por um administrador.'
          }
        />
      )}

      {rodadas.map((rodada) => (
        <CartaoRodada
          key={rodada.round.id}
          rodada={rodada}
          expandida={expandida === rodada.round.numero}
          onAlternar={() =>
            setExpandida(expandida === rodada.round.numero ? null : rodada.round.numero)
          }
          onAbrirPartida={onAbrirPartida}
        />
      ))}
    </div>
  );
};

const CartaoChaveamento = ({
  gerando,
  temChaveamento,
  onGerar,
}: {
  gerando: boolean;
  temChaveamento: boolean;
  onGerar: (quadras: number) => void;
}) => {
  const [quadras, setQuadras] = useState(3);
  const [confirmando, setConfirmando] = useState(false);

  return (
    <>
      <Cartao>
        <div className="coluna" style={{ padding: 16, gap: 12 }}>
          <strong style={{ fontSize: 15 }}>Chaveamento</strong>
          <p className="subtitulo" style={{ margin: 0 }}>
            Cada quadra recebe um trio, que joga entre si nas três rodadas da fase (A×B, A×C, B×C).
            Depois os times trocam de quadra até todos jogarem contra todos, e ninguém joga mais de
            duas partidas seguidas.
          </p>
          <div className="linha-entre">
            <span className="subtitulo" style={{ fontSize: 13 }}>
              Quadras
            </span>
            <Contador valor={quadras} minimo={1} maximo={12} onMudar={setQuadras} />
          </div>
          <button
            type="button"
            className="botao botao-primario"
            disabled={gerando}
            onClick={() => (temChaveamento ? setConfirmando(true) : onGerar(quadras))}
          >
            {gerando ? 'Gerando...' : 'Gerar chaveamento'}
          </button>
        </div>
      </Cartao>

      {confirmando && (
        <DialogoConfirmacao
          titulo="Gerar novo chaveamento?"
          mensagem="As rodadas e os placares atuais serão apagados e substituídos."
          textoConfirmar="Gerar"
          onConfirmar={() => {
            setConfirmando(false);
            onGerar(quadras);
          }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </>
  );
};

const CartaoEncerrarDia = ({
  encerrando,
  temElencos,
  temChaveamento,
  temPresentes,
  onEncerrar,
}: {
  encerrando: boolean;
  temElencos: boolean;
  temChaveamento: boolean;
  temPresentes: boolean;
  onEncerrar: () => void;
}) => {
  const [confirmando, setConfirmando] = useState(false);
  const temAlgoParaEncerrar = temElencos || temChaveamento || temPresentes;

  return (
    <>
      <Cartao>
        <div className="coluna" style={{ padding: 16, gap: 12 }}>
          <strong style={{ fontSize: 15 }}>Fim do dia</strong>
          <p className="subtitulo" style={{ margin: 0 }}>
            {temAlgoParaEncerrar
              ? 'Guarda os resultados e a presença de quem veio, desfaz os times, apaga o chaveamento e zera as presenças. O próximo sorteio usa esse histórico para evitar repetir os mesmos companheiros.'
              : 'Nada a encerrar: ninguém presente, nenhum time montado e nenhum chaveamento.'}
          </p>
          <button
            type="button"
            className="botao botao-contorno"
            disabled={encerrando || !temAlgoParaEncerrar}
            onClick={() => setConfirmando(true)}
          >
            <span style={{ color: temAlgoParaEncerrar ? 'var(--dourado)' : 'var(--texto-terciario)', display: 'flex' }}>
              <IconeDiaEncerrado />
            </span>
            {encerrando ? 'Encerrando...' : 'Encerrar dia'}
          </button>
        </div>
      </Cartao>

      {confirmando && (
        <DialogoConfirmacao
          titulo="Encerrar o dia?"
          mensagem="Os resultados das partidas finalizadas vão para o desempenho dos atletas, e todo mundo que estava presente ganha mais um dia no histórico. Depois disso os times ficam sem elenco, o chaveamento é apagado e as presenças são zeradas para o próximo sábado. Não dá para desfazer."
          textoConfirmar="Encerrar dia"
          onConfirmar={() => {
            setConfirmando(false);
            onEncerrar();
          }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </>
  );
};

const CartaoRodada = ({
  rodada,
  expandida,
  onAlternar,
  onAbrirPartida,
}: {
  rodada: RoundSchedule;
  expandida: boolean;
  onAlternar: () => void;
  onAbrirPartida: (matchId: string) => void;
}) => (
  <Cartao>
    <button
      type="button"
      className="linha"
      style={{ width: '100%', padding: 16, textAlign: 'left' }}
      aria-expanded={expandida}
      onClick={onAlternar}
    >
      <div className="expandir coluna" style={{ gap: 6 }}>
        <div className="linha">
          <span style={{ fontSize: 17, fontWeight: 900, letterSpacing: 0.5 }}>
            RODADA {rodada.round.numero}
          </span>
          <SeloFase fase={rodada.round.fase} />
        </div>
        {rodada.folgam.length > 0 && (
          <div className="linha" style={{ gap: 6 }}>
            <span className="terciario" style={{ display: 'flex' }}>
              <IconeCama />
            </span>
            <span className="subtitulo" style={{ fontWeight: 600 }}>
              Folgam: {listarNomes(rodada.folgam.map((time) => time.nome))}
            </span>
          </div>
        )}
      </div>
      <span className="secundario" style={{ display: 'flex' }}>
        <IconeExpandir aberto={expandida} />
      </span>
    </button>

    {expandida && (
      <div
        className="coluna"
        style={{ background: 'var(--cartao-interno)', padding: 12, gap: 10 }}
      >
        {rodada.matches.map((partida) => (
          <CartaoPartida
            key={partida.id}
            partida={partida}
            onClick={() => onAbrirPartida(partida.id)}
          />
        ))}
      </div>
    )}
  </Cartao>
);

export const CartaoPartida = ({
  partida,
  onClick,
}: {
  partida: MatchCard;
  onClick: () => void;
}) => {
  const finalizada = partida.status === 'finalizado';
  const jaTemPlacar = finalizada || partida.scoreA > 0 || partida.scoreB > 0;

  return (
    <Cartao onClick={onClick}>
      <div className="coluna" style={{ alignItems: 'center', gap: 8, padding: '10px 0' }}>
        <Selo texto={`Quadra ${partida.quadra}`} corTexto="var(--texto-secundario)" corFundo="var(--fundo)" />
        <div className="linha-entre" style={{ width: '100%', padding: '0 12px' }}>
          <TeamBadge team={partida.teamA} />
          <div className="linha">
            <ScoreBox
              valor={jaTemPlacar ? partida.scoreA : null}
              destacado={finalizada && partida.winnerId === partida.teamA.id}
            />
            <span className="terciario" style={{ fontSize: 13 }}>
              x
            </span>
            <ScoreBox
              valor={jaTemPlacar ? partida.scoreB : null}
              destacado={finalizada && partida.winnerId === partida.teamB.id}
            />
          </div>
          <TeamBadge team={partida.teamB} />
        </div>
      </div>
    </Cartao>
  );
};
