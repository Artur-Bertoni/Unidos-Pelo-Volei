import { useState } from 'react';
import type { Standing } from '../../domain/models';
import { Cartao, DialogoConfirmacao, EstadoVazio, PontoDoTime } from '../components/Componentes';
import { IconeLixeira, IconeTrofeu } from '../components/Icons';

const COLUNAS = { posicao: 32, vitorias: 36, saldo: 46, pontos: 42 };

export const ClassificacaoScreen = ({
  linhas,
  isAdmin,
  onApagarResultados,
}: {
  linhas: Standing[];
  isAdmin: boolean;
  onApagarResultados: () => void;
}) => {
  const [confirmando, setConfirmando] = useState(false);

  return (
    <div className="lista">
      <Cartao>
        <div
          className="linha"
          style={{
            justifyContent: 'center',
            background: 'var(--fundo-cabecalho)',
            padding: '14px 0',
            borderBottom: '1px solid var(--borda)',
          }}
        >
          <span style={{ color: 'var(--dourado)', display: 'flex' }}>
            <IconeTrofeu tamanho={18} />
          </span>
          <strong style={{ fontSize: 16 }}>Classificação Geral</strong>
        </div>

        <div className="linha" style={{ padding: '10px 14px', gap: 0 }}>
          <span className="rotulo-pequeno" style={{ width: COLUNAS.posicao, textAlign: 'center' }}>
            #
          </span>
          <span className="rotulo-pequeno expandir">TIME</span>
          <span className="rotulo-pequeno" style={{ width: COLUNAS.vitorias, textAlign: 'center' }}>
            V
          </span>
          <span className="rotulo-pequeno" style={{ width: COLUNAS.saldo, textAlign: 'center' }}>
            S
          </span>
          <span className="rotulo-pequeno" style={{ width: COLUNAS.pontos, textAlign: 'center' }}>
            PP
          </span>
        </div>

        {linhas.length === 0 ? (
          <EstadoVazio
            titulo="Sem classificação"
            descricao="Cadastre os times e finalize partidas para ver a tabela."
          />
        ) : (
          linhas.map((linha, indice) => (
            <LinhaClassificacao
              key={linha.teamId}
              posicao={indice + 1}
              linha={linha}
              ultima={indice === linhas.length - 1}
            />
          ))
        )}

        <p
          className="terciario"
          style={{
            fontSize: 11,
            textAlign: 'center',
            padding: 12,
            margin: 0,
            borderTop: '1px solid var(--borda)',
          }}
        >
          Critérios: 1. Vitórias (V) &nbsp;|&nbsp; 2. Saldo de Pontos (S) &nbsp;|&nbsp; 3. Pontos Pró (PP)
        </p>
      </Cartao>

      {isAdmin && linhas.length > 0 && (
        <button
          type="button"
          className="linha terciario"
          style={{ justifyContent: 'center', padding: 12, fontSize: 13 }}
          onClick={() => setConfirmando(true)}
        >
          <IconeLixeira />
          Apagar todos os resultados
        </button>
      )}

      {confirmando && (
        <DialogoConfirmacao
          titulo="Apagar todos os resultados?"
          mensagem="Os placares voltam a zero e as partidas voltam para agendadas. O chaveamento é mantido."
          textoConfirmar="Apagar"
          onConfirmar={() => {
            setConfirmando(false);
            onApagarResultados();
          }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
};

const LinhaClassificacao = ({
  posicao,
  linha,
  ultima,
}: {
  posicao: number;
  linha: Standing;
  ultima: boolean;
}) => {
  const destaque =
    posicao === 1
      ? 'rgba(245, 197, 24, 0.07)'
      : posicao <= 3
        ? 'rgba(245, 197, 24, 0.035)'
        : 'transparent';

  return (
    <div
      className="linha"
      style={{
        background: destaque,
        padding: '12px 14px',
        gap: 0,
        borderBottom: ultima ? 'none' : '1px solid rgba(38, 45, 58, 0.5)',
      }}
    >
      <span
        style={{
          width: COLUNAS.posicao,
          textAlign: 'center',
          fontSize: 12,
          fontWeight: 700,
          color: posicao <= 3 ? 'var(--dourado)' : 'var(--texto-secundario)',
        }}
      >
        {posicao}º
      </span>
      <div className="linha expandir" style={{ gap: 8 }}>
        <PontoDoTime corHex={linha.corHex} />
        <span
          style={{
            fontSize: 14,
            fontWeight: 600,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {linha.nome}
        </span>
      </div>
      <span style={{ width: COLUNAS.vitorias, textAlign: 'center', fontSize: 14, fontWeight: 700 }}>
        {linha.vitorias}
      </span>
      <span
        className="secundario"
        style={{ width: COLUNAS.saldo, textAlign: 'center', fontSize: 13 }}
      >
        {linha.saldoPontos > 0 ? `+${linha.saldoPontos}` : linha.saldoPontos}
      </span>
      <span
        className="terciario"
        style={{ width: COLUNAS.pontos, textAlign: 'center', fontSize: 13 }}
      >
        {linha.pontosPro}
      </span>
    </div>
  );
};
