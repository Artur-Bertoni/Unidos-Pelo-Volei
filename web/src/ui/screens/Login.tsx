import { useEffect, useRef, useState } from 'react';
import { mostrarBotaoDoGoogle } from '../../lib/google';
import { LogoUpv } from '../components/Componentes';

export const LoginScreen = ({
  entrando,
  erro,
  tentativa,
  onToken,
}: {
  entrando: boolean;
  erro: string | null;
  tentativa: number;
  onToken: (idToken: string, nonce: string) => void;
}) => {
  const lugarDoBotao = useRef<HTMLDivElement>(null);
  const aoReceber = useRef(onToken);
  aoReceber.current = onToken;
  const [falhaAoCarregar, setFalhaAoCarregar] = useState<string | null>(null);

  useEffect(() => {
    const elemento = lugarDoBotao.current;
    if (!elemento) return;
    setFalhaAoCarregar(null);
    mostrarBotaoDoGoogle(elemento, (idToken, nonce) => aoReceber.current(idToken, nonce)).catch((falha: unknown) =>
      setFalhaAoCarregar(falha instanceof Error ? falha.message : 'Não foi possível carregar o login do Google.'),
    );
  }, [tentativa]);

  const mensagem = erro ?? falhaAoCarregar;

  return (
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

      <div
        ref={lugarDoBotao}
        style={{ marginTop: 32, minHeight: 44, display: entrando ? 'none' : 'flex', justifyContent: 'center' }}
      />
      {entrando && (
        <p className="subtitulo" style={{ marginTop: 32 }}>
          Entrando...
        </p>
      )}

      {mensagem && (
        <p className="erro" style={{ textAlign: 'center', marginTop: 16 }}>
          {mensagem}
        </p>
      )}
    </div>
  );
};

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
