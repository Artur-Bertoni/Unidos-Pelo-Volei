import { useState } from 'react';
import {
  FUNDAMENTOS,
  MINIMO_DE_AVALIACOES,
  notasIniciais,
  rotuloDoFundamento,
  rotuloDoPagamento,
  STATUS_DE_PAGAMENTO,
  evolucaoLiberada,
  fundamentoMaisFraco,
  type AvaliacaoPendente,
  type ConfigFinanceiro,
  type Dica,
  type Evolucao,
  type ItemDoExtrato,
  type NotasDaAvaliacao,
  type StatusPagamento,
} from '../../domain/models';
import { gerarBrCode } from '../../domain/pix';
import { CampoTexto, Cartao, Dialogo, EstadoVazio, RotuloPequeno, Selo } from '../components/Componentes';
import { IconeVoltar } from '../components/Icons';

export const reais = (centavos: number): string =>
  `R$ ${Math.trunc(centavos / 100)},${String(centavos % 100).padStart(2, '0')}`;

export const emCentavos = (texto: string): number => {
  const limpo = texto.replace(/[^0-9,.]/g, '').replace(',', '.');
  const valor = Number(limpo);
  return Number.isFinite(valor) ? Math.round(valor * 100) : 0;
};

const SeloDoStatus = ({ status }: { status: StatusPagamento }) => {
  const cores: Record<StatusPagamento, [string, string]> = {
    pago: ['var(--verde-claro)', 'var(--selo-vitoria-fundo)'],
    pendente: ['var(--dourado)', 'var(--cartao)'],
    isento: ['var(--texto-terciario)', 'var(--cartao)'],
  };
  const [corTexto, corFundo] = cores[status];
  return <Selo texto={rotuloDoPagamento(status)} corTexto={corTexto} corFundo={corFundo} />;
};

export function CartaoDoExtrato({
  extrato,
  config,
  isAdmin,
  onAbrirPainel,
}: {
  extrato: ItemDoExtrato[];
  config: ConfigFinanceiro | null;
  isAdmin: boolean;
  onAbrirPainel: () => void;
}) {
  const [copiado, setCopiado] = useState(false);
  const emAberto = extrato
    .filter((item) => item.pagamento.status === 'pendente')
    .reduce((soma, item) => soma + item.pagamento.valorCentavos, 0);

  const codigo =
    config?.pixChave && emAberto > 0
      ? gerarBrCode(config.pixChave, config.pixNome ?? '', config.pixCidade ?? '', emAberto)
      : null;

  return (
    <Cartao>
      <div className="coluna" style={{ padding: 16, gap: 12 }}>
        <div className="linha-entre">
          <RotuloPequeno>Meu financeiro</RotuloPequeno>
          {isAdmin && (
            <button type="button" className="botao-texto" onClick={onAbrirPainel}>
              Ver do grupo
            </button>
          )}
        </div>

        <strong
          style={{ fontSize: 24, color: emAberto > 0 ? 'var(--dourado)' : 'var(--verde-claro)' }}
        >
          {emAberto > 0 ? reais(emAberto) : 'Tudo em dia'}
        </strong>
        <span className="subtitulo" style={{ fontSize: 12 }}>
          {emAberto > 0 ? 'em aberto' : 'nenhuma cobrança pendente'}
        </span>

        {emAberto > 0 && codigo !== null && (
          <button
            type="button"
            className="botao botao-primario"
            onClick={() => {
              void navigator.clipboard.writeText(codigo).then(() => setCopiado(true));
            }}
          >
            {copiado ? 'Pix copiado' : `Copiar Pix de ${reais(emAberto)}`}
          </button>
        )}
        {emAberto > 0 && codigo === null && (
          <span className="subtitulo" style={{ fontSize: 11 }}>
            A diretoria ainda não cadastrou a chave Pix.
          </span>
        )}

        {extrato.slice(0, 6).map((item) => (
          <div key={item.pagamento.id} className="linha-entre">
            <div className="coluna">
              <span style={{ fontSize: 13 }}>{item.cobranca?.titulo ?? 'Cobrança'}</span>
              <span className="subtitulo" style={{ fontSize: 11 }}>
                {reais(item.pagamento.valorCentavos)}
              </span>
            </div>
            <SeloDoStatus status={item.pagamento.status} />
          </div>
        ))}

        {extrato.length === 0 && (
          <span className="subtitulo" style={{ fontSize: 12 }}>
            Nenhuma cobrança lançada para você ainda.
          </span>
        )}
      </div>
    </Cartao>
  );
}

export interface LinhaDoPainel {
  pagamentoId: string;
  nome: string;
  cobranca: string;
  valorCentavos: number;
  status: StatusPagamento;
}

export function PainelFinanceiroScreen({
  linhas,
  carregando,
  salvando,
  onVoltar,
  onRecarregar,
  onDefinirStatus,
  onGerarMensalidade,
  onGerarDiaria,
  onConfigurar,
}: {
  linhas: LinhaDoPainel[];
  carregando: boolean;
  salvando: boolean;
  onVoltar: () => void;
  onRecarregar: () => void;
  onDefinirStatus: (pagamentoId: string, status: StatusPagamento) => void;
  onGerarMensalidade: () => void;
  onGerarDiaria: () => void;
  onConfigurar: () => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna expandir">
          <span className="titulo-tela">Financeiro do grupo</span>
          <span className="subtitulo">
            {carregando ? 'Carregando do servidor...' : 'Precisa de internet'}
          </span>
        </div>
        <button type="button" className="botao-texto" onClick={onRecarregar}>
          Recarregar
        </button>
      </div>

      <div className="conteudo">
        <div className="lista" style={{ padding: 16, gap: 10 }}>
          <button type="button" className="botao botao-primario" onClick={onGerarMensalidade}>
            Gerar mensalidade deste mês
          </button>
          <button type="button" className="botao botao-primario" onClick={onGerarDiaria}>
            Gerar diária de hoje
          </button>
          <button type="button" className="botao botao-contorno" onClick={onConfigurar}>
            Chave Pix e valores
          </button>

          {linhas.length === 0 && (
            <EstadoVazio
              titulo="Nada lançado ainda"
              descricao="Gere a mensalidade do mês ou a diária do sábado para começar."
            />
          )}

          {linhas.map((linha) => (
            <Cartao key={linha.pagamentoId} apagado>
              <div className="coluna" style={{ padding: '10px 14px', gap: 8 }}>
                <div className="linha">
                  <div className="coluna expandir">
                    <span>{linha.nome}</span>
                    <span className="subtitulo" style={{ fontSize: 11 }}>
                      {linha.cobranca} · {reais(linha.valorCentavos)}
                    </span>
                  </div>
                  <SeloDoStatus status={linha.status} />
                </div>
                <div className="linha" style={{ gap: 6 }}>
                  {STATUS_DE_PAGAMENTO.map((opcao) => (
                    <button
                      key={opcao}
                      type="button"
                      className="chip"
                      aria-pressed={linha.status === opcao}
                      disabled={salvando}
                      onClick={() => onDefinirStatus(linha.pagamentoId, opcao)}
                    >
                      {rotuloDoPagamento(opcao)}
                    </button>
                  ))}
                </div>
              </div>
            </Cartao>
          ))}
        </div>
      </div>
    </div>
  );
}

export function ConfigFinanceiroDialogo({
  config,
  onSalvar,
  onFechar,
}: {
  config: ConfigFinanceiro | null;
  onSalvar: (
    chave: string,
    nome: string,
    cidade: string,
    mensalidade: number,
    diaria: number,
  ) => void;
  onFechar: () => void;
}) {
  const emReais = (centavos: number): string =>
    centavos === 0 ? '' : `${Math.trunc(centavos / 100)},${String(centavos % 100).padStart(2, '0')}`;

  const [chave, setChave] = useState(config?.pixChave ?? '');
  const [nome, setNome] = useState(config?.pixNome ?? '');
  const [cidade, setCidade] = useState(config?.pixCidade ?? '');
  const [mensalidade, setMensalidade] = useState(emReais(config?.mensalidadeCentavos ?? 0));
  const [diaria, setDiaria] = useState(emReais(config?.diariaCentavos ?? 0));

  return (
    <Dialogo
      titulo="Chave Pix e valores"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            onClick={() =>
              onSalvar(chave, nome, cidade, emCentavos(mensalidade), emCentavos(diaria))
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={chave} rotulo="Chave Pix" onMudar={setChave} />
      <CampoTexto valor={nome} rotulo="Nome do recebedor" onMudar={setNome} />
      <CampoTexto valor={cidade} rotulo="Cidade" onMudar={setCidade} />
      <CampoTexto valor={mensalidade} rotulo="Mensalidade (R$)" onMudar={setMensalidade} />
      <CampoTexto valor={diaria} rotulo="Diária (R$)" onMudar={setDiaria} />
      <span className="subtitulo" style={{ fontSize: 11 }}>
        Os valores viram centavos inteiros no banco, então não há arredondamento.
      </span>
    </Dialogo>
  );
}

export function CartaoDaEvolucao({
  evolucao,
  dicas,
  pendentes,
  onAvaliar,
}: {
  evolucao: Evolucao | null;
  dicas: Dica[];
  pendentes: AvaliacaoPendente[];
  onAvaliar: () => void;
}) {
  const liberado = evolucaoLiberada(evolucao);
  const faltam = Math.max(MINIMO_DE_AVALIACOES - (evolucao?.totalAvaliacoes ?? 0), 0);
  const fraco = fundamentoMaisFraco(evolucao);
  const media = fraco && evolucao ? (evolucao.medias[fraco] ?? 0) : 0;
  const dicaDoFraco =
    fraco === null
      ? undefined
      : (dicas.filter((dica) => dica.fundamento === fraco).sort((a, b) => a.faixaMax - b.faixaMax)
          .find((dica) => media <= dica.faixaMax) ??
        dicas.find((dica) => dica.fundamento === fraco));

  return (
    <>
      {pendentes.length > 0 && (
        <Cartao>
          <div className="coluna" style={{ padding: 16, gap: 10 }}>
            <strong style={{ fontSize: 16 }}>
              {pendentes.length === 1
                ? '1 companheiro esperando a sua nota'
                : `${pendentes.length} companheiros esperando a sua nota`}
            </strong>
            <span className="subtitulo" style={{ fontSize: 12 }}>
              A nota é anônima: ninguém, nem a diretoria, vê quem deu qual nota.
            </span>
            <button type="button" className="botao botao-primario" onClick={onAvaliar}>
              Avaliar agora
            </button>
          </div>
        </Cartao>
      )}

      <Cartao>
        <div className="coluna" style={{ padding: 16, gap: 12 }}>
          <RotuloPequeno>Minha evolução</RotuloPequeno>

          {!liberado ? (
            <>
              <span className="subtitulo" style={{ fontSize: 13 }}>
                {faltam > 0
                  ? `Faltam ${faltam} avaliações para o painel acender.`
                  : 'O painel acende assim que chegarem avaliações suficientes.'}
              </span>
              <span className="subtitulo" style={{ fontSize: 11 }}>
                O mínimo existe para o anonimato: com poucas notas dá para adivinhar de quem vieram.
              </span>
            </>
          ) : (
            <>
              {FUNDAMENTOS.map((fundamento) => {
                const valor = evolucao?.medias[fundamento];
                if (valor === undefined) return null;
                return (
                  <div key={fundamento} className="coluna" style={{ gap: 4 }}>
                    <div className="linha-entre">
                      <span className="subtitulo" style={{ fontSize: 12 }}>
                        {rotuloDoFundamento(fundamento)}
                      </span>
                      <strong style={{ fontSize: 12 }}>{valor.toFixed(1)}</strong>
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
                  </div>
                );
              })}

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
  onEnviar: (pendente: AvaliacaoPendente, notas: NotasDaAvaliacao) => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Avaliar companheiros</span>
          <span className="subtitulo">Anônimo, e não mexe no sorteio</span>
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
  onEnviar: (notas: NotasDaAvaliacao) => void;
}) {
  const [notas, setNotas] = useState<NotasDaAvaliacao>(notasIniciais());

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
