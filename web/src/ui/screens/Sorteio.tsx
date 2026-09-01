import { forcaTotal, homensDo, mulheresDo, type TeamRoster } from '../../domain/models';
import { Cartao, EstadoVazio, TeamCircle } from '../components/Componentes';
import { IconeVoltar } from '../components/Icons';

export const SorteioScreen = ({
  previa,
  salvando,
  onVoltar,
  onRecalcular,
  onAplicar,
}: {
  previa: TeamRoster[] | null;
  salvando: boolean;
  onVoltar: () => void;
  onRecalcular: () => void;
  onAplicar: () => void;
}) => {
  const forcas = previa?.map(forcaTotal) ?? [];

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Distribuição dos times</span>
          <span className="subtitulo">Alvo de 2 homens e 2 mulheres por time</span>
        </div>
      </div>

      <div className="conteudo">
        {previa === null ? (
          <EstadoVazio titulo="Nada sorteado ainda" descricao="Toque em Sortear para montar os times." />
        ) : (
          <div className="lista lista-compacta">
            <span className="subtitulo">
              {previa.reduce((soma, e) => soma + e.players.length, 0)} jogadores presentes (
              {previa.reduce((soma, e) => soma + homensDo(e), 0)}H /{' '}
              {previa.reduce((soma, e) => soma + mulheresDo(e), 0)}M) • força de{' '}
              {forcas.length > 0 ? Math.min(...forcas) : 0} a {forcas.length > 0 ? Math.max(...forcas) : 0}
            </span>
            {previa.map((elenco) => (
              <CartaoElenco key={elenco.team.id} elenco={elenco} />
            ))}
          </div>
        )}
      </div>

      <div className="linha" style={{ padding: 16, gap: 10, flex: 'none' }}>
        <button type="button" className="botao botao-contorno" onClick={onRecalcular}>
          {previa === null ? 'Sortear' : 'Sortear de novo'}
        </button>
        <button
          type="button"
          className="botao botao-primario"
          disabled={previa === null || salvando}
          onClick={onAplicar}
        >
          {salvando ? 'Salvando...' : 'Aplicar'}
        </button>
      </div>
    </div>
  );
};

const CartaoElenco = ({ elenco }: { elenco: TeamRoster }) => (
  <Cartao>
    <div className="coluna" style={{ padding: 12, gap: 8 }}>
      <div className="linha" style={{ gap: 10 }}>
        <TeamCircle sigla={elenco.team.sigla} corHex={elenco.team.corHex} tamanho={34} />
        <span className="expandir" style={{ fontSize: 14, fontWeight: 700 }}>
          {elenco.team.nome}
        </span>
        <span style={{ color: 'var(--selo-fase-texto)', fontSize: 12, fontWeight: 700 }}>
          {homensDo(elenco)}H/{mulheresDo(elenco)}M
        </span>
        <span style={{ color: 'var(--verde-claro)', fontSize: 12, fontWeight: 700 }}>
          força {forcaTotal(elenco)}
        </span>
      </div>
      <span className="subtitulo">
        {elenco.players
          .map((jogador) => `${jogador.nome} (${jogador.skillLevel}${jogador.genero === 'feminino' ? 'F' : 'M'})`)
          .join(', ')}
      </span>
    </div>
  </Cartao>
);
