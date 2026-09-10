package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.SqlCursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T : Any> PowerSyncDatabase.observarNoGrupo(
    grupoAtivo: GrupoAtivo,
    sql: String,
    parametros: List<Any?> = emptyList(),
    vezesDoGrupo: Int = 1,
    mapper: (SqlCursor) -> T,
): Flow<List<T>> =
    grupoAtivo.id.flatMapLatest { grupo ->
        if (grupo == null) {
            flowOf(emptyList())
        } else {
            watch(
                sql = sql,
                parameters = List(vezesDoGrupo) { grupo } + parametros,
                mapper = mapper,
            )
        }
    }
