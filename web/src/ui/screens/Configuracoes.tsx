import { useState, type ReactNode } from 'react';
import { CampoTexto, Cartao, Dialogo } from '../components/Componentes';
import {
  IconeCarteira,
  IconeChave,
  IconeEditar,
  IconeGrupos,
  IconePessoa,
  IconeRelogio,
  IconeVoltar,
} from '../components/Icons';

export type AcaoDaDiretoria =
  | 'aprovacoes'
  | 'vinculos'
  | 'chaves'
  | 'membros'
  | 'identidade'
  | 'painel-financeiro'
  | 'config-do-jogo';

export function ConfiguracoesScreen({
  nomeDoGrupo,
  pedidosPendentes,
  onVoltar,
  onAbrir,
}: {
  nomeDoGrupo: string;
  pedidosPendentes: number;
  onVoltar: () => void;
  onAbrir: (acao: AcaoDaDiretoria) => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Configurações</span>
          <span className="subtitulo">O que só a diretoria do {nomeDoGrupo} mexe</span>
        </div>
      </div>

      <div className="conteudo">
        <div className="lista" style={{ padding: 16, gap: 10 }}>
          <Atalho
            icone={<IconePessoa tamanho={20} />}
            titulo={
              pedidosPendentes === 0
                ? 'Pedidos de vínculo'
                : `${pedidosPendentes} ${pedidosPendentes === 1 ? 'pedido aguardando' : 'pedidos aguardando'}`
            }
            descricao="Confirme quem é quem para liberar o acesso ao grupo"
            selo={pedidosPendentes > 0 ? String(pedidosPendentes) : undefined}
            onClick={() => onAbrir('aprovacoes')}
          />
          <Atalho
            icone={<IconePessoa tamanho={20} />}
            titulo="Contas e jogadores"
            descricao="Ligue um jogador a uma conta na mão, ou desfaça um vínculo"
            onClick={() => onAbrir('vinculos')}
          />
          <Atalho
            icone={<IconeChave tamanho={20} />}
            titulo="Chaves de acesso"
            descricao="Crie e revogue os códigos que liberam a entrada no grupo"
            onClick={() => onAbrir('chaves')}
          />
          <Atalho
            icone={<IconeGrupos tamanho={20} />}
            titulo="Membros do grupo"
            descricao="Promova alguém à diretoria ou tire quem saiu"
            onClick={() => onAbrir('membros')}
          />
          <Atalho
            icone={<IconeEditar tamanho={20} />}
            titulo="Nome, cidade e logo"
            descricao={`Troque como o ${nomeDoGrupo} aparece no app`}
            onClick={() => onAbrir('identidade')}
          />
          <Atalho
            icone={<IconeCarteira tamanho={20} />}
            titulo="Painel financeiro"
            descricao="Quem pagou, quem deve, e gerar mensalidade ou diária"
            onClick={() => onAbrir('painel-financeiro')}
          />
          <Atalho
            icone={<IconeRelogio tamanho={20} />}
            titulo="Horário e local do sábado"
            descricao="O que aparece no cartão de presença de todo mundo"
            onClick={() => onAbrir('config-do-jogo')}
          />
        </div>
      </div>
    </div>
  );
}

function Atalho({
  icone,
  titulo,
  descricao,
  onClick,
  selo,
}: {
  icone: ReactNode;
  titulo: string;
  descricao: string;
  onClick: () => void;
  selo?: string;
}) {
  return (
    <Cartao onClick={onClick}>
      <div className="linha" style={{ padding: '12px 14px', gap: 12 }}>
        <span className="atalho-icone">{icone}</span>
        <div className="coluna expandir" style={{ gap: 2 }}>
          <strong style={{ fontSize: 14 }}>{titulo}</strong>
          <span className="subtitulo" style={{ fontSize: 12 }}>
            {descricao}
          </span>
        </div>
        {selo && <span className="bloco-eu-selo atalho-selo">{selo}</span>}
        <span className="subtitulo" aria-hidden="true">
          ›
        </span>
      </div>
    </Cartao>
  );
}

const horaValida = (valor: string): boolean => /^([01]\d|2[0-3]):[0-5]\d$/.test(valor);

export function ConfigDoJogoDialogo({
  jogoHora,
  jogoLocal,
  salvando,
  onSalvar,
  onFechar,
}: {
  jogoHora: string | null;
  jogoLocal: string | null;
  salvando: boolean;
  onSalvar: (hora: string, local: string | null) => void;
  onFechar: () => void;
}) {
  const [hora, setHora] = useState(jogoHora ?? '09:00');
  const [local, setLocal] = useState(jogoLocal ?? '');
  const podeSalvar = horaValida(hora) && !salvando;

  return (
    <Dialogo
      titulo="Horário e local do sábado"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={!podeSalvar}
            onClick={() => onSalvar(hora, local.trim() === '' ? null : local.trim())}
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={hora} rotulo="Hora (HH:MM)" onMudar={setHora} />
      {!horaValida(hora) && (
        <span className="subtitulo" style={{ fontSize: 11, color: 'var(--vermelho)' }}>
          Use o formato 24 horas, como 09:00 ou 14:30.
        </span>
      )}
      <CampoTexto valor={local} rotulo="Local" onMudar={setLocal} />
      <span className="subtitulo" style={{ fontSize: 11 }}>
        Isso aparece no cartão de presença de todo mundo e no lembrete que sai antes do jogo.
      </span>
    </Dialogo>
  );
}
