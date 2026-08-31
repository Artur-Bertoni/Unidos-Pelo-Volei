package com.unidospelovolei.data

import java.time.Instant
import java.util.UUID

/** O id de toda linha sincronizada e gerado no cliente: o PowerSync exige um id na hora do INSERT. */
internal fun novoId(): String = UUID.randomUUID().toString()

/** Timestamp no mesmo formato ISO-8601 que o Postgres devolve pela replicacao. */
internal fun agoraIso(): String = Instant.now().toString()
