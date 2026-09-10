package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.model.Papel
import com.unidospelovolei.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepository(
    private val db: PowerSyncDatabase,
    private val grupoAtivo: GrupoAtivo,
) {
    fun observeProfile(userId: String): Flow<UserProfile?> =
        combine(observeConta(userId), observePapel(userId)) { conta, papel ->
            conta?.copy(papel = papel)
        }

    private fun observeConta(userId: String): Flow<UserProfile?> =
        db
            .watch(
                "SELECT id, email, nome, 'atleta' AS papel FROM profiles WHERE id = ?",
                listOf(userId),
            ) { it.toUserProfile() }
            .map { it.firstOrNull() }

    private fun observePapel(userId: String): Flow<Papel> =
        grupoAtivo.id.flatMapLatest { grupo ->
            if (grupo == null) {
                flowOf(Papel.ATLETA)
            } else {
                db
                    .watch(
                        "SELECT papel FROM grupo_membros WHERE grupo_id = ? AND profile_id = ?",
                        listOf(grupo, userId),
                    ) { it.getString("papel") }
                    .map { Papel.from(it.firstOrNull()) }
            }
        }
}
