package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeProfile(userId: String): Flow<UserProfile?> =
        db
            .watch(
                "SELECT id, email, nome, papel, is_admin FROM profiles WHERE id = ?",
                listOf(userId),
            ) { it.toUserProfile() }
            .map { it.firstOrNull() }
}
