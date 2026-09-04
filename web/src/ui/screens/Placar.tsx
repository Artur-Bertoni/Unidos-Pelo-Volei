import { useState } from 'react';
import { PLACAR_MAXIMO } from '../../data/matches';
import type { MatchCard, Team } from '../../domain/models';
import {
  BotaoRedondo,
  Carregando,
  Cartao,
  RotuloPequeno,
  Selo,
  TeamCircle,
} from '../components/Componentes';
import { IconeMais, IconeMenos, IconeVoltar } from '../components/Icons';

export const PlacarScreen = ({
  partida,
  isAdmin,
  salvando,
  erro,
  onVoltar,
  onSomar,
  onDefinirPlacar,
  onFinalizar,
  onReabrir,
}: {
  partida: MatchCard | null;
  isAdmin: boolean;
  salvando: boolean;
  erro: string | null;
  onVoltar: () => void;
  onSomar: (ladoA: boolean, delta: number) => void;
  onDefinirPlacar: (ladoA: boolean, valor: number) => void;
  onFinalizar: () => void;
  onReabrir: () => void;
}) => {
  const editavel = isAdmin && partida?.status === 'agendado';

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8 }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <span className="titulo-tela">Placar</span>
      </div>

      {!partida ? (
        <Carregando />
      ) : (
        <div className="coluna" style={{ padding: 16, gap: 16, alignItems: 'center' }}>
          <RotuloPequeno>
            {`Rodada ${partida.roundNumero} • Quadra ${partida.quadra}`}
          </RotuloPequeno>

          {partida.status === 'finalizado' && (
            <Selo texto="Finalizada" corTexto="var(--verde-claro)" corFundo="var(--selo-vitoria-fundo)" />
          )}

          <div className="linha" style={{ width: '100%', gap: 12, alignItems: 'stretch' }}>
            <PainelDeTime
              time={partida.teamA}
              placar={partida.scoreA}
              vencedor={partida.winnerId === partida.teamA.id}
              editavel={editavel}
              onSomar={(delta) => onSomar(true, delta)}
              onDefinir={(valor) => onDefinirPlacar(true, valor)}
            />
            <PainelDeTime
              time={partida.teamB}
              placar={partida.scoreB}
              vencedor={partida.winnerId === partida.teamB.id}
              editavel={editavel}
              onSomar={(delta) => onSomar(false, delta)}
              onDefinir={(valor) => onDefinirPlacar(false, valor)}
            />
          </div>

          {!isAdmin && (
            <p className="terciario" style={{ fontSize: 12, margin: 0 }}>
              Somente administradores registram placar.
            </p>
          )}

          {isAdmin && partida.status === 'agendado' && (
            <>
              <p className="terciario" style={{ fontSize: 12, textAlign: 'center', margin: 0 }}>
                Toque no número para digitar o resultado, ou use + e - para marcar ponto a ponto.
              </p>
              <button
                type="button"
                className="botao botao-primario"
                disabled={salvando}
                onClick={onFinalizar}
              >
                Finalizar partida
              </button>
            </>
          )}

          {isAdmin && partida.status === 'finalizado' && (
            <button
              type="button"
              className="botao botao-contorno"
              disabled={salvando}
              onClick={onReabrir}
            >
              Reabrir partida
            </button>
          )}

          {erro && <p className="erro">{erro}</p>}
        </div>
      )}
    </div>
  );
};

const PainelDeTime = ({
  time,
  placar,
  vencedor,
  editavel,
  onSomar,
  onDefinir,
}: {
  time: Team;
  placar: number;
  vencedor: boolean;
  editavel: boolean;
  onSomar: (delta: number) => void;
  onDefinir: (valor: number) => void;
}) => {
  const cor = vencedor ? 'var(--verde-claro)' : 'var(--texto-primario)';

  return (
    <div className="expandir">
      <Cartao>
        <div className="coluna" style={{ alignItems: 'center', gap: 12, padding: '20px 8px' }}>
          <TeamCircle sigla={time.sigla} corHex={time.corHex} tamanho={60} />
          <span
            style={{
              fontSize: 11,
              fontWeight: 700,
              letterSpacing: 0.6,
              color: 'var(--texto-secundario)',
              textAlign: 'center',
            }}
          >
            {time.nome.toUpperCase()}
          </span>

          {editavel ? (
            <>
              <CampoDePlacar placar={placar} cor={cor} nomeDoTime={time.nome} onDefinir={onDefinir} />
              <div className="linha" style={{ gap: 16 }}>
                <BotaoRedondo
                  descricao="Menos um ponto"
                  habilitado={placar > 0}
                  tamanho={44}
                  onClick={() => onSomar(-1)}
                >
                  <IconeMenos />
                </BotaoRedondo>
                <BotaoRedondo
                  descricao="Mais um ponto"
                  tamanho={44}
                  cor="var(--verde)"
                  onClick={() => onSomar(1)}
                >
                  <IconeMais />
                </BotaoRedondo>
              </div>
            </>
          ) : (
            <>
              <span style={{ fontSize: 52, fontWeight: 900, color: cor, lineHeight: 1.1 }}>{placar}</span>
              <div style={{ height: 44 }} />
            </>
          )}
        </div>
      </Cartao>
    </div>
  );
};

const CampoDePlacar = ({
  placar,
  cor,
  nomeDoTime,
  onDefinir,
}: {
  placar: number;
  cor: string;
  nomeDoTime: string;
  onDefinir: (valor: number) => void;
}) => {
  const [digitado, setDigitado] = useState<string | null>(null);
  const texto = digitado ?? String(placar);

  return (
    <input
      inputMode="numeric"
      value={texto}
      aria-label={`Placar do time ${nomeDoTime}`}
      onFocus={() => setDigitado(String(placar))}
      onBlur={() => setDigitado(null)}
      onChange={(e) => {
        const limpo = e.target.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '').slice(0, 3);
        setDigitado(limpo);
        onDefinir(Math.min(Number(limpo) || 0, PLACAR_MAXIMO));
      }}
      style={{
        width: 110,
        background: 'none',
        border: 'none',
        textAlign: 'center',
        fontSize: 52,
        fontWeight: 900,
        color: cor,
        padding: 0,
      }}
    />
  );
};
