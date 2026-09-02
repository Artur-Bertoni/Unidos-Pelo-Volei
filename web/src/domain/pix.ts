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

export function gerarBrCode(
  chave: string,
  nome: string,
  cidade: string,
  valorCentavos = 0,
  identificador = '***',
): string {
  const nomeLimpo = sanear(nome, LIMITE_NOME) || 'RECEBEDOR';
  const cidadeLimpa = sanear(cidade, LIMITE_CIDADE) || 'BRASIL';
  const txid = sanear(identificador.replace(/\*/g, 'A'), LIMITE_TXID) || '***';

  let corpo = campo('00', '01');
  corpo += campo('26', campo('00', GUI_PIX) + campo('01', chave.trim()));
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
