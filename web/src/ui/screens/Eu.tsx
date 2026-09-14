import { useState, type ReactNode } from 'react';
import {
  aniversarioDe,
  REGIMES_DO_ATLETA,
  rotuloDoGenero,
  rotuloDaPresenca,
  rotuloDoPapel,
  rotuloDoRegime,
  STATUS_DE_PRESENCA,
  type Player,
  type PlayerContato,
  type PlayerPerformance,
  type Regime,
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
import {
  IconeCarteira,
  IconeEditar,
  IconeEngrenagem,
  IconeEstrela,
  IconeGrafico,
  IconeGrupos,
  IconePessoa,
  IconeVoltar,
} from '../components/Icons';

export interface PedidoNaFila {
  pedido: VinculoPedido;
  jogador: Player | undefined;
}

export type DestinoDaEu =
  | 'grupos'
  | 'conta'
  | 'financeiro'
  | 'historico'
  | 'avaliacao'
  | 'configuracoes';

interface EuProps {
  nomeDoGrupo: string;
  quantosGrupos: number;
  isAdmin: boolean;
  dataDoSabado: string;
  jogoHora: string | null;
  jogoLocal: string | null;
  minhaResposta: StatusPresenca | null;
  onResponderChamada: (status: StatusPresenca) => void;
  meuJogador: Player | null;
  meuPedido: VinculoPedido | null;
  candidatos: Player[];
  fila: PedidoNaFila[];
  busca: string;
  salvando: boolean;
  avaliacoesPendentes: number;
  onBuscar: (valor: string) => void;
  onPedirVinculo: (playerId: string) => void;
  onCancelarPedido: () => void;
  onAbrir: (destino: DestinoDaEu) => void;
}

export function EuScreen({
  nomeDoGrupo,
  quantosGrupos,
  isAdmin,
  dataDoSabado,
  jogoHora,
  jogoLocal,
  minhaResposta,
  onResponderChamada,
  meuJogador,
  meuPedido,
  candidatos,
  fila,
  busca,
  salvando,
  avaliacoesPendentes,
  onBuscar,
  onPedirVinculo,
  onCancelarPedido,
  onAbrir,
}: EuProps) {
  const aguardando = meuJogador === null && meuPedido?.status === 'pendente';
  const recusado = meuJogador === null && meuPedido?.status === 'recusado';
  const nomePretendido = fila.find((item) => item.pedido.id === meuPedido?.id)?.jogador?.nome;
  const temJogador = meuJogador !== null;

  return (
    <div className="conteudo">
      <div className="lista" style={{ padding: 16, gap: 12 }}>
        {temJogador && (
          <CartaoDaChamada
            dataDoSabado={dataDoSabado}
            jogoHora={jogoHora}
            jogoLocal={jogoLocal}
            minhaResposta={minhaResposta}
            salvando={salvando}
            onResponder={onResponderChamada}
          />
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

        {!temJogador && !aguardando && (
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
                descricao="Todos os jogadores da lista já têm dono."
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

            <Cartao apagado>
              <p className="subtitulo" style={{ padding: 14, margin: 0, fontSize: 13 }}>
                Não encontrou seu nome na lista? Entre em contato com a diretoria para adicioná-lo
                aqui!
              </p>
            </Cartao>
          </>
        )}

        <div className="grade-eu">
          <BlocoDaGrade
            icone={<IconeGrupos tamanho={26} />}
            titulo="Grupos"
            subtitulo={
              quantosGrupos <= 1
                ? `${nomeDoGrupo} · entrar em outro`
                : `${nomeDoGrupo} · trocar entre ${quantosGrupos}`
            }
            onClick={() => onAbrir('grupos')}
          />
          <BlocoDaGrade
            icone={<IconePessoa tamanho={26} />}
            titulo="Detalhes da conta"
            subtitulo="Pagamento, aniversário e telefone"
            habilitado={temJogador}
            onClick={() => onAbrir('conta')}
          />
          <BlocoDaGrade
            icone={<IconeCarteira tamanho={26} />}
            titulo="Financeiro"
            subtitulo="O que você já pagou e o que falta"
            habilitado={temJogador}
            onClick={() => onAbrir('financeiro')}
          />
          <BlocoDaGrade
            icone={<IconeGrafico tamanho={26} />}
            titulo="Histórico"
            subtitulo="Sábados, vitórias e a sua evolução"
            habilitado={temJogador}
            onClick={() => onAbrir('historico')}
          />
          <BlocoDaGrade
            icone={<IconeEstrela tamanho={26} />}
            titulo="Avaliar colegas"
            subtitulo={
              avaliacoesPendentes > 0
                ? `${avaliacoesPendentes} esperando a sua nota`
                : 'Dê nota a quem jogou com você'
            }
            selo={avaliacoesPendentes > 0 ? String(avaliacoesPendentes) : undefined}
            habilitado={temJogador}
            onClick={() => onAbrir('avaliacao')}
          />
          {isAdmin && (
            <BlocoDaGrade
              icone={<IconeEngrenagem tamanho={26} />}
              titulo="Configurações"
              subtitulo="Só a diretoria vê e mexe"
              selo={fila.length > 0 ? String(fila.length) : undefined}
              onClick={() => onAbrir('configuracoes')}
            />
          )}
        </div>

        {!temJogador && (
          <span className="subtitulo" style={{ fontSize: 11 }}>
            Ficha, financeiro, histórico e avaliação abrem depois que a diretoria ligar a sua conta
            a um jogador da lista.
          </span>
        )}

      </div>
    </div>
  );
}

function BlocoDaGrade({
  icone,
  titulo,
  subtitulo,
  onClick,
  habilitado = true,
  selo,
}: {
  icone: ReactNode;
  titulo: string;
  subtitulo: string;
  onClick: () => void;
  habilitado?: boolean;
  selo?: string;
}) {
  return (
    <button
      type="button"
      className="bloco-eu"
      disabled={!habilitado}
      aria-label={`${titulo}. ${subtitulo}`}
      onClick={onClick}
    >
      <span className="bloco-eu-icone">
        {icone}
        {selo && <span className="bloco-eu-selo">{selo}</span>}
      </span>
      <strong className="bloco-eu-titulo">{titulo}</strong>
      <span className="bloco-eu-subtitulo">{subtitulo}</span>
    </button>
  );
}

function CabecalhoDaSubtela({
  titulo,
  subtitulo,
  onVoltar,
}: {
  titulo: string;
  subtitulo: string;
  onVoltar: () => void;
}) {
  return (
    <div className="linha" style={{ padding: 8, flex: 'none' }}>
      <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
        <IconeVoltar />
      </button>
      <div className="coluna">
        <span className="titulo-tela">{titulo}</span>
        <span className="subtitulo">{subtitulo}</span>
      </div>
    </div>
  );
}

export function ContaScreen({
  perfil,
  jogador,
  contato,
  salvando,
  onSalvarFicha,
  onVoltar,
}: {
  perfil: UserProfile | null;
  jogador: Player;
  contato: PlayerContato | null;
  salvando: boolean;
  onSalvarFicha: (
    nome: string,
    dia: number | null,
    mes: number | null,
    telefone: string | null,
    emergencia: string | null,
    ano: number | null,
    regime: Regime,
  ) => void;
  onVoltar: () => void;
}) {
  const [editando, setEditando] = useState(false);

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <CabecalhoDaSubtela
        titulo="Detalhes da conta"
        subtitulo="Como você aparece para o grupo"
        onVoltar={onVoltar}
      />
      <div className="conteudo">
        <div className="lista" style={{ padding: 16, gap: 12 }}>
          <FichaDoJogador
            perfil={perfil}
            jogador={jogador}
            contato={contato}
            onEditar={() => setEditando(true)}
          />
        </div>
      </div>

      {editando && (
        <FichaDialogo
          jogador={jogador}
          contato={contato}
          salvando={salvando}
          onSalvar={(nome, dia, mes, telefone, emergencia, ano, regime) => {
            onSalvarFicha(nome, dia, mes, telefone, emergencia, ano, regime);
            setEditando(false);
          }}
          onFechar={() => setEditando(false)}
        />
      )}
    </div>
  );
}

export function HistoricoScreen({
  desempenho,
  onVoltar,
  children,
}: {
  desempenho: PlayerPerformance | undefined;
  onVoltar: () => void;
  children: ReactNode;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <CabecalhoDaSubtela
        titulo="Histórico"
        subtitulo="O que você jogou e como a sua nota andou"
        onVoltar={onVoltar}
      />
      <div className="conteudo">
        <div className="lista" style={{ padding: 16, gap: 12 }}>
          {desempenho && desempenho.dias > 0 ? (
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
          ) : (
            <EstadoVazio
              titulo="Nenhum sábado ainda"
              descricao="Os números aparecem depois do primeiro dia encerrado com você em quadra."
            />
          )}
          {children}
        </div>
      </div>
    </div>
  );
}

function FichaDoJogador({
  perfil,
  jogador,
  contato,
  onEditar,
}: {
  perfil: UserProfile | null;
  jogador: Player;
  contato: PlayerContato | null;
  onEditar: () => void;
}) {
  const subtitulo = [perfil ? rotuloDoPapel(perfil.papel) : null, rotuloDoGenero(jogador.genero)]
    .filter((parte): parte is string => parte !== null)
    .join(' · ');

  return (
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

        <LinhaDeDado rotulo="Como eu pago" valor={rotuloDoRegime(jogador.regime as Regime)} />
        <LinhaDeDado rotulo="Aniversário" valor={aniversarioDe(jogador) ?? 'Não informado'} />
        <LinhaDeDado rotulo="Telefone" valor={contato?.telefone ?? 'Não informado'} />
        <LinhaDeDado rotulo="No grupo desde" valor={jogador.entrouEm ?? 'Não informado'} />
      </div>
    </Cartao>
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
    dia: number | null,
    mes: number | null,
    telefone: string | null,
    emergencia: string | null,
    ano: number | null,
    regime: Regime,
  ) => void;
  onFechar: () => void;
}) {
  const [nome, setNome] = useState(jogador.nome);
  const [regime, setRegime] = useState<Regime>(jogador.regime as Regime);
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
                numeroDe(dia),
                numeroDe(mes),
                telefone,
                emergencia,
                numeroDe(ano),
                regime,
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
        <span className="campo-rotulo">Como eu pago</span>
        {jogador.regime === 'isento' ? (
          <span className="subtitulo" style={{ fontSize: 11 }}>
            A diretoria te deixou isento. Fale com quem organiza para mudar.
          </span>
        ) : (
          <>
            <div className="linha" style={{ gap: 6, flexWrap: 'wrap' }}>
              {REGIMES_DO_ATLETA.map((opcao) => (
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
            <span className="subtitulo" style={{ fontSize: 11 }}>
              {regime === 'mensalista'
                ? 'Mensalista paga o mês inteiro e joga todo sábado.'
                : 'Diarista paga só a diária dos sábados em que aparecer.'}
            </span>
          </>
        )}
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
  onAbrirVinculos,
}: {
  fila: PedidoNaFila[];
  salvando: boolean;
  onVoltar: () => void;
  onDecidir: (pedido: VinculoPedido, aprovado: boolean) => void;
  onAbrirVinculos: () => void;
}) {
  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna expandir">
          <span className="titulo-tela">Pedidos de acesso</span>
          <span className="subtitulo">
            {fila.length === 1 ? '1 aguardando' : `${fila.length} aguardando`}
          </span>
        </div>
        <button type="button" className="botao-texto" onClick={onAbrirVinculos}>
          Vincular manualmente
        </button>
      </div>

      <div className="conteudo">
        {fila.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum pedido na fila"
            descricao="Quando alguém entrar e escolher o próprio nome, o pedido aparece aqui. Você também pode ligar jogador e conta na mão, em Vincular manualmente."
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
        <strong style={{ fontSize: 18 }}>Você vai jogar?</strong>
        {(jogoHora !== null || jogoLocal !== null) && (
          <span className="subtitulo" style={{ fontSize: 12 }}>
            {[jogoHora ? `às ${jogoHora}` : null, jogoLocal].filter(Boolean).join(' · ')}
          </span>
        )}
        {minhaResposta !== null && (
          <span className="subtitulo" style={{ fontSize: 12 }}>
            {`Você respondeu: ${rotuloDaPresenca(minhaResposta)}. Pode trocar quando quiser.`}
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
