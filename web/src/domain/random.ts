/**
 * Porte fiel do kotlin.random.Random (XorWowRandom) e dos helpers de coleção que
 * dependem dele. Manter a mesma sequência permite que os testes portados usem as
 * mesmas sementes do SchedulingTest.kt e comparem os mesmos resultados.
 */
export class KotlinRandom {
  private x: number;
  private y: number;
  private z: number;
  private w: number;
  private v: number;
  private addend: number;

  constructor(seed: number) {
    const seed1 = seed | 0;
    const seed2 = seed1 >> 31;
    this.x = seed1;
    this.y = seed2;
    this.z = 0;
    this.w = 0;
    this.v = ~seed1;
    this.addend = ((seed1 << 10) ^ (seed2 >>> 4)) | 0;

    if ((this.x | this.y | this.z | this.w | this.v) === 0) {
      throw new Error('Initial state must have at least one non-zero element.');
    }
    for (let i = 0; i < 64; i++) this.nextInt();
  }

  nextInt(): number {
    let t = this.x;
    t = (t ^ (t >>> 2)) | 0;
    this.x = this.y;
    this.y = this.z;
    this.z = this.w;
    const v0 = this.v;
    this.w = v0;
    t = (((t ^ (t << 1)) | 0) ^ v0 ^ (v0 << 4)) | 0;
    this.v = t;
    this.addend = (this.addend + 362437) | 0;
    return (t + this.addend) | 0;
  }

  private nextBits(bitCount: number): number {
    return (this.nextInt() >>> (32 - bitCount)) & ((-bitCount >> 31) | 0);
  }

  /** Equivalente a Random.nextInt(until). */
  nextIntUntil(until: number): number {
    if (until <= 0) throw new Error(`Random range is empty: [0, ${until}).`);
    const n = until;
    if ((n & -n) === n) {
      const bitCount = 31 - Math.clz32(n);
      return this.nextBits(bitCount);
    }
    let value: number;
    let bits: number;
    do {
      bits = this.nextInt() >>> 1;
      value = bits % n;
    } while (((bits - value + (n - 1)) | 0) < 0);
    return value;
  }

  /** Equivalente a Random.nextDouble(). */
  nextDouble(): number {
    const hi26 = this.nextBits(26);
    const low27 = this.nextBits(27);
    return (hi26 * 134217728 + low27) / 9007199254740992;
  }
}

/** Fonte de aleatoriedade padrão, sem semente fixa. */
export class SystemRandom {
  nextInt(): number {
    return (Math.random() * 4294967296) | 0;
  }

  nextIntUntil(until: number): number {
    return Math.floor(Math.random() * until);
  }

  nextDouble(): number {
    return Math.random();
  }
}

export type Rng = KotlinRandom | SystemRandom;

export const defaultRandom = (): Rng => new SystemRandom();

/** Equivalente a Iterable<T>.shuffled(random): Fisher-Yates de trás para frente. */
export function shuffled<T>(items: readonly T[], random: Rng): T[] {
  const copy = items.slice();
  for (let i = copy.length - 1; i >= 1; i--) {
    const j = random.nextIntUntil(i + 1);
    const swap = copy[i];
    copy[i] = copy[j];
    copy[j] = swap;
  }
  return copy;
}

/** Equivalente a List<T>.random(random). */
export function randomElement<T>(items: readonly T[], random: Rng): T {
  return items[random.nextIntUntil(items.length)];
}

/** Ordenação estável por uma lista de chaves, como compareBy do Kotlin. */
export function sortedBy<T>(items: readonly T[], ...keys: ((item: T) => number | string)[]): T[] {
  return items.slice().sort((a, b) => {
    for (const key of keys) {
      const ka = key(a);
      const kb = key(b);
      if (ka < kb) return -1;
      if (ka > kb) return 1;
    }
    return 0;
  });
}

/** Equivalente a minBy: devolve o primeiro elemento de menor chave. */
export function minBy<T>(items: readonly T[], key: (item: T) => number): T {
  let melhor = items[0];
  let melhorChave = key(melhor);
  for (let i = 1; i < items.length; i++) {
    const chave = key(items[i]);
    if (chave < melhorChave) {
      melhor = items[i];
      melhorChave = chave;
    }
  }
  return melhor;
}

/** Equivalente a maxBy: devolve o primeiro elemento de maior chave. */
export function maxBy<T>(items: readonly T[], key: (item: T) => number): T {
  let melhor = items[0];
  let melhorChave = key(melhor);
  for (let i = 1; i < items.length; i++) {
    const chave = key(items[i]);
    if (chave > melhorChave) {
      melhor = items[i];
      melhorChave = chave;
    }
  }
  return melhor;
}
