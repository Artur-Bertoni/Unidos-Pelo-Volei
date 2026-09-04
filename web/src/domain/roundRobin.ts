import type { Team } from './models';
import { defaultRandom, shuffled, type Rng } from './random';

export interface ScheduledMatch {
  quadra: number;
  teamA: Team;
  teamB: Team;
}

export interface ScheduledRound {
  numero: number;
  fase: number;
  matches: ScheduledMatch[];
  folgam: Team[];
}

export const TIMES_POR_QUADRA = 2;

const TENTATIVAS = 12;
const ORCAMENTO_BUSCA = 20_000;
const SEM_LIMITE = Number.MAX_SAFE_INTEGER;

type Dupla = [Team, Team];

const par = (a: Team, b: Team): string => (a.id < b.id ? `${a.id}|${b.id}` : `${b.id}|${a.id}`);

const pertenceAoPar = (chave: string, teamId: string): boolean => {
  const separador = chave.indexOf('|');
  return chave.slice(0, separador) === teamId || chave.slice(separador + 1) === teamId;
};

const duplasDe = (times: readonly Team[]): Dupla[] => {
  const duplas: Dupla[] = [];
  for (let i = 0; i < times.length; i++) {
    for (let j = i + 1; j < times.length; j++) duplas.push([times[i], times[j]]);
  }
  return duplas;
};

const paresDe = (times: readonly Team[]): string[] =>
  duplasDe(times).map(([a, b]) => par(a, b));

function mdc(a: number, b: number): number {
  return b === 0 ? a : mdc(b, a % b);
}

function limiteDeSeguidos(total: number, porRodada: number): number {
  const emQuadra = TIMES_POR_QUADRA * porRodada;
  const descansam = total - emQuadra;
  return descansam <= 0 ? SEM_LIMITE : Math.ceil(emQuadra / descansam);
}

interface Estado {
  pendentes: Set<string>;
  repeticoes: Map<string, number>;
  seguidos: Map<string, number>;
  descanso: Map<string, number>;
}

function candidatos(
  times: readonly Team[],
  estado: Estado,
  limite: number,
  random: Rng,
): Dupla[] {
  const adversariosQueFaltam = new Map<string, number>(
    times.map((time) => {
      let total = 0;
      for (const chave of estado.pendentes) if (pertenceAoPar(chave, time.id)) total++;
      return [time.id, total];
    }),
  );

  const excesso = (id: string): number =>
    limite === SEM_LIMITE ? 0 : Math.max((estado.seguidos.get(id) ?? 0) + 1 - limite, 0);

  const chaves = (dupla: Dupla): number[] => {
    const chave = par(dupla[0], dupla[1]);
    return [
      estado.pendentes.has(chave) ? 0 : 1,
      excesso(dupla[0].id) + excesso(dupla[1].id),
      estado.repeticoes.get(chave) ?? 0,
      -((estado.descanso.get(dupla[0].id) ?? 0) + (estado.descanso.get(dupla[1].id) ?? 0)),
      -(
        (adversariosQueFaltam.get(dupla[0].id) ?? 0) +
        (adversariosQueFaltam.get(dupla[1].id) ?? 0)
      ),
    ];
  };

  return shuffled(duplasDe(times), random)
    .map((dupla) => ({ dupla, ordem: chaves(dupla) }))
    .sort((a, b) => {
      for (let i = 0; i < a.ordem.length; i++) {
        if (a.ordem[i] !== b.ordem[i]) return a.ordem[i] - b.ordem[i];
      }
      return 0;
    })
    .map((item) => item.dupla);
}

function buscar(
  opcoes: readonly Dupla[],
  faltam: number,
  inicio: number,
  usados: Set<string>,
  orcamento: { valor: number },
): Dupla[] | null {
  if (faltam === 0) return [];
  if (orcamento.valor-- <= 0) return null;

  for (let indice = inicio; indice < opcoes.length; indice++) {
    const [a, b] = opcoes[indice];
    if (usados.has(a.id) || usados.has(b.id)) continue;

    usados.add(a.id);
    usados.add(b.id);
    const resto = buscar(opcoes, faltam - 1, indice + 1, usados, orcamento);
    usados.delete(a.id);
    usados.delete(b.id);

    if (resto !== null) return [opcoes[indice], ...resto];
    if (orcamento.valor <= 0) return null;
  }
  return null;
}

function escolherRodada(
  times: readonly Team[],
  porRodada: number,
  estado: Estado,
  limite: number,
  random: Rng,
): Dupla[] {
  const opcoes = candidatos(times, estado, limite, random);
  return buscar(opcoes, porRodada, 0, new Set<string>(), { valor: ORCAMENTO_BUSCA }) ?? [];
}

interface Tentativa {
  rodadas: ScheduledMatch[][];
  repetidos: number;
  maiorSequencia: number;
}

function montar(times: readonly Team[], porRodada: number, random: Rng): Tentativa {
  const estado: Estado = {
    pendentes: new Set(paresDe(times)),
    repeticoes: new Map(),
    seguidos: new Map(times.map((time) => [time.id, 0])),
    descanso: new Map(times.map((time) => [time.id, 0])),
  };
  const limite = limiteDeSeguidos(times.length, porRodada);
  const teto = estado.pendentes.size * 2;

  const rodadas: ScheduledMatch[][] = [];
  let repetidos = 0;
  let maiorSequencia = 0;

  while (estado.pendentes.size > 0 && rodadas.length < teto) {
    const duplas = escolherRodada(times, porRodada, estado, limite, random);
    if (duplas.length === 0) break;

    rodadas.push(
      duplas.map(([teamA, teamB], indice) => ({ quadra: indice + 1, teamA, teamB })),
    );

    duplas.forEach(([a, b]) => {
      const chave = par(a, b);
      if (!estado.pendentes.delete(chave)) repetidos++;
      estado.repeticoes.set(chave, (estado.repeticoes.get(chave) ?? 0) + 1);
    });

    const jogando = new Set(duplas.flatMap(([a, b]) => [a.id, b.id]));
    times.forEach((time) => {
      if (jogando.has(time.id)) {
        const sequencia = (estado.seguidos.get(time.id) ?? 0) + 1;
        estado.seguidos.set(time.id, sequencia);
        estado.descanso.set(time.id, 0);
        maiorSequencia = Math.max(maiorSequencia, sequencia);
      } else {
        estado.seguidos.set(time.id, 0);
        estado.descanso.set(time.id, (estado.descanso.get(time.id) ?? 0) + 1);
      }
    });
  }

  return { rodadas, repetidos, maiorSequencia };
}

function numerar(
  rodadas: readonly ScheduledMatch[][],
  times: readonly Team[],
  porRodada: number,
): ScheduledRound[] {
  const bloco = times.length / mdc(times.length, TIMES_POR_QUADRA * porRodada);
  return rodadas.map((partidas, indice) => {
    const jogando = new Set(partidas.flatMap((partida) => [partida.teamA.id, partida.teamB.id]));
    return {
      numero: indice + 1,
      fase: Math.floor(indice / bloco) + 1,
      matches: partidas,
      folgam: times.filter((time) => !jogando.has(time.id)),
    };
  });
}

export function generateSchedule(
  teams: readonly Team[],
  quadras: number,
  random: Rng = defaultRandom(),
): ScheduledRound[] {
  const times = teams
    .filter((time) => time.ativo)
    .slice()
    .sort(
      (a, b) => a.ordem - b.ordem || a.nome.toLowerCase().localeCompare(b.nome.toLowerCase()),
    );
  const porRodada = Math.min(quadras, Math.floor(times.length / TIMES_POR_QUADRA));
  if (porRodada < 1) return [];

  let melhor: Tentativa | null = null;
  for (let tentativa = 0; tentativa < TENTATIVAS; tentativa++) {
    const atual = montar(times, porRodada, random);
    if (
      melhor === null ||
      atual.repetidos < melhor.repetidos ||
      (atual.repetidos === melhor.repetidos &&
        (atual.rodadas.length < melhor.rodadas.length ||
          (atual.rodadas.length === melhor.rodadas.length &&
            atual.maiorSequencia < melhor.maiorSequencia)))
    ) {
      melhor = atual;
    }
  }

  return melhor === null ? [] : numerar(melhor.rodadas, times, porRodada);
}
