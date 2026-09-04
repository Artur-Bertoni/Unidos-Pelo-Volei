import { describe, expect, it } from 'vitest';
import type { Genero, Player, Team, TeamRoster } from './models';
import { forcaTotal, homensDo, mulheresDo } from './models';
import { KotlinRandom } from './random';
import { generateSchedule, TIMES_POR_QUADRA, type ScheduledRound } from './roundRobin';
import type { ElencoPassado } from './teamDraft';
import { distribute, HistoricoDeDuplas } from './teamDraft';

const times = (quantidade: number): Team[] =>
  Array.from({ length: quantidade }, (_, i) => ({
    id: `t${i + 1}`,
    nome: `Time ${i + 1}`,
    corHex: '#FFFFFF',
    sigla: `T${i + 1}`,
    ativo: true,
    ordem: i + 1,
  }));

const semFicha = {
  profileId: null,
  fotoUrl: null,
  nascimentoDia: null,
  nascimentoMes: null,
  entrouEm: null,
  regime: 'mensalista',
} as const;

const jogadores = (niveis: number[], genero: Genero = 'masculino', prefixo = 'p'): Player[] =>
  niveis.map((nivel, indice) => ({
    id: `${prefixo}${indice}`,
    nome: `Jogador ${prefixo}${indice}`,
    skillLevel: nivel,
    genero,
    ativo: true,
    ...semFicha,
  }));

const grupoMisto = (homens: number, mulheres: number): Player[] => [
  ...jogadores(
    Array.from({ length: homens }, (_, i) => 5 - (i % 5)),
    'masculino',
    'h',
  ),
  ...jogadores(
    Array.from({ length: mulheres }, (_, i) => 5 - (i % 5)),
    'feminino',
    'm',
  ),
];

const duplas = (elencos: string[][]): Set<string> => {
  const resultado = new Set<string>();
  elencos.forEach((elenco) => {
    for (let i = 0; i < elenco.length; i++) {
      for (let j = i + 1; j < elenco.length; j++) {
        resultado.add([elenco[i], elenco[j]].sort().join('|'));
      }
    }
  });
  return resultado;
};

const idsPorTime = (elencos: TeamRoster[]): string[][] =>
  elencos.map((elenco) => elenco.players.map((jogador) => jogador.id).sort());

const parDe = (a: string, b: string): string => [a, b].sort().join('|');

describe('TeamDraft', () => {
  it('sorteio mantem os times equilibrados', () => {
    const elencos = distribute(grupoMisto(18, 18), times(9), undefined, new KotlinRandom(7));

    expect(elencos).toHaveLength(9);
    expect(elencos.reduce((soma, e) => soma + e.players.length, 0)).toBe(36);
    const forcas = elencos.map(forcaTotal);
    expect(Math.max(...forcas) - Math.min(...forcas)).toBeLessThanOrEqual(3);
  });

  it('sorteio poe 2 homens e 2 mulheres em cada time', () => {
    const elencos = distribute(grupoMisto(18, 18), times(9), undefined, new KotlinRandom(11));

    elencos.forEach((elenco) => {
      expect(homensDo(elenco), `time ${elenco.team.nome}`).toBe(2);
      expect(mulheresDo(elenco), `time ${elenco.team.nome}`).toBe(2);
    });
  });

  it('com menos mulheres do que vagas elas ficam espalhadas', () => {
    const elencos = distribute(grupoMisto(26, 10), times(9), undefined, new KotlinRandom(3));

    expect(elencos.reduce((soma, e) => soma + mulheresDo(e), 0)).toBe(10);
    expect(elencos.every((e) => mulheresDo(e) >= 1)).toBe(true);
  });

  it('sorteio ignora jogadores inativos', () => {
    const comInativo: Player[] = [
      ...grupoMisto(5, 4),
      { id: 'x', nome: 'Fora', skillLevel: 5, genero: 'masculino', ativo: false, ...semFicha },
    ];

    const elencos = distribute(comInativo, times(3), undefined, new KotlinRandom(1));

    expect(elencos.reduce((soma, e) => soma + e.players.length, 0)).toBe(9);
    expect(elencos.some((e) => e.players.some((j) => j.id === 'x'))).toBe(false);
  });

  it('dois sorteios seguidos nao devolvem os mesmos times', () => {
    const players = grupoMisto(18, 18);
    const teams = times(9);

    const primeiro = distribute(players, teams, undefined, new KotlinRandom(21));
    const segundo = distribute(players, teams, undefined, new KotlinRandom(99));

    expect(idsPorTime(primeiro)).not.toEqual(idsPorTime(segundo));
  });

  it('historico afasta quem ja jogou junto', () => {
    const players = grupoMisto(18, 18);
    const teams = times(9);

    const ontem = distribute(players, teams, undefined, new KotlinRandom(5));
    const historico = HistoricoDeDuplas.de(
      ontem.map(
        (elenco): ElencoPassado => ({
          jogadores: elenco.players.map((jogador) => jogador.id),
          peso: 1.0,
        }),
      ),
    );

    const hoje = distribute(players, teams, historico, new KotlinRandom(5));

    const duplasDeOntem = duplas(ontem.map((e) => e.players.map((j) => j.id)));
    const duplasDeHoje = duplas(hoje.map((e) => e.players.map((j) => j.id)));
    const repetidas = [...duplasDeHoje].filter((dupla) => duplasDeOntem.has(dupla)).length;

    expect(repetidas, `repetidas=${repetidas} de ${duplasDeHoje.size}`).toBeLessThanOrEqual(2);
  });
});

const quadrasCheias = (quantidade: number, quadras: number): number =>
  Math.min(quadras, Math.floor(quantidade / TIMES_POR_QUADRA));

const confrontosDe = (equipes: Team[]): Set<string> => {
  const todos = new Set<string>();
  for (let i = 0; i < equipes.length; i++) {
    for (let j = i + 1; j < equipes.length; j++) todos.add(parDe(equipes[i].id, equipes[j].id));
  }
  return todos;
};

const confrontosJogados = (rodadas: ScheduledRound[]): string[] =>
  rodadas.flatMap((r) => r.matches).map((p) => parDe(p.teamA.id, p.teamB.id));

const jogouNaRodada = (rodada: ScheduledRound, teamId: string): boolean =>
  rodada.matches.some((p) => p.teamA.id === teamId || p.teamB.id === teamId);

describe('RoundRobinScheduler', () => {
  it('toda rodada enche todas as quadras', () => {
    const casos: [number, number][] = [
      [4, 1],
      [5, 2],
      [6, 3],
      [7, 3],
      [8, 3],
      [9, 3],
      [10, 3],
      [12, 4],
      [15, 5],
    ];

    casos.forEach(([n, quadras]) => {
      const rodadas = generateSchedule(times(n), quadras, new KotlinRandom(n));
      const porRodada = quadrasCheias(n, quadras);
      const caso = `${n} times em ${quadras} quadras`;

      expect(rodadas.length, caso).toBeGreaterThan(0);
      rodadas.forEach((rodada) => {
        expect(rodada.matches.length, `${caso}, rodada ${rodada.numero}`).toBe(porRodada);
        expect(
          rodada.matches.map((p) => p.quadra).sort((a, b) => a - b),
          `${caso}, rodada ${rodada.numero}`,
        ).toEqual(Array.from({ length: porRodada }, (_, i) => i + 1));
      });
    });
  });

  it('nove times em tres quadras jogam doze rodadas com seis times em cada uma', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    expect(rodadas).toHaveLength(12);
    expect(rodadas.map((r) => r.numero)).toEqual(Array.from({ length: 12 }, (_, i) => i + 1));
    rodadas.forEach((rodada) => {
      expect(rodada.matches.length, `rodada ${rodada.numero}`).toBe(3);
      expect(rodada.folgam.length, `rodada ${rodada.numero}`).toBe(3);
    });
  });

  it('chaveamento de 9 times em 3 quadras cobre o round-robin completo sem repetir', () => {
    const equipes = times(9);
    const rodadas = generateSchedule(equipes, 3, new KotlinRandom(4));
    const confrontos = confrontosJogados(rodadas);

    expect(confrontos).toHaveLength(36);
    expect(new Set(confrontos)).toEqual(confrontosDe(equipes));
    expect(new Set(rodadas.map((r) => r.fase)).size).toBe(4);
  });

  it('nenhum time joga duas vezes na mesma rodada e as folgas fecham', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    rodadas.forEach((rodada) => {
      const jogando = rodada.matches.flatMap((p) => [p.teamA.id, p.teamB.id]);
      expect(new Set(jogando).size).toBe(jogando.length);
      expect(jogando.length + rodada.folgam.length).toBe(9);
    });
  });

  it('com todos cabendo em quadra ao mesmo tempo ninguem espera', () => {
    const rodadas = generateSchedule(times(6), 3, new KotlinRandom(2));

    expect(rodadas.every((r) => r.matches.length === 3 && r.folgam.length === 0)).toBe(true);
    expect(rodadas).toHaveLength(5);
    expect(rodadas.reduce((soma, r) => soma + r.matches.length, 0)).toBe(15);
  });

  it('todo confronto acontece em todos os formatos', () => {
    const casos: [number, number][] = [
      [4, 1],
      [5, 2],
      [7, 3],
      [8, 3],
      [9, 3],
      [10, 3],
      [12, 4],
      [15, 5],
    ];

    casos.forEach(([n, quadras]) => {
      const equipes = times(n);
      const rodadas = generateSchedule(equipes, quadras, new KotlinRandom(n));
      expect(new Set(confrontosJogados(rodadas)), `${n} times em ${quadras} quadras`).toEqual(
        confrontosDe(equipes),
      );
    });
  });

  it('quando o total de confrontos divide certo ninguem joga duas vezes o mesmo adversario', () => {
    const casos: [number, number][] = [
      [4, 1],
      [5, 2],
      [6, 3],
      [7, 3],
      [9, 3],
      [10, 3],
      [15, 5],
    ];

    casos.forEach(([n, quadras]) => {
      const rodadas = generateSchedule(times(n), quadras, new KotlinRandom(n));
      const confrontos = confrontosJogados(rodadas);
      const caso = `${n} times em ${quadras} quadras`;

      expect(new Set(confrontos).size, caso).toBe(confrontos.length);
      expect(confrontos.length, caso).toBe((n * (n - 1)) / 2);
    });
  });

  it('com sobra de confrontos a ultima rodada enche a quadra com uma revanche', () => {
    const equipes = times(8);
    const rodadas = generateSchedule(equipes, 3, new KotlinRandom(8));
    const confrontos = confrontosJogados(rodadas);

    expect(rodadas.every((r) => r.matches.length === 3)).toBe(true);
    expect(new Set(confrontos)).toEqual(confrontosDe(equipes));
    expect(confrontos.length).toBe(rodadas.length * 3);
    expect(confrontos.length).toBeGreaterThan(new Set(confrontos).size);
  });

  it('quem folga sempre volta antes de cansar quando o formato permite', () => {
    const rodadas = generateSchedule(times(10), 3, new KotlinRandom(10));

    times(10).forEach((time) => {
      let seguidos = 0;
      rodadas.forEach((rodada) => {
        seguidos = jogouNaRodada(rodada, time.id) ? seguidos + 1 : 0;
        expect(seguidos, `${time.nome} jogou ${seguidos} seguidos`).toBeLessThanOrEqual(2);
      });
    });
  });

  it('todo time joga a mesma quantidade de jogos em cada fase', () => {
    const equipes = times(9);
    const rodadas = generateSchedule(equipes, 3, new KotlinRandom(4));
    const fases = new Set(rodadas.map((r) => r.fase));

    fases.forEach((fase) => {
      const daFase = rodadas.filter((r) => r.fase === fase);
      const jogos = equipes.map(
        (time) => daFase.filter((rodada) => jogouNaRodada(rodada, time.id)).length,
      );
      expect(new Set(jogos), `fase ${fase}`).toEqual(new Set([2]));
    });
  });

  it('mais quadras do que times nao quebra', () => {
    const rodadas = generateSchedule(times(4), 5, new KotlinRandom(1));

    expect(rodadas.reduce((soma, r) => soma + r.matches.length, 0)).toBe(6);
    expect(rodadas.every((r) => r.matches.length === 2)).toBe(true);
  });

  it('menos de dois times nao gera rodada', () => {
    expect(generateSchedule(times(1), 3)).toHaveLength(0);
    expect(generateSchedule(times(9), 0)).toHaveLength(0);
  });
});
