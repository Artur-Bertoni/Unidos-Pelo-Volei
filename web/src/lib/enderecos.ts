export const MINIMO_DE_LETRAS = 3;

const LIMITE = 6;
const SERVICO = 'https://photon.komoot.io/api/';

interface Propriedades {
  name?: string;
  street?: string;
  housenumber?: string;
  district?: string;
  city?: string;
  town?: string;
  village?: string;
  county?: string;
  state?: string;
}

const descricaoDe = (propriedades: Propriedades | undefined): string | null => {
  if (!propriedades) return null;

  const limpo = (valor: string | undefined): string | null => valor?.trim() || null;
  const rua = [limpo(propriedades.street), limpo(propriedades.housenumber)]
    .filter((parte): parte is string => parte !== null)
    .join(', ');
  const cidade =
    limpo(propriedades.city) ??
    limpo(propriedades.town) ??
    limpo(propriedades.village) ??
    limpo(propriedades.county);
  const cidadeComEstado = [cidade, limpo(propriedades.state)]
    .filter((parte): parte is string => parte !== null)
    .join(' - ');

  const partes = [limpo(propriedades.name), rua || null, limpo(propriedades.district), cidadeComEstado || null]
    .filter((parte): parte is string => parte !== null);

  return [...new Set(partes)].join(', ') || null;
};

export async function buscarEnderecos(termo: string, sinal?: AbortSignal): Promise<string[]> {
  const consulta = termo.trim();
  if (consulta.length < MINIMO_DE_LETRAS) return [];

  const url = `${SERVICO}?q=${encodeURIComponent(consulta)}&limit=${LIMITE}&lang=pt`;
  const resposta = await fetch(url, { signal: sinal, headers: { Accept: 'application/json' } });
  if (!resposta.ok) return [];

  const corpo: { features?: { properties?: Propriedades }[] } = await resposta.json();
  const descricoes = (corpo.features ?? [])
    .map((feature) => descricaoDe(feature.properties))
    .filter((descricao): descricao is string => descricao !== null);

  return [...new Set(descricoes)];
}
