import { LogoUpv } from '../components/Componentes';

export const LoginScreen = ({
  entrando,
  erro,
  onEntrar,
}: {
  entrando: boolean;
  erro: string | null;
  onEntrar: () => void;
}) => (
  <div
    className="coluna"
    style={{ height: '100%', alignItems: 'center', justifyContent: 'center', padding: 32, gap: 8 }}
  >
    <LogoUpv tamanho={128} />
    <h1 style={{ fontSize: 22, fontWeight: 900, letterSpacing: 0.6, margin: '24px 0 0' }}>
      UNIDOS PELO VÔLEI
    </h1>
    <p className="subtitulo" style={{ textAlign: 'center', margin: '8px 0 0', fontSize: 13 }}>
      Entre com a sua conta Google para ver os jogos e a classificação do grupo.
    </p>

    <button
      type="button"
      className="botao botao-primario"
      style={{ marginTop: 32 }}
      disabled={entrando}
      onClick={onEntrar}
    >
      {entrando ? 'Entrando...' : 'Entrar com Google'}
    </button>

    {erro && (
      <p className="erro" style={{ textAlign: 'center', marginTop: 16 }}>
        {erro}
      </p>
    )}
  </div>
);

export const ConfiguracaoPendenteScreen = ({
  chavesFaltando,
  ambiente,
}: {
  chavesFaltando: string[];
  ambiente: string;
}) => (
  <div
    className="coluna"
    style={{ height: '100%', alignItems: 'center', justifyContent: 'center', padding: 32 }}
  >
    <h1 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Configuração pendente</h1>
    <p className="subtitulo" style={{ textAlign: 'center', marginTop: 12, fontSize: 13 }}>
      Preencha as chaves abaixo no arquivo <code>.env</code> do ambiente <strong>{ambiente}</strong> e
      recompile:
    </p>
    {chavesFaltando.map((chave) => (
      <p key={chave} style={{ color: 'var(--dourado)', fontSize: 13, fontWeight: 700, margin: '6px 0 0' }}>
        {chave}
      </p>
    ))}
    <p className="terciario" style={{ textAlign: 'center', marginTop: 20, fontSize: 12 }}>
      O passo a passo está na seção "Aplicação web" do README.
    </p>
  </div>
);
