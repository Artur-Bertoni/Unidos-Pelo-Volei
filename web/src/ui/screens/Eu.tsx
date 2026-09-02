import { useState, type ReactNode } from 'react';
import {
  aniversarioDe,
  POSICOES,
  rotuloDaPosicao,
  rotuloDoGenero,
  rotuloDaPresenca,
  rotuloDoPapel,
  STATUS_DE_PRESENCA,
  type Player,
  type PlayerContato,
  type PlayerPerformance,
  type Posicao,
  type StatusPresenca,
  type UserProfile,
  type VinculoPedido,
} from '../../domain/models';
import {
  CampoBusca,
  CampoTexto,
  Cartao,
  Dialogo,
  EstadoVazio,
  RotuloPequeno,
} from '../components/Componentes';
import { IconeEditar, IconeVoltar } from '../components/Icons';

export interface PedidoNaFila {
  pedido: VinculoPedido;
  jogador: Player | undefined;
}

interface EuProps {
  perfil: UserProfile | null;
  dataDoSabado: string;
  jogoHora: string | null;
  jogoLocal: string | null;
  minhaResposta: StatusPresenca | null;
  extras: ReactNode;
  onResponderChamada: (status: StatusPresenca) => void;
  meuJogador: Player | null;
  meuPedido: VinculoPedido | null;
  meuContato: PlayerContato | null;
  meuDesempenho: PlayerPerformance | undefined;
  candidatos: Player[];
  fila: PedidoNaFila[];
  busca: string;
  salvando: boolean;
  onBuscar: (valor: string) => void;
  onPedirVinculo: (playerId: string) => void;
  onCancelarPedido: () => void;
  onSalvarFicha: (
    nome: string,
    posicao: Posicao | null,
    dia: number | null,
    mes: number | null,
    telefone: string | null,
    emergencia: string | null,
    ano: number | null,
  ) => void;
  onAbrirAprovacoes: () => void;
}

export function EuScreen({
  perfil,
  dataDoSabado,
  jogoHora,
  jogoLocal,
  minhaResposta,
  extras,
  onResponderChamada,
  meuJogador,
  meuPedido,
  meuContato,
  meuDesempenho,
  candidatos,
  fila,
  busca,
  salvando,
  onBuscar,
  onPedirVinculo,
  onCancelarPedido,
  onSalvarFicha,
  onAbrirAprovacoes,
}: EuProps) {
  const [editando, setEditando] = useState(false);

  const aguardando = meuJogador === null && meuPedido?.status === 'pendente';
  const recusado = meuJogador === null && meuPedido?.status === 'recusado';
  const nomePretendido = fila.find((item) => item.pedido.id === meuPedido?.id)?.jogador?.nome;

  return (
    <div className="conteudo">
      <div className="lista" style={{ padding: 16, gap: 12 }}>
        {perfil?.isAdmin && fila.length > 0 && (
          <Cartao onClick={onAbrirAprovacoes}>
            <div className="linha" style={{ padding: 16, gap: 12 }}>
              <div className="coluna expandir" style={{ gap: 2 }}>
                <span className="titulo-tela">
                  {fila.length === 1 ? '1 pedido aguardando' : `${fila.length} pedidos aguardando`}
                </span>
                <span className="subtitulo">Confirme quem é quem para liberar o acesso</span>
              </div>
              <span className="subtitulo" aria-hidden="true">
                ›
              </span>
            </div>
          </Cartao>
        )}

        {aguardando && (
          <Cartao>
            <div className="coluna" style={{ padding: 16, gap: 10 }}>
              <span className="titulo-tela">Aguardando a diretoria</span>
              <span className="subtitulo">
                {nomePretendido
                  ? `Você pediu para ser ${nomePretendido}. Assim que alguém da diretoria confirmar, a sua ficha aparece aqui.`
                  : 'Assim que alguém da diretoria confirmar, a sua ficha aparece aqui.'}
              </span>
              <button
                type="button"
                className="botao-texto"
                disabled={salvando}
                onClick={onCancelarPedido}
              >
                Escolher outro nome
              </button>
            </div>
          </Cartao>
        )}

        {meuJogador && (
          <>
            <CartaoDaChamada
              dataDoSabado={dataDoSabado}
              jogoHora={jogoHora}
              jogoLocal={jogoLocal}
              minhaResposta={minhaResposta}
              salvando={salvando}
              onResponder={onResponderChamada}
            />
            <MinhaFicha
              perfil={perfil}
              jogador={meuJogador}
              contato={meuContato}
              desempenho={meuDesempenho}
              onEditar={() => setEditando(true)}
            />
            {extras}
          </>
        )}

        {!meuJogador && !aguardando && (
          <>
            <Cartao>
              <div className="coluna" style={{ padding: 16, gap: 6 }}>
                <span className="titulo-tela">Quem é você?</span>
                <span className="subtitulo">
                  {recusado
                    ? 'A diretoria não confirmou o pedido anterior. Escolha o seu nome de novo ou fale com quem organiza.'
                    : 'Ache o seu nome na lista do grupo. A diretoria confirma, e a partir daí a sua ficha e o seu histórico ficam aqui.'}
                </span>
              </div>
            </Cartao>

            <CampoBusca valor={busca} onMudar={onBuscar} dica="Buscar meu nome" />

            {candidatos.length === 0 ? (
              <EstadoVazio
                titulo="Nenhum nome disponível"
                descricao="Todos os jogadores da lista já têm dono. Fale com a diretoria para cadastrarem você."
              />
            ) : (
              candidatos.map((jogador) => (
                <Cartao key={jogador.id} apagado>
                  <div className="linha" style={{ padding: '10px 14px', gap: 12 }}>
                    <div className="coluna expandir" style={{ gap: 2 }}>
                      <strong>{jogador.nome}</strong>
                      <span className="subtitulo" style={{ fontSize: 11 }}>
                        {rotuloDoGenero(jogador.genero)}
                      </span>
                    </div>
                    <button
                      type="button"
                      className="botao botao-primario"
                      style={{ width: 'auto' }}
                      disabled={salvando}
                      onClick={() => onPedirVinculo(jogador.id)}
                    >
                      Sou eu
                    </button>
                  </div>
                </Cartao>
              ))
            )}
          </>
        )}
      </div>

      {editando && meuJogador && (
        <FichaDialogo
          jogador={meuJogador}
          contato={meuContato}
          salvando={salvando}
          onSalvar={(nome, posicao, dia, mes, telefone, emergencia, ano) => {
            onSalvarFicha(nome, posicao, dia, mes, telefone, emergencia, ano);
            setEditando(false);
          }}
          onFechar={() => setEditando(false)}
        />
      )}
    </div>
  );
}

function MinhaFicha({
  perfil,
  jogador,
  contato,
  desempenho,
  onEditar,
}: {
  perfil: UserProfile | null;
  jogador: Player;
  contato: PlayerContato | null;
  desempenho: PlayerPerformance | undefined;
  onEditar: () => void;
}) {
  const subtitulo = [
    perfil ? rotuloDoPapel(perfil.papel) : null,
    jogador.posicao ? rotuloDaPosicao(jogador.posicao) : null,
    rotuloDoGenero(jogador.genero),
  ]
    .filter((parte): parte is string => parte !== null)
    .join(' · ');

  return (
    <>
      <Cartao>
        <div className="coluna" style={{ padding: 16, gap: 14 }}>
          <div className="linha" style={{ gap: 12 }}>
            <span className="inicial-membro" aria-hidden="true">
              {jogador.nome.slice(0, 2).toUpperCase()}
            </span>
            <div className="coluna expandir" style={{ gap: 2 }}>
              <span className="titulo-tela">{jogador.nome}</span>
              <span className="subtitulo">{subtitulo}</span>
            </div>
            <button
              type="button"
              className="botao-icone"
              aria-label="Editar minha ficha"
              onClick={onEditar}
            >
              <IconeEditar />
            </button>
          </div>

          <LinhaDeDado rotulo="Aniversário" valor={aniversarioDe(jogador) ?? 'Não informado'} />
          <LinhaDeDado rotulo="Telefone" valor={contato?.telefone ?? 'Não informado'} />
          <LinhaDeDado rotulo="No grupo desde" valor={jogador.entrouEm ?? 'Não informado'} />
        </div>
      </Cartao>

      {desempenho && desempenho.dias > 0 && (
        <Cartao>
          <div className="coluna" style={{ padding: 16, gap: 12 }}>
            <RotuloPequeno>Meu histórico</RotuloPequeno>
            <div className="linha-entre">
              <Numero rotulo="Sábados" valor={desempenho.dias} />
              <Numero rotulo="Jogos" valor={desempenho.jogos} />
              <Numero rotulo="Vitórias" valor={desempenho.vitorias} />
              <Numero rotulo="Saldo" valor={desempenho.pontosPro - desempenho.pontosContra} />
            </div>
          </div>
        </Cartao>
      )}
    </>
  );
}

function LinhaDeDado({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="linha-entre">
      <span className="subtitulo">{rotulo}</span>
      <strong style={{ fontSize: 13 }}>{valor}</strong>
    </div>
  );
}

function Numero({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="coluna" style={{ alignItems: 'center', gap: 2 }}>
      <strong style={{ fontSize: 20 }}>{valor}</strong>
      <RotuloPequeno>{rotulo}</RotuloPequeno>
    </div>
  );
}

const soDigitos = (valor: string, maximo: number): string =>
  valor.replace(/\D/g, '').slice(0, maximo);

function FichaDialogo({
  jogador,
  contato,
  salvando,
  onSalvar,
  onFechar,
}: {
  jogador: Player;
  contato: PlayerContato | null;
  salvando: boolean;
  onSalvar: (
    nome: string,
    posicao: Posicao | null,
    dia: number | null,
    mes: number | null,
    telefone: string | null,
    emergencia: string | null,
    ano: number | null,
  ) => void;
  onFechar: () => void;
}) {
  const [nome, setNome] = useState(jogador.nome);
  const [posicao, setPosicao] = useState<Posicao | null>(jogador.posicao);
  const [dia, setDia] = useState(jogador.nascimentoDia?.toString() ?? '');
  const [mes, setMes] = useState(jogador.nascimentoMes?.toString() ?? '');
  const [ano, setAno] = useState(contato?.nascimentoAno?.toString() ?? '');
  const [telefone, setTelefone] = useState(contato?.telefone ?? '');
  const [emergencia, setEmergencia] = useState(contato?.contatoEmergencia ?? '');

  const numeroDe = (valor: string): number | null => (valor === '' ? null : Number(valor));
  const diaValido = dia === '' || (Number(dia) >= 1 && Number(dia) <= 31);
  const mesValido = mes === '' || (Number(mes) >= 1 && Number(mes) <= 12);
  const parCompleto = (dia === '') === (mes === '');
  const podeSalvar = nome.trim() !== '' && diaValido && mesValido && parCompleto && !salvando;

  return (
    <Dialogo
      titulo="Minha ficha"
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
            onClick={() =>
              onSalvar(
                nome.trim(),
                posicao,
                numeroDe(dia),
                numeroDe(mes),
                telefone,
                emergencia,
                numeroDe(ano),
              )
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={nome} rotulo="Nome" onMudar={setNome} />

      <div className="campo">
        <span className="campo-rotulo">Posição</span>
        <div className="linha" style={{ gap: 6, flexWrap: 'wrap' }}>
          {POSICOES.map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="chip"
              aria-pressed={posicao === opcao}
              onClick={() => setPosicao(posicao === opcao ? null : opcao)}
            >
              {rotuloDaPosicao(opcao).slice(0, 3).toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      <div className="campo">
        <span className="campo-rotulo">Aniversário</span>
        <div className="linha" style={{ gap: 8 }}>
          <div className="expandir">
            <CampoTexto valor={dia} rotulo="Dia" onMudar={(v) => setDia(soDigitos(v, 2))} />
          </div>
          <div className="expandir">
            <CampoTexto valor={mes} rotulo="Mês" onMudar={(v) => setMes(soDigitos(v, 2))} />
          </div>
          <div className="expandir">
            <CampoTexto valor={ano} rotulo="Ano" onMudar={(v) => setAno(soDigitos(v, 4))} />
          </div>
        </div>
        <span className="subtitulo" style={{ fontSize: 11 }}>
          O grupo vê só o dia e o mês. O ano fica com a diretoria.
        </span>
      </div>

      <CampoTexto valor={telefone} rotulo="Telefone" onMudar={setTelefone} />
      <CampoTexto valor={emergencia} rotulo="Contato de emergência" onMudar={setEmergencia} />
    </Dialogo>
  );
}

export function AprovacoesScreen({
  fila,
  salvando,
  onVoltar,
  onDecidir,
}: {
  fila: PedidoNaFila[];
  salvando: boolean;
  onVoltar: () => void;
  onDecidir: (pedido: VinculoPedido, aprovado: boolean) => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Pedidos de acesso</span>
          <span className="subtitulo">
            {fila.length === 1 ? '1 aguardando' : `${fila.length} aguardando`}
          </span>
        </div>
      </div>

      <div className="conteudo">
        {fila.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum pedido na fila"
            descricao="Quando alguém entrar e escolher o próprio nome, o pedido aparece aqui."
          />
        ) : (
          <div className="lista" style={{ padding: 16, gap: 12 }}>
            {fila.map((item) => (
              <Cartao key={item.pedido.id}>
                <div className="coluna" style={{ padding: 16, gap: 12 }}>
                  <div className="coluna" style={{ gap: 2 }}>
                    <RotuloPequeno>Entrou com a conta</RotuloPequeno>
                    <strong>{item.pedido.profileNome ?? 'Conta sem nome'}</strong>
                  </div>
                  <div className="coluna" style={{ gap: 2 }}>
                    <RotuloPequeno>Diz ser o jogador</RotuloPequeno>
                    <strong style={{ color: item.jogador ? 'var(--verde-claro)' : 'var(--vermelho)' }}>
                      {item.jogador?.nome ?? 'Jogador removido da lista'}
                    </strong>
                  </div>
                  <div className="linha" style={{ gap: 8 }}>
                    <button
                      type="button"
                      className="botao botao-primario expandir"
                      disabled={salvando || !item.jogador}
                      onClick={() => onDecidir(item.pedido, true)}
                    >
                      Confirmar
                    </button>
                    <button
                      type="button"
                      className="botao botao-contorno expandir"
                      disabled={salvando}
                      onClick={() => onDecidir(item.pedido, false)}
                    >
                      Recusar
                    </button>
                  </div>
                </div>
              </Cartao>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export function CartaoDaChamada({
  dataDoSabado,
  jogoHora,
  jogoLocal,
  minhaResposta,
  salvando,
  onResponder,
}: {
  dataDoSabado: string;
  jogoHora: string | null;
  jogoLocal: string | null;
  minhaResposta: StatusPresenca | null;
  salvando: boolean;
  onResponder: (status: StatusPresenca) => void;
}) {
  const partes = dataDoSabado.slice(0, 10).split('-');
  const diaEMes = partes.length === 3 ? `${partes[2]}/${partes[1]}` : dataDoSabado;
  const cores: Record<StatusPresenca, string> = {
    vou: 'var(--verde-claro)',
    talvez: 'var(--dourado)',
    nao_vou: 'var(--vermelho)',
  };

  return (
    <Cartao>
      <div className="coluna" style={{ padding: 16, gap: 12 }}>
        <RotuloPequeno>Sábado {diaEMes}</RotuloPequeno>
        <strong style={{ fontSize: 18 }}>
          {minhaResposta === null
            ? 'Você vai jogar?'
            : `Você respondeu: ${rotuloDaPresenca(minhaResposta)}`}
        </strong>
        {(jogoHora !== null || jogoLocal !== null) && (
          <span className="subtitulo" style={{ fontSize: 12 }}>
            {[jogoHora ? `às ${jogoHora}` : null, jogoLocal].filter(Boolean).join(' · ')}
          </span>
        )}
        <div className="linha" style={{ gap: 8 }}>
          {STATUS_DE_PRESENCA.map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="cartao-chamada-botao"
              aria-pressed={minhaResposta === opcao}
              disabled={salvando}
              style={
                minhaResposta === opcao
                  ? { background: cores[opcao], borderColor: cores[opcao] }
                  : undefined
              }
              onClick={() => onResponder(opcao)}
            >
              {rotuloDaPresenca(opcao)}
            </button>
          ))}
        </div>
      </div>
    </Cartao>
  );
}
