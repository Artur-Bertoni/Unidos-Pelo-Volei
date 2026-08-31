package com.unidospelovolei.domain.scheduling

import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TeamRoster

/**
 * Distribuicao equilibrada de jogadores por habilidade (snake draft).
 *
 * Os jogadores ativos sao ordenados por `skillLevel` decrescente e distribuidos
 * em ordem serpenteante: a primeira leva vai do time 1 ao time N, a segunda
 * volta do time N ao time 1, e assim por diante. Esse vaivem faz com que quem
 * escolhe por ultimo em uma leva escolha primeiro na seguinte, o que mantem a
 * soma de habilidade dos times o mais proxima possivel.
 */
object SnakeDraft {
    fun distribute(
        players: List<Player>,
        teams: List<Team>,
    ): List<TeamRoster> {
        if (teams.isEmpty()) return emptyList()

        val ordenados =
            players
                .filter { it.ativo }
                .sortedWith(compareByDescending<Player> { it.skillLevel }.thenBy { it.nome.lowercase() })

        val timesEmOrdem = teams.sortedWith(compareBy({ it.ordem }, { it.nome.lowercase() }))
        val elencos = List(timesEmOrdem.size) { mutableListOf<Player>() }

        ordenados.forEachIndexed { indice, jogador ->
            val leva = indice / timesEmOrdem.size
            val posicaoNaLeva = indice % timesEmOrdem.size
            val destino =
                if (leva % 2 == 0) {
                    posicaoNaLeva
                } else {
                    timesEmOrdem.size - 1 - posicaoNaLeva
                }
            elencos[destino] += jogador
        }

        return timesEmOrdem.mapIndexed { indice, time -> TeamRoster(time, elencos[indice].toList()) }
    }
}
