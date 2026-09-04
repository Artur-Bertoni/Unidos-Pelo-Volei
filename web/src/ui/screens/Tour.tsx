import { useState } from 'react';

const CHAVE = 'upv.tour.visto';

export const jaViuOTour = (): boolean => {
  try {
    return window.localStorage.getItem(CHAVE) === '1';
  } catch {
    return true;
  }
};

export const marcarTourComoVisto = (): void => {
  try {
    window.localStorage.setItem(CHAVE, '1');
  } catch {
    /* navegador sem armazenamento: o tour volta na próxima visita */
  }
};

interface Passo {
  emoji: string;
  aba: string | null;
  titulo: string;
  texto: string;
}

const PASSOS: Passo[] = [
  {
    emoji: '📣',
    aba: 'SOCIAL',
    titulo: 'é onde o sábado começa',
    texto:
      'O mural traz os recados da diretoria, a agenda mostra jogos e confraternizações, e as ' +
      'regras do vôlei de areia ficam guardadas ali.',
  },
  {
    emoji: '🤝',
    aba: 'TIMES',
    titulo: 'monta o quarteto',
    texto:
      'A diretoria marca quem veio, sorteia os quartetos equilibrados e ativa os times do dia. ' +
      'Você acompanha o elenco de cada um.',
  },
  {
    emoji: '🏐',
    aba: 'JOGOS',
    titulo: 'tem o chaveamento e o placar',
    texto:
      'Cada rodada enche todas as quadras. Toque em uma partida para ver ou digitar o placar ' +
      'ponto a ponto.',
  },
  {
    emoji: '🏆',
    aba: 'CLASSIFICAÇÃO',
    titulo: 'fecha a conta',
    texto: 'Vitórias, saldo e pontos pró de todos os times do dia, atualizados na hora.',
  },
  {
    emoji: '🙋',
    aba: 'EU',
    titulo: 'é a sua parte',
    texto:
      'Confirme presença no sábado, escolha se paga mensalidade ou diária, veja o seu extrato ' +
      'com Pix e avalie os companheiros de time.',
  },
  {
    emoji: '✅',
    aba: null,
    titulo: 'Falta só achar você na lista',
    texto:
      'Escolha o seu nome entre os jogadores do grupo. A diretoria confirma e, a partir daí, a ' +
      'sua ficha e o seu histórico ficam aqui dentro.',
  },
];

export function TourScreen({ onConcluir }: { onConcluir: () => void }) {
  const [passo, setPasso] = useState(0);
  const atual = PASSOS[passo];
  const ultimo = passo === PASSOS.length - 1;

  return (
    <div
      className="coluna"
      style={{
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        padding: 28,
        textAlign: 'center',
      }}
    >
      <span
        aria-hidden="true"
        style={{
          fontSize: 40,
          width: 88,
          height: 88,
          borderRadius: '50%',
          background: 'var(--selo-fase-fundo)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {atual.emoji}
      </span>

      <p style={{ fontSize: 22, fontWeight: 500, margin: '16px 0 0' }}>
        {atual.aba !== null && (
          <strong style={{ fontWeight: 900, letterSpacing: 0.8 }}>{atual.aba} </strong>
        )}
        {atual.titulo}
      </p>
      <p className="subtitulo" style={{ margin: 0, fontSize: 14 }}>
        {atual.texto}
      </p>

      <div className="linha" style={{ gap: 6, marginTop: 16 }}>
        {PASSOS.map((item, indice) => (
          <span
            key={item.titulo}
            aria-hidden="true"
            style={{
              width: indice === passo ? 9 : 7,
              height: indice === passo ? 9 : 7,
              borderRadius: '50%',
              background: indice === passo ? 'var(--verde)' : 'var(--borda)',
            }}
          />
        ))}
      </div>

      <button
        type="button"
        className="botao botao-primario"
        style={{ marginTop: 16 }}
        onClick={() => (ultimo ? onConcluir() : setPasso(passo + 1))}
      >
        {ultimo ? 'Escolher meu nome' : 'Continuar'}
      </button>
      <button type="button" className="botao-texto secundario" onClick={onConcluir}>
        {ultimo ? 'Agora não' : 'Pular o tour'}
      </button>
    </div>
  );
}
