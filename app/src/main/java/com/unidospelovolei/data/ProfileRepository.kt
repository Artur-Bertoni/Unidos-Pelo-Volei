package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Perfil do usuario logado, lido do SQLite local.
 *
 * As sync rules mandam para o dispositivo apenas o proprio profile, e com ele a
 * flag `is_admin` que libera as telas de edicao.
 */
class ProfileRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeProfile(userId: String): Flow<UserProfile?> =
        db
            .watch(
                "SELECT id, email, nome, is_admin FROM profiles WHERE id = ?",
                listOf(userId),
            ) { it.toUserProfile() }
            .map { it.firstOrNull() }
}
