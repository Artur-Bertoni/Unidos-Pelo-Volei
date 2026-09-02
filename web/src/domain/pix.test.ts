import { describe, expect, it } from 'vitest';
import { campo, crc16, gerarBrCode, sanear, valorFormatado } from './pix';

describe('crc16', () => {
  it('bate com o vetor padrao do CRC16-CCITT-FALSE', () => {
    expect(crc16('123456789')).toBe(0x29b1);
  });

  it('muda quando o payload muda', () => {
    expect(crc16('123456789')).not.toBe(crc16('12345678A'));
  });
});

describe('campo', () => {
  it('prefixa o tamanho com dois digitos', () => {
    expect(campo('00', '01')).toBe('000201');
    expect(campo('01', 'chave@exemplo.com')).toBe('0117chave@exemplo.com');
  });
});

describe('sanear', () => {
  it('tira acento, pontuacao e corta no limite', () => {
    expect(sanear('José da Silva-Júnior', 25)).toBe('JOSE DA SILVAJUNIOR');
    expect(sanear('São Paulo', 15)).toBe('SAO PAULO');
    expect(sanear('Cidade Muito Comprida Demais', 15)).toBe('CIDADE MUITO CO');
  });
});

describe('valorFormatado', () => {
  it('escreve centavos com duas casas', () => {
    expect(valorFormatado(5000)).toBe('50.00');
    expect(valorFormatado(2550)).toBe('25.50');
    expect(valorFormatado(5)).toBe('0.05');
  });
});

describe('gerarBrCode', () => {
  const chave = 'unidos@volei.com';

  it('abre com o indicador de formato e fecha com o CRC', () => {
    const codigo = gerarBrCode(chave, 'Unidos Pelo Volei', 'Blumenau');
    expect(codigo.startsWith('000201')).toBe(true);
    expect(codigo.slice(-8, -4)).toBe('6304');
    expect(/^[0-9A-F]{4}$/.test(codigo.slice(-4))).toBe(true);
  });

  it('carrega o GUI e a chave do Pix', () => {
    const codigo = gerarBrCode(chave, 'Unidos Pelo Volei', 'Blumenau');
    expect(codigo).toContain('0014br.gov.bcb.pix');
    expect(codigo).toContain(`0116${chave}`);
  });

  it('so inclui o valor quando ele existe', () => {
    expect(gerarBrCode(chave, 'Unidos', 'Blumenau', 0)).not.toContain('5405');
    expect(gerarBrCode(chave, 'Unidos', 'Blumenau', 5000)).toContain('540550.00');
  });

  it('o CRC declarado confere com o corpo', () => {
    const codigo = gerarBrCode(chave, 'Unidos Pelo Volei', 'Blumenau', 2550);
    const corpo = codigo.slice(0, -4);
    const declarado = codigo.slice(-4);
    expect(crc16(corpo).toString(16).toUpperCase().padStart(4, '0')).toBe(declarado);
  });

  it('cai para valores seguros quando nome e cidade vem vazios', () => {
    const codigo = gerarBrCode(chave, '   ', '');
    expect(codigo).toContain('5909RECEBEDOR');
    expect(codigo).toContain('6006BRASIL');
  });
});
