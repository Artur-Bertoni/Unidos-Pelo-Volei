import { useMemo, useState } from 'react';
import {
  FUNDAMENTOS,
  fundamentoMaisFracoDe,
  notasIniciais,
  PERIODOS,
  pontosNoPeriodo,
  rotuloDoFundamento,
  rotuloDoPeriodo,
  variacaoNoPeriodo,
  type AvaliacaoPendente,
  type Dica,
  type Fundamento,
  type NotasPorFundamento,
  type PeriodoDoGrafico,
  type Player,
  type PontoDaNota,
} from '../../domain/models';
import { Cartao, Estrelas, EstadoVazio, RotuloPequeno } from '../components/Componentes';
import { IconeVoltar } from '../components/Icons';

type Serie = 'media' | Fundamento;

const LARGURA = 320;
const ALTURA = 160;
const MARGEM = { topo: 12, direita: 10, baixo: 22, esquerda: 28 };

const AREA_LARGURA = LARGURA - MARGEM.esquerda - MARGEM.direita;
const AREA_ALTURA = ALTURA - MARGEM.topo - MARGEM.baixo;

const valorDaSerie = (ponto: PontoDaNota, serie: Serie): number =>
  serie === 'media' ? ponto.media : ponto.notas[serie];

const rotuloDaSerie = (serie: Serie): string =>
  serie === 'media' ? 'Média geral' : rotuloDoFundamento(serie);

const diaEMes = (iso: string): string => {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '';
  return `${String(data.getDate()).padStart(2, '0')}/${String(data.getMonth() + 1).padStart(2, '0')}`;
};

const comSinal = (valor: number): string =>
  `${valor > 0 ? '+' : valor < 0 ? '−' : ''}${Math.abs(valor).toFixed(2).replace('.', ',')}`;

const GraficoDaNota = ({
  pontos,
  serie,
  selecionado,
  onSelecionar,
}: {
  pontos: PontoDaNota[];
  serie: Serie;
  selecionado: number | null;
  onSelecionar: (indice: number | null) => void;
}) => {
  const posicaoX = (indice: number): number =>
    pontos.length === 1
      ? MARGEM.esquerda + AREA_LARGURA / 2
      : MARGEM.esquerda + (indice / (pontos.length - 1)) * AREA_LARGURA;

  const posicaoY = (valor: number): number =>
    MARGEM.topo + ((5 - Math.min(5, Math.max(1, valor))) / 4) * AREA_ALTURA;

  const caminho = pontos
    .map((ponto, indice) => `${posicaoX(indice)},${posicaoY(valorDaSerie(ponto, serie))}`)
    .join(' ');

  const marcarPontos = pontos.length <= 26;
  const primeiro = pontos[0];
  const ultimo = pontos.at(-1) as PontoDaNota;

  return (
    <svg
      viewBox={`0 0 ${LARGURA} ${ALTURA}`}
      style={{ width: '100%', height: 'auto', display: 'block', touchAction: 'manipulation' }}
      role="img"
      aria-label={`${rotuloDaSerie(serie)} de ${valorDaSerie(primeiro, serie).toFixed(2)} a ${valorDaSerie(ultimo, serie).toFixed(2)} em ${pontos.length} pontos`}
    >
      {[1, 2, 3, 4, 5].map((linha) => (
        <g key={linha}>
          <line
            x1={MARGEM.esquerda}
            x2={LARGURA - MARGEM.direita}
            y1={posicaoY(linha)}
            y2={posicaoY(linha)}
            stroke="var(--borda)"
            strokeWidth={1}
          />
          <text
            x={MARGEM.esquerda - 6}
            y={posicaoY(linha) + 3}
            textAnchor="end"
            fontSize={9}
            fill="var(--texto-terciario)"
          >
            {linha}
          </text>
        </g>
      ))}

      <polyline
        points={caminho}
        fill="none"
        stroke="var(--verde-claro)"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {marcarPontos &&
        pontos.map((ponto, indice) => {
          const x = posicaoX(indice);
          const y = posicaoY(valorDaSerie(ponto, serie));
          const daDiretoria = ponto.origem === 'diretoria';
          return (
            <g key={ponto.id}>
              <title>
                {`${diaEMes(ponto.registradoEm)} • ${valorDaSerie(ponto, serie).toFixed(2)}`}
              </title>
              <circle
                cx={x}
                cy={y}
                r={indice === selecionado ? 5 : 4}
                fill={daDiretoria ? 'var(--dourado)' : 'var(--verde-claro)'}
                stroke="var(--cartao)"
                strokeWidth={2}
              />
              <circle
                cx={x}
                cy={y}
                r={11}
                fill="transparent"
                style={{ cursor: 'pointer' }}
                onClick={() => onSelecionar(indice === selecionado ? null : indice)}
              />
            </g>
          );
        })}

      <text
        x={MARGEM.esquerda}
        y={ALTURA - 6}
        textAnchor="start"
        fontSize={9}
        fill="var(--texto-terciario)"
      >
        {diaEMes(primeiro.registradoEm)}
      </text>
      {pontos.length > 1 && (
        <text
          x={LARGURA - MARGEM.direita}
          y={ALTURA - 6}
          textAnchor="end"
          fontSize={9}
          fill="var(--texto-terciario)"
        >
          {diaEMes(ultimo.registradoEm)}
        </text>
      )}
    </svg>
  );
};

export function CartaoDaEvolucao({
  jogador,
  historico,
  totalAvaliacoes,
  dicas,
}: {
  jogador: Player | null;
  historico: PontoDaNota[];
  totalAvaliacoes: number;
  dicas: Dica[];
}) {
  const [periodo, setPeriodo] = useState<PeriodoDoGrafico>('ano');
  const [serie, setSerie] = useState<Serie>('media');
  const [selecionado, setSelecionado] = useState<number | null>(null);

  const pontos = useMemo(() => pontosNoPeriodo(historico, periodo), [historico, periodo]);
  const variacao = useMemo(() => variacaoNoPeriodo(pontos), [pontos]);
  const movida = historico.some((ponto) => ponto.origem === 'avaliacao');

  const notas = jogador?.notas ?? notasIniciais();
  const fraco = fundamentoMaisFracoDe(notas);
  const dicaDoFraco =
    dicas
      .filter((dica) => dica.fundamento === fraco)
      .sort((a, b) => a.faixaMax - b.faixaMax)
      .find((dica) => notas[fraco] <= dica.faixaMax) ??
    dicas.find((dica) => dica.fundamento === fraco);

  const escolhido = selecionado === null ? null : (pontos[selecionado] ?? null);

  return (
    <>
      <Cartao>
        <div className="coluna" style={{ padding: 16, gap: 12 }}>
          <div className="linha-entre">
            <RotuloPequeno>Minha nota</RotuloPequeno>
            <div className="linha" style={{ gap: 6 }}>
              <Estrelas nivel={jogador?.media ?? 3} />
              <strong style={{ fontSize: 13 }}>{(jogador?.media ?? 3).toFixed(1)}</strong>
            </div>
          </div>

          {jogador === null ? (
            <span className="subtitulo" style={{ fontSize: 13 }}>
              A nota aparece aqui assim que a diretoria ligar a sua conta a um jogador.
            </span>
          ) : (
            <>
              {FUNDAMENTOS.map((fundamento) => {
                const valor = notas[fundamento];
                const delta = variacao[fundamento] ?? 0;
                return (
                  <button
                    key={fundamento}
                    type="button"
                    className="coluna"
                    style={{ gap: 4, textAlign: 'left', padding: 0, width: '100%' }}
                    aria-pressed={serie === fundamento}
                    onClick={() => {
                      setSerie(serie === fundamento ? 'media' : fundamento);
                      setSelecionado(null);
                    }}
                  >
                    <div className="linha-entre">
                      <span className="subtitulo" style={{ fontSize: 12 }}>
                        {rotuloDoFundamento(fundamento)}
                      </span>
                      <div className="linha" style={{ gap: 8 }}>
                        {delta !== 0 && (
                          <span
                            style={{
                              fontSize: 11,
                              color: delta > 0 ? 'var(--verde-claro)' : 'var(--vermelho)',
                            }}
                          >
                            {comSinal(delta)}
                          </span>
                        )}
                        <strong style={{ fontSize: 12 }}>{valor.toFixed(2).replace('.', ',')}</strong>
                      </div>
                    </div>
                    <div className="barra-evolucao">
                      <div
                        className="barra-evolucao-preenchida"
                        style={{
                          width: `${Math.min(Math.max(valor / 5, 0), 1) * 100}%`,
                          background: valor >= 3.5 ? 'var(--verde)' : 'var(--dourado)',
                        }}
                      />
                    </div>
                  </button>
                );
              })}

              <div className="linha" style={{ gap: 6, flexWrap: 'wrap', paddingTop: 4 }}>
                {PERIODOS.map((opcao) => (
                  <button
                    key={opcao}
                    type="button"
                    className="chip"
                    aria-pressed={periodo === opcao}
                    onClick={() => {
                      setPeriodo(opcao);
                      setSelecionado(null);
                    }}
                  >
                    {rotuloDoPeriodo(opcao)}
                  </button>
                ))}
              </div>

              {pontos.length < 2 ? (
                <span className="subtitulo" style={{ fontSize: 12 }}>
                  {movida
                    ? 'Nenhum ponto neste período. Escolha um período maior.'
                    : 'O gráfico começa a desenhar quando o primeiro sábado avaliado entrar na conta.'}
                </span>
              ) : (
                <>
                  <span className="subtitulo" style={{ fontSize: 12 }}>
                    {rotuloDaSerie(serie)} • toque numa barra acima para trocar a linha
                  </span>
                  <GraficoDaNota
                    pontos={pontos}
                    serie={serie}
                    selecionado={selecionado}
                    onSelecionar={setSelecionado}
                  />
                  <span className="subtitulo" style={{ fontSize: 11 }}>
                    {escolhido
                      ? `${diaEMes(escolhido.registradoEm)} • ${rotuloDaSerie(serie)} ${valorDaSerie(escolhido, serie).toFixed(2).replace('.', ',')}${escolhido.origem === 'diretoria' ? ' • ajuste da diretoria' : ` • ${escolhido.avaliadores} avaliações naquele dia`}`
                      : 'Ponto dourado é ajuste da diretoria. Ponto verde é sábado avaliado.'}
                  </span>
                </>
              )}

              {dicaDoFraco && (
                <div className="dica">
                  <RotuloPequeno>
                    Para treinar: {rotuloDoFundamento(dicaDoFraco.fundamento)}
                  </RotuloPequeno>
                  <strong style={{ fontSize: 14 }}>{dicaDoFraco.titulo}</strong>
                  <span className="subtitulo" style={{ fontSize: 12 }}>
                    {dicaDoFraco.texto}
                  </span>
                </div>
              )}

              <span className="subtitulo" style={{ fontSize: 11 }}>
                {totalAvaliacoes === 0
                  ? 'Você ainda não recebeu avaliações.'
                  : `${totalAvaliacoes} avaliações recebidas até agora.`}{' '}
                Cada sábado com pelo menos duas notas empurra a sua em até 0,25 na direção do que o
                time achou.
              </span>
            </>
          )}
        </div>
      </Cartao>
    </>
  );
}

export function AvaliacaoScreen({
  pendentes,
  salvando,
  onVoltar,
  onEnviar,
}: {
  pendentes: AvaliacaoPendente[];
  salvando: boolean;
  onVoltar: () => void;
  onEnviar: (pendente: AvaliacaoPendente, notas: NotasPorFundamento) => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Avaliar companheiros</span>
          <span className="subtitulo">Anônimo, e mexe pouco a pouco na nota de cada um</span>
        </div>
      </div>

      <div className="conteudo">
        {pendentes.length === 0 ? (
          <EstadoVazio
            titulo="Nada para avaliar"
            descricao="Depois do próximo sábado encerrado, os seus companheiros de time aparecem aqui."
          />
        ) : (
          <div className="lista" style={{ padding: 16, gap: 12 }}>
            {pendentes.map((pendente) => (
              <FichaDeAvaliacao
                key={`${pendente.dayId}-${pendente.avaliadoPlayerId}`}
                pendente={pendente}
                salvando={salvando}
                onEnviar={(notas) => onEnviar(pendente, notas)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function FichaDeAvaliacao({
  pendente,
  salvando,
  onEnviar,
}: {
  pendente: AvaliacaoPendente;
  salvando: boolean;
  onEnviar: (notas: NotasPorFundamento) => void;
}) {
  const [notas, setNotas] = useState<NotasPorFundamento>(notasIniciais());

  return (
    <Cartao>
      <div className="coluna" style={{ padding: 16, gap: 12 }}>
        <strong style={{ fontSize: 17 }}>{pendente.avaliadoNome}</strong>

        {FUNDAMENTOS.map((fundamento) => (
          <div key={fundamento} className="linha-entre">
            <span className="subtitulo" style={{ fontSize: 13 }}>
              {rotuloDoFundamento(fundamento)}
            </span>
            <div className="linha" style={{ gap: 4 }}>
              {[1, 2, 3, 4, 5].map((nota) => (
                <button
                  key={nota}
                  type="button"
                  className="nota"
                  aria-pressed={notas[fundamento] === nota}
                  onClick={() => setNotas({ ...notas, [fundamento]: nota })}
                >
                  {nota}
                </button>
              ))}
            </div>
          </div>
        ))}

        <button
          type="button"
          className="botao botao-primario"
          disabled={salvando}
          onClick={() => onEnviar(notas)}
        >
          Enviar avaliação
        </button>
      </div>
    </Cartao>
  );
}
