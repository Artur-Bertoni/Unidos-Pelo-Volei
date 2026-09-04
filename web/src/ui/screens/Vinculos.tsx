import { useEffect, useRef, useState } from 'react';
import type { Player } from '../../domain/models';
import { CampoBusca, Cartao, Dialogo, EstadoVazio, Selo } from '../components/Componentes';
import { IconeVoltar } from '../components/Icons';

export interface ContaDoGrupo {
  id: string;
  nome: string | null;
  email: string | null;
  isAdmin: boolean;
}

export const rotuloDaConta = (conta: ContaDoGrupo): string =>
  conta.nome?.trim() || conta.email || 'Conta sem nome';

export function VinculosScreen({
  jogadores,
  contas,
  carregandoContas,
  salvando,
  onVoltar,
  onCarregarContas,
  onVincular,
  onDesvincular,
}: {
  jogadores: Player[];
  contas: ContaDoGrupo[];
  carregandoContas: boolean;
  salvando: boolean;
  onVoltar: () => void;
  onCarregarContas: () => void;
  onVincular: (playerId: string, profileId: string) => void;
  onDesvincular: (playerId: string) => void;
}) {
  const [busca, setBusca] = useState('');
  const [escolhendoPara, setEscolhendoPara] = useState<Player | null>(null);
  const [desvinculando, setDesvinculando] = useState<Player | null>(null);

  const carregar = useRef(onCarregarContas);
  carregar.current = onCarregarContas;
  useEffect(() => {
    carregar.current();
  }, []);

  const porPerfil = new Map(contas.map((conta) => [conta.id, conta]));
  const termo = busca.trim().toLowerCase();
  const visiveis = jogadores
    .filter((jogador) => termo === '' || jogador.nome.toLowerCase().includes(termo))
    .sort(
      (a, b) =>
        Number(a.profileId !== null) - Number(b.profileId !== null) ||
        a.nome.toLowerCase().localeCompare(b.nome.toLowerCase()),
    );
  const vinculados = jogadores.filter((jogador) => jogador.profileId !== null).length;
  const livres = contas.filter((conta) => !jogadores.some((j) => j.profileId === conta.id));

  return (
    <div className="coluna" style={{ height: '100%' }}>
      <div className="linha" style={{ padding: 8, flex: 'none' }}>
        <button type="button" className="botao-icone" aria-label="Voltar" onClick={onVoltar}>
          <IconeVoltar />
        </button>
        <div className="coluna">
          <span className="titulo-tela">Vincular jogadores</span>
          <span className="subtitulo">
            {`${vinculados} de ${jogadores.length} com conta`}
            {carregandoContas ? ' · carregando contas…' : ''}
          </span>
        </div>
      </div>

      <div style={{ padding: '0 16px', flex: 'none' }}>
        <CampoBusca valor={busca} onMudar={setBusca} dica="Buscar jogador pelo nome" />
      </div>

      <div className="conteudo">
        {visiveis.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum jogador"
            descricao="Cadastre os jogadores do grupo antes de ligar as contas."
          />
        ) : (
          <div className="lista" style={{ padding: 16, gap: 8 }}>
            {visiveis.map((jogador) => {
              const conta = jogador.profileId ? porPerfil.get(jogador.profileId) : undefined;
              const ligado = jogador.profileId !== null;
              return (
                <Cartao key={jogador.id}>
                  <div className="linha" style={{ padding: '12px 14px', gap: 12 }}>
                    <div className="coluna expandir" style={{ gap: 2 }}>
                      <strong>{jogador.nome}</strong>
                      <span
                        className="subtitulo"
                        style={{
                          fontSize: 12,
                          color: ligado ? 'var(--verde-claro)' : 'var(--texto-terciario)',
                        }}
                      >
                        {!ligado
                          ? 'Sem conta ligada'
                          : conta
                            ? rotuloDaConta(conta)
                            : 'Conta ligada'}
                      </span>
                    </div>
                    <button
                      type="button"
                      className="botao-texto"
                      disabled={salvando}
                      style={{ color: ligado ? 'var(--vermelho)' : 'var(--azul)' }}
                      onClick={() =>
                        ligado ? setDesvinculando(jogador) : setEscolhendoPara(jogador)
                      }
                    >
                      {ligado ? 'Desvincular' : 'Vincular'}
                    </button>
                  </div>
                </Cartao>
              );
            })}
          </div>
        )}
      </div>

      {escolhendoPara && (
        <Dialogo
          titulo={`Conta de ${escolhendoPara.nome}`}
          onFechar={() => setEscolhendoPara(null)}
          acoes={
            <button
              type="button"
              className="botao-texto secundario"
              onClick={() => setEscolhendoPara(null)}
            >
              Fechar
            </button>
          }
        >
          {carregandoContas ? (
            <span className="subtitulo">Carregando as contas que já entraram no app…</span>
          ) : livres.length === 0 ? (
            <span className="subtitulo">
              Nenhuma conta livre. Quem for usar o app precisa entrar com o Google pelo menos uma
              vez para aparecer aqui.
            </span>
          ) : (
            <div className="coluna" style={{ gap: 4 }}>
              {livres.map((conta) => (
                <button
                  key={conta.id}
                  type="button"
                  className="linha"
                  style={{ padding: '10px 0', textAlign: 'left', gap: 8 }}
                  onClick={() => {
                    onVincular(escolhendoPara.id, conta.id);
                    setEscolhendoPara(null);
                  }}
                >
                  <div className="coluna expandir" style={{ gap: 2 }}>
                    <strong style={{ fontSize: 14 }}>{rotuloDaConta(conta)}</strong>
                    {conta.email && conta.email !== rotuloDaConta(conta) && (
                      <span className="terciario" style={{ fontSize: 11 }}>
                        {conta.email}
                      </span>
                    )}
                  </div>
                  {conta.isAdmin && (
                    <Selo
                      texto="Diretoria"
                      corTexto="var(--selo-fase-texto)"
                      corFundo="var(--selo-fase-fundo)"
                    />
                  )}
                </button>
              ))}
            </div>
          )}
        </Dialogo>
      )}

      {desvinculando && (
        <Dialogo
          titulo={`Desvincular ${desvinculando.nome}?`}
          onFechar={() => setDesvinculando(null)}
          acoes={
            <>
              <button
                type="button"
                className="botao-texto secundario"
                onClick={() => setDesvinculando(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="botao-texto"
                style={{ color: 'var(--vermelho)' }}
                onClick={() => {
                  onDesvincular(desvinculando.id);
                  setDesvinculando(null);
                }}
              >
                Desvincular
              </button>
            </>
          }
        >
          <span className="subtitulo">
            A conta perde o acesso à ficha, ao extrato e às avaliações desse jogador. Os dados do
            jogador continuam no grupo.
          </span>
        </Dialogo>
      )}
    </div>
  );
}
