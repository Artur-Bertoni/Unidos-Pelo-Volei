import type { Regime, VinculoPedido } from '../domain/models';
import { db } from '../lib/powersync/db';
import { agoraIso, novoId } from './mappers';

export async function pedirVinculo(
  profileId: string,
  profileNome: string | null,
  playerId: string,
): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM vinculo_pedidos WHERE profile_id = ? AND status = ?', [
      profileId,
      'pendente',
    ]);
    await tx.execute(
      `INSERT INTO vinculo_pedidos (id, profile_id, player_id, profile_nome, status, criado_em)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [novoId(), profileId, playerId, profileNome, 'pendente', agoraIso()],
    );
  });
}

export async function cancelarPedido(pedidoId: string): Promise<void> {
  await db.execute('DELETE FROM vinculo_pedidos WHERE id = ?', [pedidoId]);
}

export async function decidirPedido(
  pedido: VinculoPedido,
  aprovado: boolean,
  decididoPor: string,
): Promise<void> {
  const agora = agoraIso();
  await db.writeTransaction(async (tx) => {
    await tx.execute(
      `UPDATE vinculo_pedidos
       SET status = ?, decidido_por = ?, decidido_em = ?
       WHERE id = ?`,
      [aprovado ? 'aprovado' : 'recusado', decididoPor, agora, pedido.id],
    );
    if (aprovado) {
      await tx.execute(
        'UPDATE players SET profile_id = NULL, updated_at = ? WHERE profile_id = ? AND id <> ?',
        [agora, pedido.profileId, pedido.playerId],
      );
      await tx.execute('UPDATE players SET profile_id = ?, updated_at = ? WHERE id = ?', [
        pedido.profileId,
        agora,
        pedido.playerId,
      ]);
    }
  });
}

export async function salvarFicha(
  playerId: string,
  nome: string,
  nascimentoDia: number | null,
  nascimentoMes: number | null,
  regime: Regime,
): Promise<void> {
  await db.execute(
    `UPDATE players
     SET nome = ?, nascimento_dia = ?, nascimento_mes = ?, regime = ?, updated_at = ?
     WHERE id = ?`,
    [nome.trim(), nascimentoDia, nascimentoMes, regime, agoraIso(), playerId],
  );
}

export async function vincularJogador(
  playerId: string,
  profileId: string | null,
): Promise<void> {
  const agora = agoraIso();
  await db.writeTransaction(async (tx) => {
    if (profileId !== null) {
      await tx.execute(
        'UPDATE players SET profile_id = NULL, updated_at = ? WHERE profile_id = ? AND id <> ?',
        [agora, profileId, playerId],
      );
      await tx.execute('DELETE FROM vinculo_pedidos WHERE profile_id = ? AND status = ?', [
        profileId,
        'pendente',
      ]);
    }
    await tx.execute('UPDATE players SET profile_id = ?, updated_at = ? WHERE id = ?', [
      profileId,
      agora,
      playerId,
    ]);
  });
}

const limpar = (valor: string | null): string | null => {
  const texto = valor?.trim() ?? '';
  return texto.length > 0 ? texto : null;
};

export async function salvarContato(
  playerId: string,
  profileId: string,
  telefone: string | null,
  contatoEmergencia: string | null,
  nascimentoAno: number | null,
): Promise<void> {
  const fone = limpar(telefone);
  const emergencia = limpar(contatoEmergencia);
  const agora = agoraIso();

  await db.writeTransaction(async (tx) => {
    const existente = await tx.getOptional<{ id: string }>(
      'SELECT id FROM player_contatos WHERE player_id = ?',
      [playerId],
    );

    if (existente) {
      await tx.execute(
        `UPDATE player_contatos
         SET telefone = ?, contato_emergencia = ?, nascimento_ano = ?, atualizado_em = ?
         WHERE id = ?`,
        [fone, emergencia, nascimentoAno, agora, existente.id],
      );
    } else {
      await tx.execute(
        `INSERT INTO player_contatos (
           id, player_id, profile_id, telefone, contato_emergencia, nascimento_ano, atualizado_em
         ) VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [novoId(), playerId, profileId, fone, emergencia, nascimentoAno, agora],
      );
    }
  });
}
