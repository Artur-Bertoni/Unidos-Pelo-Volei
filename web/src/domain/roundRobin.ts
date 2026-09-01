import type { Team } from './models';
import { defaultRandom, maxBy, shuffled, sortedBy, type Rng } from './random';

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

export const TIMES_POR_QUADRA = 3;

const MAX_FASES = 80;
const ORCAMENTO_BUSCA = 40_000;

const par = (a: Team, b: Team): string => (a.id < b.id ? `${a.id}|${b.id}` : `${b.id}|${a.id}`);

const paresDe = (times: readonly Team[]): string[] => {
  const pares: string[] = [];
  for (let i = 0; i < times.length; i++) {
    for (let j = i + 1; j < times.length; j++) pares.push(par(times[i], times[j]));
  }
  return pares;
};

const pertenceAoPar = (chave: string, teamId: string): boolean => {
  const separador = chave.indexOf('|');
  return chave.slice(0, separador) === teamId || chave.slice(separador + 1) === teamId;
};

function tamanhosDosGrupos(total: number, quadras: number): number[] {
  if (total <= 2 * quadras) return new Array(Math.floor(total / 2)).fill(2);

  const trios = Math.min(quadras, Math.floor(total / TIMES_POR_QUADRA));
  if (trios === 0) return new Array(Math.floor(total / 2)).fill(2);

  const grupos = new Array<number>(trios).fill(TIMES_POR_QUADRA);
  if (grupos.length < quadras) {
    const sobra = total - trios * TIMES_POR_QUADRA;
    if (sobra === 2) {
      grupos.push(2);
    } else if (sobra === 1) {
      grupos[grupos.length - 1] = 2;
      grupos.push(2);
    }
  }
  return grupos;
}

const ordemDosConfrontos = (tamanho: number): [number, number][] => {
  if (tamanho === 3) {
    return [
      [0, 1],
      [0, 2],
      [1, 2],
    ];
  }
  if (tamanho === 2) return [[0, 1]];
  return [];
};

function combinacoes<T>(itens: readonly T[], k: number): T[][] {
  if (k === 0) return [[]];
  if (itens.length < k) return [];

  const resultado: T[][] = [];
  const atual: T[] = [];
  const montar = (inicio: number): void => {
    if (atual.length === k) {
      resultado.push(atual.slice());
      return;
    }
    for (let i = inicio; i <= itens.length - (k - atual.length); i++) {
      atual.push(itens[i]);
      montar(i + 1);
      atual.pop();
    }
  };
  montar(0);
  return resultado;
}

function cabeNaSequencia(grupo: readonly Team[], sequencia: Map<string, number> | null): boolean {
  if (sequencia === null) return true;
  const seguidos = grupo.map((time) => sequencia.get(time.id) ?? 0).sort((a, b) => a - b);
  if (grupo.length < TIMES_POR_QUADRA) return seguidos.every((valor) => valor <= 1);
  return seguidos[0] === 0 && seguidos[1] <= 1;
}

function escalarParaAFase(
  times: readonly Team[],
  quantidade: number,
  foraDaFase: Map<string, number>,
  pendentes: Set<string>,
  random: Rng,
): Team[] {
  if (quantidade >= times.length) return times.slice();

  const paresPendentesDe = (time: Team): number => {
    let total = 0;
    for (const chave of pendentes) if (pertenceAoPar(chave, time.id)) total++;
    return total;
  };

  return sortedBy(
    shuffled(times, random),
    (time) => -(foraDaFase.get(time.id) ?? 0),
    (time) => -paresPendentesDe(time),
  ).slice(0, quantidade);
}

function buscarSemRepetir(
  restantes: readonly Team[],
  tamanhos: readonly number[],
  pendentes: Set<string>,
  sequencia: Map<string, number> | null,
  orcamento: { valor: number },
): Team[][] | null {
  if (tamanhos.length === 0) return [];
  if (orcamento.valor-- <= 0) return null;

  const ancora = restantes[0];
  for (const companheiros of combinacoes(restantes.slice(1), tamanhos[0] - 1)) {
    const grupo = [ancora, ...companheiros];
    if (paresDe(grupo).every((chave) => pendentes.has(chave)) && cabeNaSequencia(grupo, sequencia)) {
      const sobra = restantes.filter((time) => !grupo.some((doGrupo) => doGrupo.id === time.id));
      const demais = buscarSemRepetir(sobra, tamanhos.slice(1), pendentes, sequencia, orcamento);
      if (demais !== null) return [grupo, ...demais];
    }
    if (orcamento.valor <= 0) return null;
  }
  return null;
}

function agruparPermitindoRepetir(
  disponiveis: readonly Team[],
  tamanhos: readonly number[],
  pendentes: Set<string>,
  random: Rng,
): Team[][] {
  const restantes = shuffled(disponiveis, random);
  const remover = (time: Team): void => {
    const indice = restantes.findIndex((candidato) => candidato.id === time.id);
    if (indice >= 0) restantes.splice(indice, 1);
  };

  return tamanhos.map((tamanho) => {
    const ancora = maxBy(restantes, (time) =>
      restantes.filter((outro) => outro.id !== time.id && pendentes.has(par(time, outro))).length,
    );
    remover(ancora);
    const grupo = [ancora];
    for (let i = 0; i < tamanho - 1; i++) {
      const escolhido = maxBy(
        restantes,
        (candidato) => grupo.filter((doGrupo) => pendentes.has(par(doGrupo, candidato))).length,
      );
      grupo.push(escolhido);
      remover(escolhido);
    }
    return grupo;
  });
}

function particionar(
  disponiveis: readonly Team[],
  tamanhos: readonly number[],
  pendentes: Set<string>,
  sequencia: Map<string, number> | null,
  random: Rng,
): Team[][] {
  const embaralhados = shuffled(disponiveis, random);
  return (
    buscarSemRepetir(embaralhados, tamanhos, pendentes, sequencia, { valor: ORCAMENTO_BUSCA }) ??
    buscarSemRepetir(embaralhados, tamanhos, pendentes, null, { valor: ORCAMENTO_BUSCA }) ??
    agruparPermitindoRepetir(disponiveis, tamanhos, pendentes, random)
  );
}

export function generateSchedule(
  teams: readonly Team[],
  quadras: number,
  random: Rng = defaultRandom(),
): ScheduledRound[] {
  const times = sortedBy(
    teams.filter((time) => time.ativo),
    (time) => time.ordem,
    (time) => time.nome.toLowerCase(),
  );
  if (times.length < 2 || quadras < 1) return [];

  const tamanhos = tamanhosDosGrupos(times.length, quadras);
  if (tamanhos.length === 0) return [];

  const porFase = tamanhos.reduce((soma, tamanho) => soma + tamanho, 0);
  const comTrios = tamanhos.includes(TIMES_POR_QUADRA);
  const pendentes = new Set(paresDe(times));
  const foraDaFase = new Map(times.map((time) => [time.id, 0]));
  const sequencia = new Map(times.map((time) => [time.id, 0]));

  const rodadas: ScheduledRound[] = [];
  let fase = 1;
  let numero = 1;

  while (pendentes.size > 0 && fase <= MAX_FASES) {
    const descanso = comTrios ? sequencia : null;
    const escalados = escalarParaAFase(times, porFase, foraDaFase, pendentes, random);

    const comAtraso = sortedBy(
      particionar(escalados, tamanhos, pendentes, descanso, random)
        .map((grupo) => sortedBy(grupo, (time) => sequencia.get(time.id) ?? 0))
        .map((grupo) => ({ grupo, atraso: cabeNaSequencia(grupo, descanso) ? 0 : 1 })),
      (item) => item.atraso,
    );

    const grupos = comAtraso.map((item) => item.grupo);
    const atrasos = comAtraso.map((item) => item.atraso);

    const ineditos = grupos.reduce(
      (soma, grupo) => soma + paresDe(grupo).filter((chave) => pendentes.has(chave)).length,
      0,
    );
    if (ineditos === 0 && rodadas.length > 0) break;

    const rodadasNaFase = Math.max(
      ...grupos.map((grupo, indice) => atrasos[indice] + ordemDosConfrontos(grupo.length).length),
    );

    for (let indice = 0; indice < rodadasNaFase; indice++) {
      const partidas: ScheduledMatch[] = [];
      grupos.forEach((grupo, quadra) => {
        const confronto = ordemDosConfrontos(grupo.length)[indice - atrasos[quadra]];
        if (confronto !== undefined) {
          partidas.push({ quadra: quadra + 1, teamA: grupo[confronto[0]], teamB: grupo[confronto[1]] });
        }
      });

      const jogando = new Set(partidas.flatMap((partida) => [partida.teamA.id, partida.teamB.id]));

      rodadas.push({
        numero: numero++,
        fase,
        matches: partidas,
        folgam: times.filter((time) => !jogando.has(time.id)),
      });

      times.forEach((time) => {
        sequencia.set(time.id, jogando.has(time.id) ? (sequencia.get(time.id) ?? 0) + 1 : 0);
      });
      partidas.forEach((partida) => pendentes.delete(par(partida.teamA, partida.teamB)));
    }

    const dentro = new Set(escalados.map((time) => time.id));
    times.forEach((time) => {
      foraDaFase.set(time.id, dentro.has(time.id) ? 0 : (foraDaFase.get(time.id) ?? 0) + 1);
    });
    fase++;
  }

  return rodadas;
}
