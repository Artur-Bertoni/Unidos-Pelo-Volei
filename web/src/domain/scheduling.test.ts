import { describe, expect, it } from 'vitest';
import type { Genero, Player, Team, TeamRoster } from './models';
import { forcaTotal, homensDo, mulheresDo } from './models';
import { KotlinRandom } from './random';
import { generateSchedule } from './roundRobin';
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
  posicao: null,
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

describe('RoundRobinScheduler', () => {
  it('cada quadra recebe um trio que joga A-B, A-C e B-C na fase', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    const porFaseEQuadra = new Map<string, { a: string; b: string }[]>();
    rodadas.forEach((rodada) => {
      rodada.matches.forEach((partida) => {
        const chave = `${rodada.fase}-${partida.quadra}`;
        const lista = porFaseEQuadra.get(chave) ?? [];
        lista.push({ a: partida.teamA.id, b: partida.teamB.id });
        porFaseEQuadra.set(chave, lista);
      });
    });

    porFaseEQuadra.forEach((partidas, chave) => {
      const confrontos = new Set(partidas.map((p) => parDe(p.a, p.b)));
      const trio = new Set(partidas.flatMap((p) => [p.a, p.b]));

      expect(trio.size, chave).toBe(3);
      expect(partidas.length, chave).toBe(3);

      const membros = [...trio];
      const todosOsPares = new Set<string>();
      for (let i = 0; i < membros.length; i++) {
        for (let j = i + 1; j < membros.length; j++) todosOsPares.add(parDe(membros[i], membros[j]));
      }
      expect(confrontos, chave).toEqual(todosOsPares);
    });
  });

  it('nenhum time joga mais de dois jogos seguidos', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    times(9).forEach((time) => {
      let seguidos = 0;
      rodadas.forEach((rodada) => {
        const jogou = rodada.matches.some((p) => p.teamA.id === time.id || p.teamB.id === time.id);
        seguidos = jogou ? seguidos + 1 : 0;
        expect(seguidos, `${time.nome} jogou ${seguidos} seguidos`).toBeLessThanOrEqual(2);
      });
    });
  });

  it('o trio que chega cansado entra uma rodada depois', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    const porFase = new Map<number, number>();
    rodadas.forEach((rodada) => porFase.set(rodada.fase, (porFase.get(rodada.fase) ?? 0) + 1));

    const fases = [...porFase.keys()].sort((a, b) => a - b);
    expect(fases.map((fase) => porFase.get(fase))).toEqual([3, 3, 3, 4]);
    expect(rodadas).toHaveLength(13);
    expect(rodadas.map((r) => r.numero)).toEqual(Array.from({ length: 13 }, (_, i) => i + 1));
  });

  it('chaveamento de 9 times em 3 quadras cobre o round-robin completo', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    const confrontos = rodadas.flatMap((r) => r.matches).map((p) => parDe(p.teamA.id, p.teamB.id));

    expect(confrontos).toHaveLength(36);
    expect(new Set(confrontos).size).toBe(36);
    expect(new Set(rodadas.map((r) => r.fase)).size).toBe(4);
  });

  it('nenhum time joga duas vezes na mesma rodada e as folgas fecham', () => {
    const rodadas = generateSchedule(times(9), 3, new KotlinRandom(4));

    rodadas.forEach((rodada) => {
      const jogando = rodada.matches.flatMap((p) => [p.teamA.id, p.teamB.id]);
      expect(new Set(jogando).size).toBe(jogando.length);
      expect(jogando.length + rodada.folgam.length).toBe(9);
      expect(rodada.matches.length).toBeLessThanOrEqual(3);
    });
  });

  it('com todos cabendo em quadra ao mesmo tempo ninguem espera', () => {
    const rodadas = generateSchedule(times(6), 3, new KotlinRandom(2));

    expect(rodadas.every((r) => r.matches.length === 3 && r.folgam.length === 0)).toBe(true);
    expect(rodadas).toHaveLength(5);
    expect(rodadas.reduce((soma, r) => soma + r.matches.length, 0)).toBe(15);
  });

  it('todo confronto acontece e ninguem passa de dois jogos seguidos', () => {
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
      const caso = `${n} times em ${quadras} quadras`;

      const jogados = new Set(
        rodadas.flatMap((r) => r.matches).map((p) => parDe(p.teamA.id, p.teamB.id)),
      );
      const todos = new Set<string>();
      for (let i = 0; i < equipes.length; i++) {
        for (let j = i + 1; j < equipes.length; j++) todos.add(parDe(equipes[i].id, equipes[j].id));
      }
      expect(jogados, caso).toEqual(todos);
      expect(rodadas.every((r) => r.matches.length <= quadras), caso).toBe(true);

      equipes.forEach((time) => {
        let seguidos = 0;
        rodadas.forEach((rodada) => {
          const jogou = rodada.matches.some((p) => p.teamA.id === time.id || p.teamB.id === time.id);
          seguidos = jogou ? seguidos + 1 : 0;
          expect(seguidos, `${caso}: ${time.nome} jogou ${seguidos} seguidos`).toBeLessThanOrEqual(2);
        });
      });
    });
  });

  it('mais quadras do que times nao quebra', () => {
    const rodadas = generateSchedule(times(4), 5, new KotlinRandom(1));

    expect(rodadas.reduce((soma, r) => soma + r.matches.length, 0)).toBe(6);
    expect(rodadas.every((r) => r.matches.length <= 2)).toBe(true);
  });
});
