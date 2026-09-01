import { useEffect, type ReactNode } from 'react';
import { GENEROS, rotuloDoGenero, type Genero, type Team } from '../../domain/models';
import {
  IconeBusca,
  IconeEstrela,
  IconeFechar,
  IconeMais,
  IconeMenos,
  IconeNuvemOff,
  IconeNuvemOk,
  IconeNuvemSync,
  IconeSair,
} from './Icons';

export const corDoTime = (hex: string): string =>
  /^#[0-9a-f]{6}$/i.test(hex.trim()) ? hex.trim() : '#6B7280';

export const LogoUpv = ({ tamanho = 40 }: { tamanho?: number }) => (
  <img
    src="./logo-upv.png"
    alt=""
    width={tamanho}
    height={tamanho}
    style={{ flex: 'none', objectFit: 'contain' }}
  />
);

export const Cartao = ({
  children,
  apagado = false,
  className = '',
  onClick,
}: {
  children: ReactNode;
  apagado?: boolean;
  className?: string;
  onClick?: () => void;
}) => {
  const classes = `cartao ${apagado ? 'cartao-apagado' : ''} ${className}`.trim();
  if (onClick) {
    return (
      <div className={classes} onClick={onClick} role="button" tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick();
          }
        }}
      >
        {children}
      </div>
    );
  }
  return <div className={classes}>{children}</div>;
};

export const TeamCircle = ({
  sigla,
  corHex,
  tamanho = 52,
}: {
  sigla: string;
  corHex: string;
  tamanho?: number;
}) => (
  <div
    className="circulo-time"
    style={{
      width: tamanho,
      height: tamanho,
      background: corDoTime(corHex),
      fontSize: Math.round(tamanho * 0.36),
    }}
  >
    {sigla.toUpperCase()}
  </div>
);

export const TeamBadge = ({ team, tamanho = 52 }: { team: Team; tamanho?: number }) => (
  <div className="emblema-time" style={{ width: tamanho + 24 }}>
    <TeamCircle sigla={team.sigla} corHex={team.corHex} tamanho={tamanho} />
    <span className="emblema-nome">{team.nome.toUpperCase()}</span>
  </div>
);

export const PontoDoTime = ({ corHex, tamanho = 10 }: { corHex: string; tamanho?: number }) => (
  <span
    className="ponto-time"
    style={{ width: tamanho, height: tamanho, background: corDoTime(corHex) }}
  />
);

export const ScoreBox = ({ valor, destacado = false }: { valor: number | null; destacado?: boolean }) => (
  <div className={`placar-caixa ${destacado ? 'destacado' : ''} ${valor === null ? 'vazio' : ''}`}>
    {valor === null ? '-' : valor}
  </div>
);

export const Selo = ({
  texto,
  corTexto,
  corFundo,
}: {
  texto: string;
  corTexto: string;
  corFundo: string;
}) => (
  <span className="selo" style={{ color: corTexto, background: corFundo }}>
    {texto.toUpperCase()}
  </span>
);

export const SeloFase = ({ fase }: { fase: number }) => (
  <Selo texto={`Fase ${fase}`} corTexto="var(--selo-fase-texto)" corFundo="var(--selo-fase-fundo)" />
);

export const SeloResultado = ({ venceu }: { venceu: boolean }) =>
  venceu ? (
    <Selo texto="Vitoria" corTexto="var(--verde-claro)" corFundo="var(--selo-vitoria-fundo)" />
  ) : (
    <Selo texto="Derrota" corTexto="var(--vermelho)" corFundo="var(--selo-derrota-fundo)" />
  );

export const RotuloPequeno = ({ children, cor }: { children: ReactNode; cor?: string }) => (
  <span className="rotulo-pequeno" style={cor ? { color: cor } : undefined}>
    {children}
  </span>
);

export const EstadoVazio = ({ titulo, descricao }: { titulo: string; descricao: string }) => (
  <div className="vazio">
    <span className="vazio-titulo">{titulo}</span>
    <span className="subtitulo">{descricao}</span>
  </div>
);

export const Carregando = () => (
  <div className="centralizado">
    <div className="girando" role="status" aria-label="Carregando" />
  </div>
);

export const BotaoRedondo = ({
  children,
  descricao,
  onClick,
  habilitado = true,
  tamanho = 34,
  cor,
}: {
  children: ReactNode;
  descricao: string;
  onClick: () => void;
  habilitado?: boolean;
  tamanho?: number;
  cor?: string;
}) => (
  <button
    type="button"
    className="botao-redondo"
    aria-label={descricao}
    disabled={!habilitado}
    onClick={onClick}
    style={{ width: tamanho, height: tamanho, background: habilitado && cor ? cor : undefined }}
  >
    {children}
  </button>
);

export const Contador = ({
  valor,
  minimo,
  maximo,
  onMudar,
}: {
  valor: number;
  minimo: number;
  maximo: number;
  onMudar: (valor: number) => void;
}) => (
  <div className="linha" style={{ gap: 10 }}>
    <BotaoRedondo
      descricao="Diminuir"
      habilitado={valor > minimo}
      onClick={() => onMudar(Math.max(valor - 1, minimo))}
    >
      <IconeMenos tamanho={17} />
    </BotaoRedondo>
    <span style={{ minWidth: 28, textAlign: 'center', fontSize: 17, fontWeight: 700 }}>{valor}</span>
    <BotaoRedondo
      descricao="Aumentar"
      habilitado={valor < maximo}
      onClick={() => onMudar(Math.min(valor + 1, maximo))}
    >
      <IconeMais tamanho={17} />
    </BotaoRedondo>
  </div>
);

export const CampoTexto = ({
  valor,
  rotulo,
  onMudar,
  maiusculas = false,
  maxLength,
}: {
  valor: string;
  rotulo: string;
  onMudar: (valor: string) => void;
  maiusculas?: boolean;
  maxLength?: number;
}) => (
  <label className="campo">
    <span className="campo-rotulo">{rotulo}</span>
    <input
      className="campo-entrada"
      value={valor}
      maxLength={maxLength}
      onChange={(e) => onMudar(maiusculas ? e.target.value.toUpperCase() : e.target.value)}
    />
  </label>
);

export const CampoBusca = ({
  valor,
  onMudar,
  dica = 'Buscar',
}: {
  valor: string;
  onMudar: (valor: string) => void;
  dica?: string;
}) => (
  <div className="busca">
    <span className="busca-icone">
      <IconeBusca />
    </span>
    <input
      className="campo-entrada"
      value={valor}
      placeholder={dica}
      aria-label={dica}
      onChange={(e) => onMudar(e.target.value)}
    />
    {valor.length > 0 && (
      <button type="button" className="busca-limpar" aria-label="Limpar busca" onClick={() => onMudar('')}>
        <IconeFechar tamanho={16} />
      </button>
    )}
  </div>
);

export const Estrelas = ({
  nivel,
  tamanho = 14,
  onMudar,
}: {
  nivel: number;
  tamanho?: number;
  onMudar?: (valor: number) => void;
}) => (
  <div className="estrelas" style={{ gap: onMudar ? 8 : 2 }}>
    {[1, 2, 3, 4, 5].map((valor) =>
      onMudar ? (
        <button
          key={valor}
          type="button"
          className={`estrela ${valor <= nivel ? 'cheia' : ''}`}
          aria-label={`Nível ${valor}`}
          onClick={() => onMudar(valor)}
        >
          <IconeEstrela tamanho={tamanho} cheia={valor <= nivel} />
        </button>
      ) : (
        <span key={valor} className={`estrela ${valor <= nivel ? 'cheia' : ''}`}>
          <IconeEstrela tamanho={tamanho} cheia={valor <= nivel} />
        </span>
      ),
    )}
  </div>
);

export const Interruptor = ({
  ligado,
  onMudar,
  descricao,
}: {
  ligado: boolean;
  onMudar: (ligado: boolean) => void;
  descricao: string;
}) => (
  <button
    type="button"
    role="switch"
    aria-checked={ligado}
    aria-label={descricao}
    className="interruptor"
    onClick={() => onMudar(!ligado)}
  />
);

export const SeletorGenero = ({
  genero,
  onMudar,
}: {
  genero: Genero;
  onMudar: (genero: Genero) => void;
}) => (
  <div className="opcoes-genero">
    {GENEROS.map((opcao) => (
      <button
        key={opcao}
        type="button"
        className="opcao-genero"
        aria-pressed={opcao === genero}
        onClick={() => onMudar(opcao)}
      >
        <span className="radio" />
        {rotuloDoGenero(opcao)}
      </button>
    ))}
  </div>
);

export const CORES_DE_TIME = [
  '#2F80ED',
  '#E8590C',
  '#3F444D',
  '#E8437F',
  '#8B5CF6',
  '#16A34A',
  '#9CA3AF',
  '#E23B3B',
  '#EAB308',
  '#06B6D4',
];

export const SeletorCor = ({
  corSelecionada,
  onMudar,
}: {
  corSelecionada: string;
  onMudar: (cor: string) => void;
}) => (
  <div className="cores">
    {CORES_DE_TIME.map((hex) => (
      <button
        key={hex}
        type="button"
        className="cor"
        aria-pressed={hex.toLowerCase() === corSelecionada.toLowerCase()}
        aria-label={`Cor ${hex}`}
        style={{ background: hex }}
        onClick={() => onMudar(hex)}
      />
    ))}
  </div>
);

export const Dialogo = ({
  titulo,
  children,
  acoes,
  onFechar,
}: {
  titulo: ReactNode;
  children: ReactNode;
  acoes: ReactNode;
  onFechar: () => void;
}) => {
  useEffect(() => {
    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onFechar();
    };
    window.addEventListener('keydown', aoTeclar);
    return () => window.removeEventListener('keydown', aoTeclar);
  }, [onFechar]);

  return (
    <div className="fundo-modal" onClick={onFechar} role="presentation">
      <div className="dialogo" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div className="dialogo-titulo">{titulo}</div>
        {children}
        <div className="dialogo-acoes">{acoes}</div>
      </div>
    </div>
  );
};

export const DialogoConfirmacao = ({
  titulo,
  mensagem,
  textoConfirmar,
  onConfirmar,
  onCancelar,
  destrutivo = true,
}: {
  titulo: string;
  mensagem: string;
  textoConfirmar: string;
  onConfirmar: () => void;
  onCancelar: () => void;
  destrutivo?: boolean;
}) => (
  <Dialogo
    titulo={titulo}
    onFechar={onCancelar}
    acoes={
      <>
        <button type="button" className="botao-texto secundario" onClick={onCancelar}>
          Cancelar
        </button>
        <button
          type="button"
          className="botao-texto"
          style={{ color: destrutivo ? 'var(--vermelho)' : 'var(--verde)' }}
          onClick={onConfirmar}
        >
          {textoConfirmar}
        </button>
      </>
    }
  >
    <p className="secundario" style={{ margin: 0, fontSize: 14 }}>
      {mensagem}
    </p>
  </Dialogo>
);

export type SinalSync = 'online' | 'conectando' | 'offline';

export const AppHeader = ({
  subtitulo,
  sinal,
  onSair,
}: {
  subtitulo: string;
  sinal: SinalSync;
  onSair?: () => void;
}) => {
  const cor =
    sinal === 'online'
      ? 'var(--verde-claro)'
      : sinal === 'conectando'
        ? 'var(--dourado)'
        : 'var(--texto-terciario)';
  const rotulo = sinal === 'online' ? 'Online' : sinal === 'conectando' ? 'Conectando' : 'Offline';

  return (
    <header className="cabecalho">
      <div className="cabecalho-linha">
        <LogoUpv />
        <div className="expandir">
          <div className="cabecalho-titulo">UNIDOS PELO VÔLEI</div>
          <div className="cabecalho-subtitulo">{subtitulo}</div>
        </div>
        <span className="sinal-sync" style={{ color: cor }}>
          {sinal === 'online' ? <IconeNuvemOk /> : sinal === 'conectando' ? <IconeNuvemSync /> : <IconeNuvemOff />}
          {rotulo}
        </span>
        {onSair && (
          <button type="button" className="botao-icone" aria-label="Sair" onClick={onSair}>
            <IconeSair />
          </button>
        )}
      </div>
    </header>
  );
};

export const Aviso = ({ mensagem, onFechar }: { mensagem: string; onFechar: () => void }) => {
  useEffect(() => {
    const id = setTimeout(onFechar, 5000);
    return () => clearTimeout(id);
  }, [mensagem, onFechar]);

  return (
    <div className="aviso" role="status" onClick={onFechar}>
      {mensagem}
    </div>
  );
};
