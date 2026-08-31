package com.unidospelovolei.domain.scheduling

import com.unidospelovolei.domain.model.Team
import kotlin.math.max

/** Um confronto gerado pelo chaveamento, ainda sem id de banco. */
data class ScheduledMatch(
    val quadra: Int,
    val teamA: Team,
    val teamB: Team,
)

/** Uma rodada gerada: N partidas (N = quadras) e os times que folgam. */
data class ScheduledRound(
    val numero: Int,
    val fase: Int,
    val matches: List<ScheduledMatch>,
    val folgam: List<Team>,
)

/**
 * Geracao do chaveamento em round-robin pelo metodo do circulo.
 *
 * O metodo do circulo fixa o primeiro time e gira os demais, produzindo todos os
 * confrontos possiveis sem repeticao. Com numero impar de times entra um
 * "fantasma": quem for sorteado contra ele simplesmente nao joga aquele
 * confronto.
 *
 * O metodo do circulo, sozinho, produz rodadas com ate `times / 2` partidas
 * simultaneas -- mais do que cabe nas quadras disponiveis. Por isso os
 * confrontos gerados entram numa fila e sao empacotados em rodadas de no maximo
 * `quadras` partidas, sempre respeitando a regra de um time jogar no maximo uma
 * vez por rodada. Os times que sobram folgam.
 *
 * A habilidade dos jogadores nao entra aqui: ela so influencia a formacao dos
 * times (ver [SnakeDraft]).
 */
object RoundRobinScheduler {
    fun generate(
        teams: List<Team>,
        quadras: Int,
    ): List<ScheduledRound> {
        val timesEmOrdem = teams.filter { it.ativo }.sortedWith(compareBy({ it.ordem }, { it.nome.lowercase() }))
        if (timesEmOrdem.size < 2 || quadras < 1) return emptyList()

        val quadrasUteis = minOf(quadras, timesEmOrdem.size / 2)
        val fila = confrontosPeloMetodoDoCirculo(timesEmOrdem).toMutableList()

        val rodadas = mutableListOf<List<Pair<Team, Team>>>()
        while (fila.isNotEmpty()) {
            val rodada = mutableListOf<Pair<Team, Team>>()
            val ocupados = mutableSetOf<String>()
            val iterador = fila.iterator()
            while (iterador.hasNext() && rodada.size < quadrasUteis) {
                val confronto = iterador.next()
                val (a, b) = confronto
                if (a.id in ocupados || b.id in ocupados) continue
                rodada += confronto
                ocupados += a.id
                ocupados += b.id
                iterador.remove()
            }
            // Defesa contra laco infinito: se nada coube, encerra.
            if (rodada.isEmpty()) break
            rodadas += rodada
        }

        val rodadasPorFase = rodadasPorFase(timesEmOrdem.size, quadrasUteis, rodadas.size)

        return rodadas.mapIndexed { indice, confrontos ->
            val jogando = confrontos.flatMap { listOf(it.first.id, it.second.id) }.toSet()
            ScheduledRound(
                numero = indice + 1,
                fase = indice / rodadasPorFase + 1,
                matches = confrontos.mapIndexed { quadra, (a, b) -> ScheduledMatch(quadra + 1, a, b) },
                folgam = timesEmOrdem.filter { it.id !in jogando },
            )
        }
    }

    /**
     * Uma fase e um ciclo completo de folgas: o numero de rodadas necessario
     * para que todos os times tenham folgado ao menos uma vez. Com 9 times e 3
     * quadras folgam 3 times por rodada, entao a fase tem 3 rodadas.
     */
    private fun rodadasPorFase(
        totalTimes: Int,
        quadras: Int,
        totalRodadas: Int,
    ): Int {
        val folgamPorRodada = totalTimes - quadras * 2
        if (folgamPorRodada <= 0) return max(1, totalRodadas)
        val ciclo = (totalTimes + folgamPorRodada - 1) / folgamPorRodada
        return max(1, ciclo)
    }

    /**
     * Metodo do circulo: fixa o primeiro elemento e gira os demais no sentido
     * horario, gerando todos os confrontos possiveis.
     */
    private fun confrontosPeloMetodoDoCirculo(teams: List<Team>): List<Pair<Team, Team>> {
        // Com numero impar de times entra um "fantasma" (null): quem cai contra ele folga.
        val roda: MutableList<Team?> = teams.toMutableList<Team?>()
        if (teams.size % 2 == 1) roda += null

        val total = roda.size
        val confrontos = mutableListOf<Pair<Team, Team>>()

        repeat(total - 1) {
            for (i in 0 until total / 2) {
                val a = roda[i]
                val b = roda[total - 1 - i]
                if (a != null && b != null) confrontos += a to b
            }
            // Gira mantendo a posicao 0 fixa.
            val ultimo = roda.removeAt(total - 1)
            roda.add(1, ultimo)
        }

        return confrontos
    }
}
