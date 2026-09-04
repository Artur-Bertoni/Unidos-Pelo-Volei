import { useEffect, useRef, useState } from 'react';
import { buscarEnderecos, MINIMO_DE_LETRAS } from '../../lib/enderecos';
import {
  EMOJI_PADRAO,
  EMOJIS_DE_REACAO,
  rotuloDaCategoria,
  rotuloDaPresenca,
  rotuloDoEvento,
  STATUS_DE_PRESENCA,
  TIPOS_DE_EVENTO,
  type Evento,
  type Marco,
  type Pagina,
  type Player,
  type Post,
  type Presenca,
  type ResumoDaChamada,
  type StatusPresenca,
  type TipoEvento,
} from '../../domain/models';
import { CampoTexto, Cartao, Dialogo, EstadoVazio, RotuloPequeno, Selo } from '../components/Componentes';
import { IconeEditar, IconeLixeira, IconeVoltar } from '../components/Icons';

export type SecaoDoGrupo = 'mural' | 'agenda' | 'chamada' | 'regras';

const SECOES: { chave: SecaoDoGrupo; rotulo: string }[] = [
  { chave: 'mural', rotulo: 'Mural' },
  { chave: 'agenda', rotulo: 'Agenda' },
  { chave: 'chamada', rotulo: 'Chamada' },
  { chave: 'regras', rotulo: 'Regras' },
];

export const dataLegivel = (iso: string): string => {
  const partes = iso.slice(0, 10).split('-');
  return partes.length === 3 ? `${partes[2]}/${partes[1]}` : iso;
};

export function marcosDe(jogadores: Player[], hoje = new Date()): Marco[] {
  const aniversarios: Marco[] = jogadores
    .filter((jogador) => jogador.nascimentoDia !== null && jogador.nascimentoMes !== null)
    .map((jogador) => ({
      playerId: jogador.id,
      nome: jogador.nome,
      tipo: 'aniversario' as const,
      dia: jogador.nascimentoDia as number,
      mes: jogador.nascimentoMes as number,
      anos: null,
    }));

  const tempoDeCasa: Marco[] = jogadores
    .filter((jogador) => jogador.entrouEm !== null)
    .map((jogador) => {
      const partes = (jogador.entrouEm as string).slice(0, 10).split('-');
      const anos = hoje.getFullYear() - Number(partes[0]);
      return {
        playerId: jogador.id,
        nome: jogador.nome,
        tipo: 'tempo_de_casa' as const,
        dia: Number(partes[2]),
        mes: Number(partes[1]),
        anos,
      };
    })
    .filter((marco) => marco.anos !== null && marco.anos >= 1 && !Number.isNaN(marco.dia));

  const diasAte = (marco: Marco): number => {
    const alvo = new Date(hoje.getFullYear(), marco.mes - 1, marco.dia);
    if (alvo < hoje) alvo.setFullYear(alvo.getFullYear() + 1);
    return Math.round((alvo.getTime() - hoje.getTime()) / 86400000);
  };

  return [...aniversarios, ...tempoDeCasa].sort(
    (a, b) => diasAte(a) - diasAte(b) || a.nome.localeCompare(b.nome),
  );
}

export function resumoDaChamada(presencas: Presenca[], totalDeJogadores: number): ResumoDaChamada {
  const conta = (status: StatusPresenca) => presencas.filter((p) => p.status === status).length;
  const vou = conta('vou');
  const talvez = conta('talvez');
  const naoVou = conta('nao_vou');
  return {
    vou,
    talvez,
    naoVou,
    semResposta: Math.max(totalDeJogadores - vou - talvez - naoVou, 0),
  };
}

interface GrupoProps {
  secao: SecaoDoGrupo;
  isAdmin: boolean;
  posts: Post[];
  eventos: Evento[];
  marcos: Marco[];
  paginas: Pagina[];
  membros: Player[];
  presencas: Presenca[];
  resumo: ResumoDaChamada;
  dataDoSabado: string;
  salvando: boolean;
  onSecao: (secao: SecaoDoGrupo) => void;
  onNovoPost: () => void;
  onExcluirPost: (postId: string) => void;
  onReagir: (postId: string, emoji: string) => void;
  onNovoEvento: () => void;
  onEditarEvento: (evento: Evento) => void;
  onExcluirEvento: (eventoId: string) => void;
  onAbrirPagina: (pagina: Pagina) => void;
  onResponderPor: (playerId: string, status: StatusPresenca) => void;
  onTrazerConfirmados: () => void;
}

export function GrupoScreen({
  secao,
  isAdmin,
  posts,
  eventos,
  marcos,
  paginas,
  membros,
  presencas,
  resumo,
  dataDoSabado,
  salvando,
  onSecao,
  onNovoPost,
  onExcluirPost,
  onReagir,
  onNovoEvento,
  onEditarEvento,
  onExcluirEvento,
  onAbrirPagina,
  onResponderPor,
  onTrazerConfirmados,
}: GrupoProps) {
  const porJogador = new Map(presencas.map((presenca) => [presenca.playerId, presenca]));

  return (
    <div className="conteudo">
      <div className="linha" style={{ gap: 8, padding: '12px 16px', overflowX: 'auto' }}>
        {SECOES.map((item) => (
          <button
            key={item.chave}
            type="button"
            className="chip"
            aria-pressed={secao === item.chave}
            onClick={() => onSecao(item.chave)}
          >
            {item.rotulo}
          </button>
        ))}
      </div>

      <div className="lista" style={{ padding: 16, gap: 12 }}>
        {secao === 'mural' && (
          <>
            {isAdmin && (
              <button type="button" className="botao botao-primario" onClick={onNovoPost}>
                Publicar no mural
              </button>
            )}
            {posts.length === 0 && (
              <EstadoVazio
                titulo="Mural vazio"
                descricao="Quando a diretoria publicar um recado, ele aparece aqui."
              />
            )}
            {posts.map((post) => (
              <Cartao key={post.id}>
                <div className="coluna" style={{ padding: 16, gap: 8 }}>
                  <div className="linha" style={{ gap: 8 }}>
                    {post.fixado && <span aria-label="Fixado">📌</span>}
                    <strong className="expandir">{post.titulo}</strong>
                    {isAdmin && (
                      <button
                        type="button"
                        className="botao-icone"
                        aria-label="Excluir publicação"
                        onClick={() => onExcluirPost(post.id)}
                      >
                        <IconeLixeira />
                      </button>
                    )}
                  </div>
                  {post.corpo !== '' && <p className="subtitulo">{post.corpo}</p>}
                  {post.imagemUrl !== null && (
                    <img
                      src={post.imagemUrl}
                      alt={`Imagem do recado ${post.titulo}`}
                      loading="lazy"
                      style={{
                        width: '100%',
                        maxHeight: 320,
                        objectFit: 'cover',
                        borderRadius: 12,
                        background: 'var(--cartao-interno)',
                      }}
                    />
                  )}
                  <div className="linha-entre">
                    <span className="subtitulo" style={{ fontSize: 11 }}>
                      {post.autorNome ?? 'Diretoria'}
                    </span>
                    <button
                      type="button"
                      className="chip"
                      aria-pressed={post.reagi}
                      onClick={() => onReagir(post.id, post.emoji)}
                    >
                      {post.reacoes > 0 ? `${post.emoji} ${post.reacoes}` : post.emoji}
                    </button>
                  </div>
                </div>
              </Cartao>
            ))}
          </>
        )}

        {secao === 'agenda' && (
          <>
            {isAdmin && (
              <button type="button" className="botao botao-primario" onClick={onNovoEvento}>
                Novo evento
              </button>
            )}
            {eventos.length === 0 && (
              <EstadoVazio
                titulo="Nada marcado"
                descricao="O sábado é toda semana. Aqui entram jogo, confraternização e campeonato."
              />
            )}
            {eventos.map((evento) => (
              <Cartao key={evento.id}>
                <div className="linha" style={{ padding: 16, gap: 12 }}>
                  <div className="coluna expandir" style={{ gap: 2 }}>
                    <strong>{evento.titulo}</strong>
                    <span className="subtitulo" style={{ fontSize: 12 }}>
                      {[dataLegivel(evento.inicio), evento.local].filter(Boolean).join(' · ')}
                    </span>
                  </div>
                  <Selo
                    texto={rotuloDoEvento(evento.tipo)}
                    corTexto="var(--selo-fase-texto)"
                    corFundo="var(--selo-fase-fundo)"
                  />
                  {isAdmin && (
                    <>
                      <button
                        type="button"
                        className="botao-icone"
                        aria-label="Editar evento"
                        onClick={() => onEditarEvento(evento)}
                      >
                        <IconeEditar />
                      </button>
                      <button
                        type="button"
                        className="botao-icone"
                        aria-label="Excluir evento"
                        onClick={() => onExcluirEvento(evento.id)}
                      >
                        <IconeLixeira />
                      </button>
                    </>
                  )}
                </div>
              </Cartao>
            ))}

            {marcos.length > 0 && <RotuloPequeno>Datas do grupo</RotuloPequeno>}
            {marcos.map((marco) => (
              <Cartao key={`${marco.playerId}-${marco.tipo}`} apagado>
                <div className="linha" style={{ padding: '10px 14px', gap: 10 }}>
                  <span aria-hidden="true">{marco.tipo === 'aniversario' ? '🎂' : '🏐'}</span>
                  <div className="coluna expandir" style={{ gap: 2 }}>
                    <span>{marco.nome}</span>
                    <span className="subtitulo" style={{ fontSize: 11 }}>
                      {marco.tipo === 'aniversario'
                        ? `Aniversário em ${String(marco.dia).padStart(2, '0')}/${String(marco.mes).padStart(2, '0')}`
                        : `${marco.anos} anos de Unidos em ${String(marco.dia).padStart(2, '0')}/${String(marco.mes).padStart(2, '0')}`}
                    </span>
                  </div>
                </div>
              </Cartao>
            ))}
          </>
        )}

        {secao === 'chamada' && (
          <>
            <Cartao>
              <div className="coluna" style={{ padding: 16, gap: 12 }}>
                <RotuloPequeno>Sábado {dataLegivel(dataDoSabado)}</RotuloPequeno>
                <div className="linha-entre">
                  <NumeroDaChamada rotulo="Vou" valor={resumo.vou} cor="var(--verde-claro)" />
                  <NumeroDaChamada rotulo="Talvez" valor={resumo.talvez} cor="var(--dourado)" />
                  <NumeroDaChamada rotulo="Não vou" valor={resumo.naoVou} cor="var(--vermelho)" />
                  <NumeroDaChamada
                    rotulo="Sem resposta"
                    valor={resumo.semResposta}
                    cor="var(--texto-terciario)"
                  />
                </div>
                {isAdmin && (
                  <button
                    type="button"
                    className="botao botao-primario"
                    disabled={salvando || resumo.vou === 0}
                    onClick={onTrazerConfirmados}
                  >
                    Trazer confirmados para a lista de hoje
                  </button>
                )}
              </div>
            </Cartao>

            {membros.map((membro) => {
              const presenca = porJogador.get(membro.id);
              return (
                <Cartao key={membro.id} apagado>
                  <div className="coluna" style={{ padding: '10px 14px', gap: 8 }}>
                    <div className="linha">
                      <div className="coluna expandir">
                        <span>{membro.nome}</span>
                        {presenca?.origem === 'diretoria' && (
                          <span className="subtitulo" style={{ fontSize: 10 }}>
                            respondido pela diretoria
                          </span>
                        )}
                      </div>
                      {presenca && (
                        <Selo
                          texto={rotuloDaPresenca(presenca.status)}
                          corTexto={corDaPresenca(presenca.status)}
                          corFundo="var(--cartao)"
                        />
                      )}
                    </div>
                    {isAdmin && (
                      <div className="linha" style={{ gap: 6 }}>
                        {STATUS_DE_PRESENCA.map((opcao) => (
                          <button
                            key={opcao}
                            type="button"
                            className="chip"
                            aria-pressed={presenca?.status === opcao}
                            onClick={() => onResponderPor(membro.id, opcao)}
                          >
                            {rotuloDaPresenca(opcao)}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </Cartao>
              );
            })}
          </>
        )}


        {secao === 'regras' &&
          paginas.map((pagina) => (
            <Cartao key={pagina.id} onClick={() => onAbrirPagina(pagina)}>
              <div className="coluna" style={{ padding: 16, gap: 4 }}>
                <RotuloPequeno>{rotuloDaCategoria(pagina.categoria)}</RotuloPequeno>
                <strong>{pagina.titulo}</strong>
                <span className="subtitulo" style={{ fontSize: 12 }}>
                  {pagina.corpo
                    .split('\n')
                    .find((linha) => linha.trim() !== '')
                    ?.replace(/^#\s*/, '') ?? 'Página em branco'}
                </span>
              </div>
            </Cartao>
          ))}
      </div>
    </div>
  );
}

function NumeroDaChamada({ rotulo, valor, cor }: { rotulo: string; valor: number; cor: string }) {
  return (
    <div className="coluna" style={{ alignItems: 'center', gap: 2 }}>
      <strong style={{ fontSize: 20, color: cor }}>{valor}</strong>
      <RotuloPequeno>{rotulo}</RotuloPequeno>
    </div>
  );
}

const corDaPresenca = (status: StatusPresenca): string => {
  if (status === 'vou') return 'var(--verde-claro)';
  if (status === 'talvez') return 'var(--dourado)';
  return 'var(--vermelho)';
};

export function PostDialogo({
  salvando,
  onSalvar,
  onFechar,
}: {
  salvando: boolean;
  onSalvar: (
    titulo: string,
    corpo: string,
    fixado: boolean,
    imagem: File | null,
    emoji: string,
  ) => void;
  onFechar: () => void;
}) {
  const [titulo, setTitulo] = useState('');
  const [corpo, setCorpo] = useState('');
  const [fixado, setFixado] = useState(false);
  const [emoji, setEmoji] = useState(EMOJI_PADRAO);
  const [imagem, setImagem] = useState<File | null>(null);

  return (
    <Dialogo
      titulo="Publicar no mural"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={titulo.trim() === '' || salvando}
            onClick={() => onSalvar(titulo, corpo, fixado, imagem, emoji)}
          >
            {salvando && imagem !== null ? 'Enviando…' : 'Publicar'}
          </button>
        </>
      }
    >
      <CampoTexto valor={titulo} rotulo="Título" onMudar={setTitulo} />
      <label className="campo">
        <span className="campo-rotulo">Recado</span>
        <textarea
          className="campo-entrada"
          rows={5}
          value={corpo}
          onChange={(e) => setCorpo(e.target.value)}
        />
      </label>

      <label className="campo">
        <span className="campo-rotulo">Imagem</span>
        <input
          type="file"
          className="campo-entrada"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={(e) => setImagem(e.target.files?.[0] ?? null)}
        />
        {imagem !== null && (
          <span className="subtitulo" style={{ fontSize: 11 }}>
            {`${imagem.name} · ${Math.round(imagem.size / 1024)} KB`}
          </span>
        )}
      </label>

      <div className="campo">
        <span className="campo-rotulo">Reação padrão</span>
        <div className="linha" style={{ gap: 4, flexWrap: 'wrap' }}>
          {EMOJIS_DE_REACAO.map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="chip"
              aria-pressed={emoji === opcao}
              style={{ fontSize: 17 }}
              onClick={() => setEmoji(opcao)}
            >
              {opcao}
            </button>
          ))}
        </div>
        <span className="subtitulo" style={{ fontSize: 11 }}>
          É o emoji que o grupo toca para reagir a este recado.
        </span>
      </div>

      <div className="linha-entre">
        <span className="subtitulo">Fixar no topo</span>
        <button
          type="button"
          className="chip"
          aria-pressed={fixado}
          onClick={() => setFixado(!fixado)}
        >
          {fixado ? 'Sim' : 'Não'}
        </button>
      </div>
    </Dialogo>
  );
}

const soDigitos = (valor: string, maximo: number): string =>
  valor.replace(/\D/g, '').slice(0, maximo);

export const mascaraDeData = (entrada: string): string =>
  soDigitos(entrada, 8)
    .split('')
    .map((caractere, indice) => (indice === 2 || indice === 4 ? `/${caractere}` : caractere))
    .join('');

export const mascaraDeHora = (entrada: string): string =>
  soDigitos(entrada, 4)
    .split('')
    .map((caractere, indice) => (indice === 2 ? `:${caractere}` : caractere))
    .join('');

export const paraIso = (diaMesAno: string): string | null => {
  const partes = diaMesAno.split('/');
  if (partes.length !== 3 || partes[2].length !== 4) return null;

  const dia = Number(partes[0]);
  const mes = Number(partes[1]);
  const ano = Number(partes[2]);
  if (!Number.isInteger(dia) || !Number.isInteger(mes) || !Number.isInteger(ano)) return null;
  if (dia < 1 || dia > 31 || mes < 1 || mes > 12) return null;

  const data = new Date(Date.UTC(ano, mes - 1, dia));
  if (data.getUTCFullYear() !== ano || data.getUTCMonth() !== mes - 1 || data.getUTCDate() !== dia) {
    return null;
  }
  return `${partes[2]}-${partes[1].padStart(2, '0')}-${partes[0].padStart(2, '0')}`;
};

export const paraDiaMesAno = (iso: string | undefined): string => {
  const partes = iso?.slice(0, 10).split('-');
  return partes?.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : '';
};

export function EventoDialogo({
  evento,
  salvando,
  onSalvar,
  onFechar,
}: {
  evento: Evento | null;
  salvando: boolean;
  onSalvar: (
    id: string | null,
    titulo: string,
    descricao: string,
    tipo: TipoEvento,
    inicio: string,
    local: string,
  ) => void;
  onFechar: () => void;
}) {
  const [titulo, setTitulo] = useState(evento?.titulo ?? '');
  const [descricao, setDescricao] = useState(evento?.descricao ?? '');
  const [data, setData] = useState(paraDiaMesAno(evento?.inicio));
  const [hora, setHora] = useState(evento?.inicio.slice(11, 16) ?? '19:00');
  const [local, setLocal] = useState(evento?.local ?? '');
  const [tipo, setTipo] = useState<TipoEvento>(evento?.tipo ?? 'jogo');
  const [sugestoes, setSugestoes] = useState<string[]>([]);
  const [mostrarSugestoes, setMostrarSugestoes] = useState(false);
  const cancelar = useRef<AbortController | null>(null);
  const espera = useRef<number | undefined>(undefined);

  useEffect(
    () => () => {
      cancelar.current?.abort();
      window.clearTimeout(espera.current);
    },
    [],
  );

  const procurarEndereco = (termo: string): void => {
    cancelar.current?.abort();
    window.clearTimeout(espera.current);
    if (termo.trim().length < MINIMO_DE_LETRAS) {
      setSugestoes([]);
      return;
    }
    const controlador = new AbortController();
    cancelar.current = controlador;
    espera.current = window.setTimeout(() => {
      buscarEnderecos(termo, controlador.signal)
        .then(setSugestoes)
        .catch(() => setSugestoes([]));
    }, 350);
  };

  const dataIso = paraIso(data);
  const horaValida = /^\d{2}:\d{2}$/.test(hora);
  const podeSalvar = titulo.trim() !== '' && dataIso !== null && horaValida && !salvando;

  return (
    <Dialogo
      titulo={evento === null ? 'Novo evento' : 'Editar evento'}
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
              onSalvar(evento?.id ?? null, titulo, descricao, tipo, `${dataIso}T${hora}:00Z`, local)
            }
          >
            Salvar
          </button>
        </>
      }
    >
      <CampoTexto valor={titulo} rotulo="Título" onMudar={setTitulo} />
      <div className="linha" style={{ gap: 8 }}>
        <div className="expandir">
          <CampoTexto
            valor={data}
            rotulo="Data (dd/mm/aaaa)"
            onMudar={(valor) => setData(mascaraDeData(valor))}
          />
        </div>
        <div className="expandir">
          <CampoTexto
            valor={hora}
            rotulo="Hora"
            onMudar={(valor) => setHora(mascaraDeHora(valor))}
          />
        </div>
      </div>
      {data !== '' && dataIso === null && (
        <span className="subtitulo" style={{ fontSize: 11, color: 'var(--vermelho)' }}>
          Data inválida. Use dd/mm/aaaa.
        </span>
      )}

      <div className="campo">
        <CampoTexto
          valor={local}
          rotulo="Local"
          onMudar={(valor) => {
            setLocal(valor);
            setMostrarSugestoes(true);
            procurarEndereco(valor);
          }}
        />
        {mostrarSugestoes && sugestoes.length > 0 && (
          <div
            className="coluna"
            style={{
              maxHeight: 160,
              overflowY: 'auto',
              border: '1px solid var(--borda)',
              borderRadius: 10,
            }}
          >
            {sugestoes.map((sugestao) => (
              <button
                key={sugestao}
                type="button"
                className="subtitulo"
                style={{ padding: '8px 10px', textAlign: 'left', fontSize: 12 }}
                onClick={() => {
                  setLocal(sugestao);
                  setSugestoes([]);
                  setMostrarSugestoes(false);
                }}
              >
                {sugestao}
              </button>
            ))}
          </div>
        )}
        <span className="subtitulo" style={{ fontSize: 11 }}>
          Digite o endereço e toque em uma sugestão do mapa.
        </span>
      </div>

      <CampoTexto valor={descricao} rotulo="Descrição" onMudar={setDescricao} />
      <div className="campo">
        <span className="campo-rotulo">Tipo</span>
        <div className="linha" style={{ gap: 6, flexWrap: 'wrap' }}>
          {TIPOS_DE_EVENTO.map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="chip"
              aria-pressed={tipo === opcao}
              onClick={() => setTipo(opcao)}
            >
              {rotuloDoEvento(opcao)}
            </button>
          ))}
        </div>
      </div>
    </Dialogo>
  );
}

export function TextoFormatado({ corpo }: { corpo: string }) {
  return (
    <div className="coluna" style={{ gap: 8 }}>
      {corpo.split('\n').map((linha, indice) => {
        const texto = linha.trim();
        const chave = `${indice}-${texto.slice(0, 12)}`;
        if (texto === '') return null;
        if (texto.startsWith('# ')) {
          return (
            <strong key={chave} style={{ fontSize: 17, marginTop: 8 }}>
              {texto.slice(2)}
            </strong>
          );
        }
        if (texto.startsWith('- ')) {
          return (
            <div key={chave} className="linha" style={{ gap: 8, alignItems: 'flex-start' }}>
              <span style={{ color: 'var(--verde)' }}>•</span>
              <span className="subtitulo">{texto.slice(2).replaceAll('**', '')}</span>
            </div>
          );
        }
        return (
          <span key={chave} className="subtitulo">
            {texto.replaceAll('**', '')}
          </span>
        );
      })}
    </div>
  );
}

export function PaginaScreen({
  pagina,
  isAdmin,
  salvando,
  onVoltar,
  onSalvar,
}: {
  pagina: Pagina;
  isAdmin: boolean;
  salvando: boolean;
  onVoltar: () => void;
  onSalvar: (titulo: string, corpo: string) => void;
}) {
  const [editando, setEditando] = useState(false);
  const [titulo, setTitulo] = useState(pagina.titulo);
  const [corpo, setCorpo] = useState(pagina.corpo);

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna expandir">
          <span className="titulo-tela">{pagina.titulo}</span>
          <span className="subtitulo">{rotuloDaCategoria(pagina.categoria)}</span>
        </div>
        {isAdmin && (
          <button
            type="button"
            className="botao-texto"
            disabled={salvando}
            onClick={() => {
              if (editando) {
                onSalvar(titulo, corpo);
                setEditando(false);
              } else {
                setEditando(true);
              }
            }}
          >
            {editando ? 'Salvar' : 'Editar'}
          </button>
        )}
      </div>

      <div className="conteudo">
        <div className="coluna" style={{ padding: 16, gap: 12 }}>
          {editando ? (
            <>
              <CampoTexto valor={titulo} rotulo="Título" onMudar={setTitulo} />
              <label className="campo">
                <span className="campo-rotulo">Conteúdo</span>
                <textarea
                  className="campo-entrada"
                  rows={18}
                  value={corpo}
                  onChange={(e) => setCorpo(e.target.value)}
                />
              </label>
              <span className="subtitulo" style={{ fontSize: 11 }}>
                Use # no começo da linha para título e - para lista.
              </span>
            </>
          ) : (
            <TextoFormatado corpo={pagina.corpo} />
          )}
        </div>
      </div>
    </div>
  );
}
