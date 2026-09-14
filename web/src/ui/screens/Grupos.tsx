import { useEffect, useRef, useState } from 'react';
import {
  codigoFormatado,
  iniciaisDoGrupo,
  limparCodigo,
  rotuloDoMembro,
  type ChaveDeAcesso,
  type MembroDoGrupo,
  type MeuGrupo,
  type Papel,
} from '../../domain/models';
import {
  Cartao,
  CampoTexto,
  Dialogo,
  DialogoConfirmacao,
  EstadoVazio,
  Selo,
} from '../components/Componentes';
import { IconeVoltar } from '../components/Icons';

export const LogoDoGrupo = ({
  grupo,
  tamanho = 40,
}: {
  grupo: { nome: string; logoUrl: string | null };
  tamanho?: number;
}) => {
  const estilo = {
    width: tamanho,
    height: tamanho,
    borderRadius: '50%',
    flex: 'none' as const,
    objectFit: 'cover' as const,
    background: 'var(--cartao-interno)',
  };

  if (grupo.logoUrl) {
    return <img src={grupo.logoUrl} alt="" style={estilo} />;
  }

  return (
    <span
      style={{
        ...estilo,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'var(--texto-secundario)',
        fontWeight: 700,
        fontSize: tamanho / 2.6,
      }}
    >
      {iniciaisDoGrupo(grupo.nome)}
    </span>
  );
};

const PAPEIS: { valor: Papel; rotulo: string }[] = [
  { valor: 'atleta', rotulo: 'Atleta' },
  { valor: 'diretoria', rotulo: 'Diretoria' },
];

const SeletorDePapel = ({
  papel,
  onMudar,
}: {
  papel: Papel;
  onMudar: (papel: Papel) => void;
}) => (
  <div className="linha" style={{ gap: 8 }}>
    {PAPEIS.map((opcao) => (
      <button
        key={opcao.valor}
        type="button"
        className="chip"
        aria-pressed={papel === opcao.valor}
        onClick={() => onMudar(opcao.valor)}
      >
        {opcao.rotulo}
      </button>
    ))}
  </div>
);

export function GruposScreen({
  grupos,
  grupoAtual,
  salvando,
  onVoltar,
  onSelecionar,
  onEntrarComChave,
  onCriarGrupo,
  onSair,
  chaveSugerida,
}: {
  grupos: MeuGrupo[];
  grupoAtual: MeuGrupo | null;
  salvando: boolean;
  onVoltar: (() => void) | null;
  onSelecionar: (grupo: MeuGrupo) => void;
  onEntrarComChave: (codigo: string) => void;
  onCriarGrupo: (nome: string, cidade: string | null) => void;
  onSair: (grupo: MeuGrupo) => void;
  chaveSugerida?: string | null;
}) {
  const [entrando, setEntrando] = useState(chaveSugerida != null && chaveSugerida.length > 0);
  const [criando, setCriando] = useState(false);
  const [saindoDe, setSaindoDe] = useState<MeuGrupo | null>(null);

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        {onVoltar && (
          <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
            <IconeVoltar />
          </button>
        )}
        <div className="coluna expandir" style={{ gap: 2, paddingLeft: onVoltar ? 0 : 12 }}>
          <span className="titulo-tela">Meus grupos</span>
          <span className="subtitulo">
            {grupos.length === 0
              ? 'Entre com a chave que a diretoria passou'
              : 'Toque para trocar o grupo que você está usando'}
          </span>
        </div>
      </div>

      {grupos.length === 0 && (
        <EstadoVazio
          titulo="Você ainda não está em nenhum grupo"
          descricao="Cada grupo de jogo tem os seus jogadores, times, placar e financeiro. Peça a chave de acesso a quem organiza, ou crie o seu grupo agora."
        />
      )}

      <div className="lista expandir" style={{ padding: 16, gap: 10, overflowY: 'auto' }}>
        {grupos.map((grupo) => (
          <Cartao key={grupo.id}>
            <div className="linha" style={{ padding: 14, gap: 12 }}>
              <LogoDoGrupo grupo={grupo} tamanho={36} />
              <button
                type="button"
                className="coluna expandir"
                style={{
                  gap: 3,
                  background: 'none',
                  border: 'none',
                  textAlign: 'left',
                  cursor: 'pointer',
                  padding: 0,
                }}
                onClick={() => onSelecionar(grupo)}
              >
                <span className="titulo-tela">{grupo.nome}</span>
                <span className="subtitulo">
                  {[grupo.cidade, grupo.papel === 'diretoria' ? 'Você é da diretoria' : 'Você é atleta']
                    .filter(Boolean)
                    .join(' · ')}
                </span>
              </button>
              {grupo.id === grupoAtual?.id && (
                <Selo texto="Em uso" corTexto="var(--verde-claro)" corFundo="var(--selo-vitoria-fundo)" />
              )}
              <button
                type="button"
                className="botao-texto secundario"
                onClick={() => setSaindoDe(grupo)}
              >
                Sair
              </button>
            </div>
          </Cartao>
        ))}
      </div>
      <div className="coluna" style={{ padding: 16, gap: 10, flex: 'none' }}>
        <button
          type="button"
          className="botao botao-primario"
          disabled={salvando}
          onClick={() => setEntrando(true)}
        >
          Entrar com uma chave
        </button>
        <button
          type="button"
          className="botao botao-contorno"
          disabled={salvando}
          onClick={() => setCriando(true)}
        >
          Criar um grupo
        </button>
      </div>

      {entrando && (
        <EntrarNoGrupoDialogo
          codigoInicial={chaveSugerida ?? ''}
          salvando={salvando}
          onEntrar={(codigo) => {
            onEntrarComChave(codigo);
            setEntrando(false);
          }}
          onFechar={() => setEntrando(false)}
        />
      )}

      {criando && (
        <CriarGrupoDialogo
          salvando={salvando}
          onCriar={(nome, cidade) => {
            onCriarGrupo(nome, cidade);
            setCriando(false);
          }}
          onFechar={() => setCriando(false)}
        />
      )}

      {saindoDe && (
        <DialogoConfirmacao
          titulo={`Sair do ${saindoDe.nome}?`}
          mensagem="Você perde o acesso ao grupo neste aparelho e a sua ficha fica sem dono. Para voltar, vai precisar da chave de acesso de novo."
          textoConfirmar="Sair do grupo"
          onConfirmar={() => {
            onSair(saindoDe);
            setSaindoDe(null);
          }}
          onCancelar={() => setSaindoDe(null)}
        />
      )}
    </div>
  );
}

function EntrarNoGrupoDialogo({
  codigoInicial,
  salvando,
  onEntrar,
  onFechar,
}: {
  codigoInicial: string;
  salvando: boolean;
  onEntrar: (codigo: string) => void;
  onFechar: () => void;
}) {
  const [codigo, setCodigo] = useState(limparCodigo(codigoInicial));
  const limpo = limparCodigo(codigo);

  return (
    <Dialogo
      titulo="Entrar em um grupo"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={salvando || limpo.length < 4}
            onClick={() => onEntrar(limpo)}
          >
            Entrar
          </button>
        </>
      }
    >
      <div className="coluna" style={{ gap: 12 }}>
        <span className="subtitulo">
          A chave de acesso é um código de 8 letras e números que a diretoria do grupo manda para
          quem vai entrar.
        </span>
        <CampoTexto valor={codigo} rotulo="Chave de acesso" onMudar={setCodigo} maiusculas />
      </div>
    </Dialogo>
  );
}

export function EditarGrupoDialogo({
  grupo,
  salvando,
  onSalvar,
  onFechar,
}: {
  grupo: MeuGrupo;
  salvando: boolean;
  onSalvar: (nome: string, cidade: string | null, logo: File | null, remover: boolean) => void;
  onFechar: () => void;
}) {
  const [nome, setNome] = useState(grupo.nome);
  const [cidade, setCidade] = useState(grupo.cidade ?? '');
  const [logo, setLogo] = useState<File | null>(null);
  const [remover, setRemover] = useState(false);
  const escolher = useRef<HTMLInputElement>(null);

  const previa = logo ? URL.createObjectURL(logo) : remover ? null : grupo.logoUrl;

  return (
    <Dialogo
      titulo="Nome e logo do grupo"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={salvando || nome.trim().length === 0}
            onClick={() => onSalvar(nome.trim(), cidade.trim() || null, logo, remover)}
          >
            Salvar
          </button>
        </>
      }
    >
      <div className="coluna" style={{ gap: 12 }}>
        <div className="linha" style={{ gap: 12 }}>
          <LogoDoGrupo grupo={{ nome, logoUrl: previa }} tamanho={52} />
          <div className="coluna" style={{ gap: 2 }}>
            <button
              type="button"
              className="botao-texto"
              onClick={() => escolher.current?.click()}
            >
              {previa ? 'Trocar logo' : 'Escolher logo'}
            </button>
            {previa && (
              <button
                type="button"
                className="botao-texto"
                style={{ color: 'var(--vermelho)' }}
                onClick={() => {
                  setLogo(null);
                  setRemover(true);
                }}
              >
                Remover logo
              </button>
            )}
          </div>
        </div>

        <input
          ref={escolher}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          onChange={(evento) => {
            const arquivo = evento.target.files?.[0] ?? null;
            if (arquivo) {
              setLogo(arquivo);
              setRemover(false);
            }
          }}
        />

        <CampoTexto valor={nome} rotulo="Nome do grupo" onMudar={setNome} />
        <CampoTexto valor={cidade} rotulo="Cidade (opcional)" onMudar={setCidade} />
      </div>
    </Dialogo>
  );
}

function CriarGrupoDialogo({
  salvando,
  onCriar,
  onFechar,
}: {
  salvando: boolean;
  onCriar: (nome: string, cidade: string | null) => void;
  onFechar: () => void;
}) {
  const [nome, setNome] = useState('');
  const [cidade, setCidade] = useState('');

  return (
    <Dialogo
      titulo="Criar um grupo"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={salvando || nome.trim().length === 0}
            onClick={() => onCriar(nome.trim(), cidade.trim() || null)}
          >
            Criar grupo
          </button>
        </>
      }
    >
      <div className="coluna" style={{ gap: 12 }}>
        <span className="subtitulo">
          Você entra como diretoria e o grupo já nasce com os times coloridos, as páginas de regras
          e uma chave de acesso para convidar a galera.
        </span>
        <CampoTexto valor={nome} rotulo="Nome do grupo" onMudar={setNome} />
        <CampoTexto valor={cidade} rotulo="Cidade (opcional)" onMudar={setCidade} />
      </div>
    </Dialogo>
  );
}

const convite = (nomeDoGrupo: string, codigo: string): string =>
  `Entra no nosso grupo do UP Vôlei! Abra o app, entre com o Google e use a chave ${codigo} para entrar no ${nomeDoGrupo}.`;

export function ChavesScreen({
  nomeDoGrupo,
  chaves,
  carregando,
  salvando,
  onVoltar,
  onCarregar,
  onCriar,
  onAlternar,
  onExcluir,
  falha,
}: {
  nomeDoGrupo: string;
  chaves: ChaveDeAcesso[];
  carregando: boolean;
  salvando: boolean;
  onVoltar: () => void;
  onCarregar: () => void;
  onCriar: (rotulo: string | null, papel: Papel, usosMax: number | null, expiraEm: string | null) => void;
  onAlternar: (chave: ChaveDeAcesso) => void;
  onExcluir: (chave: ChaveDeAcesso) => void;
  falha?: string | null;
}) {
  const [criando, setCriando] = useState(false);
  const [excluindo, setExcluindo] = useState<ChaveDeAcesso | null>(null);
  const [copiada, setCopiada] = useState<string | null>(null);

  const carregar = useRef(onCarregar);
  carregar.current = onCarregar;
  useEffect(() => {
    carregar.current();
  }, []);

  const compartilhar = async (chave: ChaveDeAcesso): Promise<void> => {
    const texto = convite(nomeDoGrupo, codigoFormatado(chave.codigo));
    if (typeof navigator.share === 'function') {
      await navigator.share({ text: texto }).catch(() => undefined);
      return;
    }
    await navigator.clipboard?.writeText(texto).catch(() => undefined);
    setCopiada(chave.id);
    setTimeout(() => setCopiada(null), 2000);
  };

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna expandir" style={{ gap: 2 }}>
          <span className="titulo-tela">Chaves de acesso</span>
          <span className="subtitulo">Quem tem a chave entra no {nomeDoGrupo}</span>
        </div>
        <button
          type="button"
          className="botao-texto"
          disabled={salvando}
          onClick={() => setCriando(true)}
        >
          Nova
        </button>
      </div>

      {chaves.length === 0 && !carregando && (
        <EstadoVazio
          titulo={falha ? 'Não deu para ler as chaves' : 'Nenhuma chave por aqui'}
          descricao={
            falha ??
            'Crie uma chave e mande para quem vai entrar. Você pode desligar a chave a qualquer momento, sem tirar ninguém que já entrou.'
          }
        />
      )}

      <div className="lista expandir" style={{ padding: 16, gap: 10, overflowY: 'auto' }}>
        {chaves.map((chave) => (
          <Cartao key={chave.id}>
            <div className="coluna" style={{ padding: 14, gap: 8 }}>
              <div className="linha" style={{ gap: 8 }}>
                <span
                  className="expandir"
                  style={{ fontSize: 20, fontWeight: 800, letterSpacing: 2 }}
                >
                  {codigoFormatado(chave.codigo)}
                </span>
                <button
                  type="button"
                  className="botao-texto secundario"
                  onClick={() => void compartilhar(chave)}
                >
                  {copiada === chave.id ? 'Copiado' : 'Convidar'}
                </button>
                <button
                  type="button"
                  className="botao-texto"
                  style={{ color: 'var(--vermelho)' }}
                  disabled={salvando}
                  onClick={() => setExcluindo(chave)}
                >
                  Apagar
                </button>
              </div>
              <div className="linha" style={{ gap: 8 }}>
                {chave.papel === 'diretoria' && (
                  <Selo
                    texto="Entra como diretoria"
                    corTexto="var(--dourado)"
                    corFundo="var(--selo-fase-fundo)"
                  />
                )}
                <span className="subtitulo expandir">
                  {[
                    chave.rotulo,
                    chave.usosMax === null
                      ? `${chave.usos} usos`
                      : `${chave.usos} de ${chave.usosMax} usos`,
                    chave.expiraEm ? `vence em ${chave.expiraEm.slice(0, 10)}` : null,
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
                <button
                  type="button"
                  className="interruptor"
                  role="switch"
                  aria-checked={chave.ativa}
                  aria-label={chave.ativa ? 'Desligar chave' : 'Ligar chave'}
                  disabled={salvando}
                  onClick={() => onAlternar(chave)}
                />
              </div>
            </div>
          </Cartao>
        ))}
      </div>

      {criando && (
        <NovaChaveDialogo
          salvando={salvando}
          onCriar={(rotulo, papel, usos, expira) => {
            onCriar(rotulo, papel, usos, expira);
            setCriando(false);
          }}
          onFechar={() => setCriando(false)}
        />
      )}

      {excluindo && (
        <DialogoConfirmacao
          titulo={`Apagar a chave ${codigoFormatado(excluindo.codigo)}?`}
          mensagem="Quem já entrou com ela continua no grupo. A chave é que deixa de funcionar."
          textoConfirmar="Apagar"
          onConfirmar={() => {
            onExcluir(excluindo);
            setExcluindo(null);
          }}
          onCancelar={() => setExcluindo(null)}
        />
      )}
    </div>
  );
}

function NovaChaveDialogo({
  salvando,
  onCriar,
  onFechar,
}: {
  salvando: boolean;
  onCriar: (rotulo: string | null, papel: Papel, usosMax: number | null, expiraEm: string | null) => void;
  onFechar: () => void;
}) {
  const [rotulo, setRotulo] = useState('');
  const [papel, setPapel] = useState<Papel>('atleta');
  const [usos, setUsos] = useState('');
  const [dias, setDias] = useState('');

  const soDigitos = (valor: string): string => valor.replace(/\D/g, '').slice(0, 4);

  const vencimento = (): string | null => {
    const quantos = Number(dias);
    if (!Number.isFinite(quantos) || quantos <= 0) return null;
    const alvo = new Date();
    alvo.setDate(alvo.getDate() + quantos);
    return alvo.toISOString();
  };

  return (
    <Dialogo
      titulo="Nova chave"
      onFechar={onFechar}
      acoes={
        <>
          <button type="button" className="botao-texto secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button
            type="button"
            className="botao-texto"
            disabled={salvando}
            onClick={() =>
              onCriar(rotulo.trim() || null, papel, Number(usos) > 0 ? Number(usos) : null, vencimento())
            }
          >
            Criar chave
          </button>
        </>
      }
    >
      <div className="coluna" style={{ gap: 12 }}>
        <CampoTexto
          valor={rotulo}
          rotulo="Para que é esta chave (opcional)"
          onMudar={setRotulo}
        />
        <span className="subtitulo">Quem entrar com ela vira:</span>
        <SeletorDePapel papel={papel} onMudar={setPapel} />
        <CampoTexto
          valor={usos}
          rotulo="Limite de usos (vazio = sem limite)"
          onMudar={(valor) => setUsos(soDigitos(valor))}
        />
        <CampoTexto
          valor={dias}
          rotulo="Vence em quantos dias (vazio = nunca)"
          onMudar={(valor) => setDias(soDigitos(valor))}
        />
      </div>
    </Dialogo>
  );
}

export function MembrosScreen({
  nomeDoGrupo,
  membros,
  meuId,
  carregando,
  salvando,
  onVoltar,
  onCarregar,
  onDefinirPapel,
  onRemover,
  falha,
}: {
  nomeDoGrupo: string;
  membros: MembroDoGrupo[];
  meuId: string | undefined;
  carregando: boolean;
  salvando: boolean;
  onVoltar: () => void;
  onCarregar: () => void;
  onDefinirPapel: (membro: MembroDoGrupo, papel: Papel) => void;
  onRemover: (membro: MembroDoGrupo) => void;
  falha?: string | null;
}) {
  const [removendo, setRemovendo] = useState<MembroDoGrupo | null>(null);

  const carregar = useRef(onCarregar);
  carregar.current = onCarregar;
  useEffect(() => {
    carregar.current();
  }, []);

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna expandir" style={{ gap: 2 }}>
          <span className="titulo-tela">Membros do grupo</span>
          <span className="subtitulo">
            {carregando ? 'Carregando...' : `${membros.length} contas no ${nomeDoGrupo}`}
          </span>
        </div>
      </div>

      {membros.length === 0 && !carregando && (
        <EstadoVazio
          titulo={falha ? 'Não deu para ler os membros' : 'Ninguém por aqui ainda'}
          descricao={falha ?? 'Mande a chave de acesso para a galera entrar no grupo.'}
        />
      )}

      <div className="lista expandir" style={{ padding: 16, gap: 10, overflowY: 'auto' }}>
        {membros.map((membro) => (
          <Cartao key={membro.profileId}>
            <div className="coluna" style={{ padding: 14, gap: 8 }}>
              <div className="linha" style={{ gap: 8 }}>
                <div className="coluna expandir" style={{ gap: 2 }}>
                  <span className="titulo-tela">{rotuloDoMembro(membro)}</span>
                  {membro.email && membro.email !== rotuloDoMembro(membro) && (
                    <span className="rotulo-pequeno">{membro.email}</span>
                  )}
                </div>
                {membro.profileId !== meuId && (
                  <button
                    type="button"
                    className="botao-texto"
                    style={{ color: 'var(--vermelho)' }}
                    disabled={salvando}
                    onClick={() => setRemovendo(membro)}
                  >
                    Tirar
                  </button>
                )}
              </div>
              <SeletorDePapel
                papel={membro.papel}
                onMudar={(papel) => onDefinirPapel(membro, papel)}
              />
            </div>
          </Cartao>
        ))}
      </div>

      {removendo && (
        <DialogoConfirmacao
          titulo={`Tirar ${rotuloDoMembro(removendo)} do grupo?`}
          mensagem="A conta perde o acesso ao grupo. A ficha do jogador continua aqui, só fica sem dono."
          textoConfirmar="Tirar do grupo"
          onConfirmar={() => {
            onRemover(removendo);
            setRemovendo(null);
          }}
          onCancelar={() => setRemovendo(null)}
        />
      )}
    </div>
  );
}
