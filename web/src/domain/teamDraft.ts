import type { Player, Team, TeamRoster } from './models';
import { defaultRandom, minBy, randomElement, shuffled, sortedBy, type Rng } from './random';

export interface ElencoPassado {
  jogadores: string[];
  peso: number;
}

export class HistoricoDeDuplas {
  static readonly VAZIO = new HistoricoDeDuplas(new Map());

  private constructor(private readonly pesos: Map<string, Map<string, number>>) {}

  static de(elencos: readonly ElencoPassado[]): HistoricoDeDuplas {
    const acumulado = new Map<string, Map<string, number>>();
    const somar = (a: string, b: string, peso: number): void => {
      const linha = acumulado.get(a) ?? new Map<string, number>();
      linha.set(b, (linha.get(b) ?? 0) + peso);
      acumulado.set(a, linha);
    };

    elencos.forEach((elenco) => {
      for (let i = 0; i < elenco.jogadores.length; i++) {
        for (let j = i + 1; j < elenco.jogadores.length; j++) {
          somar(elenco.jogadores[i], elenco.jogadores[j], elenco.peso);
          somar(elenco.jogadores[j], elenco.jogadores[i], elenco.peso);
        }
      }
    });
    return new HistoricoDeDuplas(acumulado);
  }

  peso(a: string, b: string): number {
    return this.pesos.get(a)?.get(b) ?? 0;
  }

  penalidade(jogadorId: string, elenco: readonly Player[]): number {
    return elenco.reduce((soma, jogador) => soma + this.peso(jogadorId, jogador.id), 0);
  }

  repeticoes(elencos: readonly (readonly Player[])[]): number {
    return elencos.reduce((total, elenco) => {
      let soma = 0;
      for (let i = 0; i < elenco.length; i++) {
        for (let j = i + 1; j < elenco.length; j++) soma += this.peso(elenco[i].id, elenco[j].id);
      }
      return total + soma;
    }, 0);
  }
}

export const ALVO_POR_GENERO = 2;
export const JOGADORES_POR_TIME = ALVO_POR_GENERO * 2;
export const TENTATIVAS_PADRAO = 240;

const PESO_HISTORICO_NA_ESCOLHA = 2.5;
const RUIDO_NA_ESCOLHA = 1.5;

const PESO_AMPLITUDE = 1.0;
const PESO_REPETICAO = 2.5;
const PESO_GENERO = 12.0;

const forcaDe = (elenco: readonly Player[]): number =>
  elenco.reduce((soma, jogador) => soma + jogador.skillLevel, 0);

function capacidades(total: number, totalTimes: number, random: Rng): number[] {
  const capacidade = new Array<number>(totalTimes).fill(Math.floor(total / totalTimes));
  const indices = shuffled(
    Array.from({ length: totalTimes }, (_, i) => i),
    random,
  ).slice(0, total % totalTimes);
  indices.forEach((indice) => {
    capacidade[indice] += 1;
  });
  return capacidade;
}

function escolher(random: Rng, candidatos: readonly number[], chave: (i: number) => number): number | null {
  if (candidatos.length === 0) return null;
  const melhor = Math.max(...candidatos.map(chave));
  return randomElement(
    candidatos.filter((candidato) => chave(candidato) === melhor),
    random,
  );
}

function cotasPorGenero(
  capacidade: readonly number[],
  totalDeMulheres: number,
  random: Rng,
): { feminino: number[]; masculino: number[] } {
  const n = capacidade.length;
  const feminino = capacidade.map((vagas) => Math.min(ALVO_POR_GENERO, vagas));
  const masculino = capacidade.map((vagas, i) => vagas - feminino[i]);
  const indices = Array.from({ length: n }, (_, i) => i);

  let soma = feminino.reduce((total, valor) => total + valor, 0);
  while (soma > totalDeMulheres) {
    const time = escolher(
      random,
      indices.filter((i) => feminino[i] > 0),
      (i) => feminino[i],
    );
    if (time === null) break;
    feminino[time] -= 1;
    masculino[time] += 1;
    soma -= 1;
  }
  while (soma < totalDeMulheres) {
    const time = escolher(
      random,
      indices.filter((i) => masculino[i] > 0),
      (i) => -feminino[i],
    );
    if (time === null) break;
    feminino[time] += 1;
    masculino[time] -= 1;
    soma += 1;
  }

  return { feminino, masculino };
}

function melhorDestino(
  jogador: Player,
  elencos: readonly Player[][],
  vagasDoGenero: readonly number[],
  capacidade: readonly number[],
  historico: HistoricoDeDuplas,
  random: Rng,
): number {
  const indices = Array.from({ length: elencos.length }, (_, i) => i);

  let disponiveis = vagasDoGenero
    .map((_, i) => i)
    .filter((i) => vagasDoGenero[i] > 0);
  if (disponiveis.length === 0) {
    disponiveis = indices.filter((i) => elencos[i].length < capacidade[i]);
  }
  if (disponiveis.length === 0) {
    const menor = Math.min(...elencos.map((elenco) => elenco.length));
    disponiveis = indices.filter((i) => elencos[i].length === menor);
  }

  return minBy(
    disponiveis,
    (time) =>
      forcaDe(elencos[time]) +
      PESO_HISTORICO_NA_ESCOLHA * historico.penalidade(jogador.id, elencos[time]) +
      random.nextDouble() * RUIDO_NA_ESCOLHA,
  );
}

function montar(
  ativos: readonly Player[],
  totalTimes: number,
  historico: HistoricoDeDuplas,
  random: Rng,
): Player[][] {
  const capacidade = capacidades(ativos.length, totalTimes, random);
  const masculinos = ativos.filter((jogador) => jogador.genero === 'masculino');
  const femininas = ativos.filter((jogador) => jogador.genero === 'feminino');
  const vagas = cotasPorGenero(capacidade, femininas.length, random);

  const elencos: Player[][] = Array.from({ length: totalTimes }, () => []);

  const distribuir = (jogadores: readonly Player[], restante: number[]): void => {
    const fila = sortedBy(shuffled(jogadores, random), (jogador) => -jogador.skillLevel);
    fila.forEach((jogador) => {
      const destino = melhorDestino(jogador, elencos, restante, capacidade, historico, random);
      elencos[destino].push(jogador);
      restante[destino] -= 1;
    });
  };

  distribuir(femininas, vagas.feminino);
  distribuir(masculinos, vagas.masculino);

  return elencos;
}

function custo(elencos: readonly Player[][], historico: HistoricoDeDuplas): number {
  const forcas = elencos.map(forcaDe);
  const amplitude = Math.max(...forcas) - Math.min(...forcas);
  const desequilibrio = elencos.reduce((soma, elenco) => {
    const mulheres = elenco.filter((jogador) => jogador.genero === 'feminino').length;
    return soma + Math.abs(mulheres - Math.min(ALVO_POR_GENERO, elenco.length));
  }, 0);

  return (
    PESO_AMPLITUDE * amplitude +
    PESO_REPETICAO * historico.repeticoes(elencos) +
    PESO_GENERO * desequilibrio
  );
}

export function distribute(
  players: readonly Player[],
  teams: readonly Team[],
  historico: HistoricoDeDuplas = HistoricoDeDuplas.VAZIO,
  random: Rng = defaultRandom(),
  tentativas: number = TENTATIVAS_PADRAO,
): TeamRoster[] {
  const timesEmOrdem = sortedBy(
    teams,
    (time) => time.ordem,
    (time) => time.nome.toLowerCase(),
  );
  if (timesEmOrdem.length === 0) return [];

  const ativos = players.filter((jogador) => jogador.ativo);
  if (ativos.length === 0) return timesEmOrdem.map((team) => ({ team, players: [] }));

  let melhor = montar(ativos, timesEmOrdem.length, historico, random);
  let melhorCusto = custo(melhor, historico);
  for (let i = 0; i < Math.max(tentativas - 1, 0); i++) {
    const candidato = montar(ativos, timesEmOrdem.length, historico, random);
    const custoDoCandidato = custo(candidato, historico);
    if (custoDoCandidato < melhorCusto) {
      melhorCusto = custoDoCandidato;
      melhor = candidato;
    }
  }

  return timesEmOrdem.map((team, indice) => ({
    team,
    players: sortedBy(
      melhor[indice],
      (jogador) => -jogador.skillLevel,
      (jogador) => jogador.nome.toLowerCase(),
    ),
  }));
}
