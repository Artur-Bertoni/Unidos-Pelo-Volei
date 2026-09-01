package com.unidospelovolei.data

import java.time.Instant
import java.util.UUID

internal fun novoId(): String = UUID.randomUUID().toString()

internal fun agoraIso(): String = Instant.now().toString()
