import { useState } from 'react';
import {
  rotuloDoPagamento,
  STATUS_DE_PAGAMENTO,
  type ConfigFinanceiro,
  type ItemDoExtrato,
  type StatusPagamento,
} from '../../domain/models';
import {
  chavePixValida,
  gerarBrCode,
  normalizarChavePix,
  rotuloDaChavePix,
  TIPOS_DE_CHAVE_PIX,
  type TipoDaChavePix,
} from '../../domain/pix';
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

export function MeuFinanceiroScreen({
  extrato,
  config,
  onVoltar,
}: {
  extrato: ItemDoExtrato[];
  config: ConfigFinanceiro | null;
  onVoltar: () => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Financeiro</span>
          <span className="subtitulo">O que você já pagou e o que falta</span>
        </div>
      </div>
      <div className="conteudo">
        <div className="lista" style={{ padding: 16, gap: 12 }}>
          <CartaoDoExtrato extrato={extrato} config={config} />
        </div>
      </div>
    </div>
  );
}

export function CartaoDoExtrato({
  extrato,
  config,
}: {
  extrato: ItemDoExtrato[];
  config: ConfigFinanceiro | null;
}) {
  const [copiado, setCopiado] = useState(false);
  const emAberto = extrato
    .filter((item) => item.pagamento.status === 'pendente')
    .reduce((soma, item) => soma + item.pagamento.valorCentavos, 0);

  const codigo =
    config?.pixChave && emAberto > 0
      ? gerarBrCode(
          config.pixChave,
          config.pixTipo,
          config.pixNome ?? '',
          config.pixCidade ?? '',
          emAberto,
        )
      : null;

  return (
    <Cartao>
      <div className="coluna" style={{ padding: 16, gap: 12 }}>
        <RotuloPequeno>Meu financeiro</RotuloPequeno>

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

const DICAS_DE_CHAVE: Record<TipoDaChavePix, string> = {
  celular: 'Digite como quiser: o código do país entra sozinho. Precisa de DDD mais o número.',
  cpf: 'Onze dígitos. Ponto e traço podem ficar, saem sozinhos.',
  cnpj: 'Quatorze dígitos. Ponto, barra e traço podem ficar, saem sozinhos.',
  email: 'O e-mail que você registrou como chave no banco.',
  aleatoria: 'A chave aleatória tem 36 caracteres com hífens. Copie do app do banco.',
};

export function ConfigFinanceiroDialogo({
  config,
  onSalvar,
  onFechar,
}: {
  config: ConfigFinanceiro | null;
  onSalvar: (
    chave: string,
    tipo: TipoDaChavePix,
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
  const [tipo, setTipo] = useState<TipoDaChavePix>(config?.pixTipo ?? 'aleatoria');
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
            disabled={!chavePixValida(chave, tipo)}
            onClick={() =>
              onSalvar(chave, tipo, nome, cidade, emCentavos(mensalidade), emCentavos(diaria))
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <div className="campo">
        <span className="campo-rotulo">Tipo da chave Pix</span>
        <div className="linha" style={{ gap: 6, flexWrap: 'wrap' }}>
          {TIPOS_DE_CHAVE_PIX.map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="chip"
              aria-pressed={tipo === opcao}
              onClick={() => setTipo(opcao)}
            >
              {rotuloDaChavePix(opcao)}
            </button>
          ))}
        </div>
      </div>
      <CampoTexto valor={chave} rotulo="Chave Pix" onMudar={setChave} />
      <span
        className="subtitulo"
        style={{ fontSize: 11, color: chavePixValida(chave, tipo) ? undefined : 'var(--vermelho)' }}
      >
        {chavePixValida(chave, tipo)
          ? `No código Pix ela vai como ${normalizarChavePix(chave, tipo)}`
          : DICAS_DE_CHAVE[tipo]}
      </span>
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
