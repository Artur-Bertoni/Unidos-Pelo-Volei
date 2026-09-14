export const GUI_PIX = 'br.gov.bcb.pix';

const LIMITE_NOME = 25;
const LIMITE_CIDADE = 15;
const LIMITE_TXID = 25;

export function crc16(dados: string): number {
  const bytes = new TextEncoder().encode(dados);
  let crc = 0xffff;
  for (const byte of bytes) {
    crc ^= byte << 8;
    for (let i = 0; i < 8; i++) {
      crc = (crc & 0x8000) !== 0 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff;
    }
  }
  return crc & 0xffff;
}

export const campo = (id: string, valor: string): string =>
  id + valor.length.toString().padStart(2, '0') + valor;

export const sanear = (texto: string, limite: number): string =>
  texto
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/[^A-Za-z0-9 ]/g, '')
    .trim()
    .toUpperCase()
    .slice(0, limite);

export const valorFormatado = (centavos: number): string =>
  `${Math.trunc(centavos / 100)}.${(centavos % 100).toString().padStart(2, '0')}`;

export type TipoDaChavePix = 'cpf' | 'cnpj' | 'celular' | 'email' | 'aleatoria';

export const TIPOS_DE_CHAVE_PIX: TipoDaChavePix[] = [
  'celular',
  'cpf',
  'cnpj',
  'email',
  'aleatoria',
];

const ROTULOS_DE_CHAVE_PIX: Record<TipoDaChavePix, string> = {
  celular: 'Celular',
  cpf: 'CPF',
  cnpj: 'CNPJ',
  email: 'E-mail',
  aleatoria: 'Aleatória',
};

export const rotuloDaChavePix = (tipo: TipoDaChavePix): string => ROTULOS_DE_CHAVE_PIX[tipo];

export const tipoDaChavePix = (valor: string | null | undefined): TipoDaChavePix =>
  TIPOS_DE_CHAVE_PIX.find((tipo) => tipo === valor) ?? 'aleatoria';

const digitosDe = (chave: string): string => chave.replace(/\D/g, '');

export function normalizarChavePix(chave: string, tipo: TipoDaChavePix): string {
  const limpa = chave.trim();
  if (tipo === 'email') return limpa.toLowerCase();
  if (tipo === 'aleatoria') return limpa.toLowerCase();
  if (tipo === 'cpf' || tipo === 'cnpj') return digitosDe(limpa);

  const digitos = digitosDe(limpa);
  const semPais = digitos.startsWith('55') && digitos.length > 11 ? digitos.slice(2) : digitos;
  return semPais === '' ? '' : `+55${semPais}`;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

export function chavePixValida(chave: string, tipo: TipoDaChavePix): boolean {
  const pronta = normalizarChavePix(chave, tipo);
  if (pronta === '') return false;
  if (tipo === 'cpf') return pronta.length === 11;
  if (tipo === 'cnpj') return pronta.length === 14;
  if (tipo === 'celular') return pronta.length === 13 || pronta.length === 14;
  if (tipo === 'email') return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(pronta) && pronta.length <= 77;
  return UUID.test(pronta);
}

export function gerarBrCode(
  chave: string,
  tipo: TipoDaChavePix,
  nome: string,
  cidade: string,
  valorCentavos = 0,
  identificador = '***',
): string {
  const nomeLimpo = sanear(nome, LIMITE_NOME) || 'RECEBEDOR';
  const cidadeLimpa = sanear(cidade, LIMITE_CIDADE) || 'BRASIL';
  const txid = sanear(identificador.replace(/\*/g, 'A'), LIMITE_TXID) || '***';

  let corpo = campo('00', '01');
  corpo += campo('26', campo('00', GUI_PIX) + campo('01', normalizarChavePix(chave, tipo)));
  corpo += campo('52', '0000');
  corpo += campo('53', '986');
  if (valorCentavos > 0) corpo += campo('54', valorFormatado(valorCentavos));
  corpo += campo('58', 'BR');
  corpo += campo('59', nomeLimpo);
  corpo += campo('60', cidadeLimpa);
  corpo += campo('62', campo('05', txid));

  const comMarcador = `${corpo}6304`;
  return comMarcador + crc16(comMarcador).toString(16).toUpperCase().padStart(4, '0');
}

export const reaisDe = (centavos: number): string =>
  (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
