package com.unidospelovolei.domain.scheduling

import com.unidospelovolei.domain.model.Team
import kotlin.random.Random

data class ScheduledMatch(
    val quadra: Int,
    val teamA: Team,
    val teamB: Team,
)

data class ScheduledRound(
    val numero: Int,
    val fase: Int,
    val matches: List<ScheduledMatch>,
    val folgam: List<Team>,
)

private typealias Par = Set<String>

object RoundRobinScheduler {
    const val TIMES_POR_QUADRA: Int = 2

    private const val TENTATIVAS = 12
    private const val ORCAMENTO_BUSCA = 20_000
    private const val SEM_LIMITE = Int.MAX_VALUE

    fun generate(
        teams: List<Team>,
        quadras: Int,
        random: Random = Random.Default,
    ): List<ScheduledRound> {
        val times = teams.filter { it.ativo }.sortedWith(compareBy({ it.ordem }, { it.nome.lowercase() }))
        val porRodada = minOf(quadras, times.size / TIMES_POR_QUADRA)
        if (porRodada < 1) return emptyList()

        val melhor =
            (1..TENTATIVAS)
                .map { montar(times, porRodada, random) }
                .minWithOrNull(
                    compareBy({ it.repetidos }, { it.rodadas.size }, { it.maiorSequencia }),
                ) ?: return emptyList()

        return numerar(melhor.rodadas, times, porRodada)
    }

    private data class Tentativa(
        val rodadas: List<List<ScheduledMatch>>,
        val repetidos: Int,
        val maiorSequencia: Int,
    )

    private fun montar(
        times: List<Team>,
        porRodada: Int,
        random: Random,
    ): Tentativa {
        val pendentes = paresDe(times).toMutableSet()
        val repeticoes = mutableMapOf<Par, Int>()
        val seguidos = times.associate { it.id to 0 }.toMutableMap()
        val descanso = times.associate { it.id to 0 }.toMutableMap()
        val limite = limiteDeSeguidos(times.size, porRodada)
        val teto = pendentes.size * 2

        val rodadas = mutableListOf<List<ScheduledMatch>>()
        var repetidos = 0
        var maiorSequencia = 0

        while (pendentes.isNotEmpty() && rodadas.size < teto) {
            val duplas =
                escolherRodada(times, porRodada, pendentes, repeticoes, seguidos, descanso, limite, random)
            if (duplas.isEmpty()) break

            rodadas +=
                duplas.mapIndexed { indice, (a, b) ->
                    ScheduledMatch(quadra = indice + 1, teamA = a, teamB = b)
                }

            duplas.forEach { (a, b) ->
                val chave = par(a, b)
                if (!pendentes.remove(chave)) repetidos++
                repeticoes[chave] = (repeticoes[chave] ?: 0) + 1
            }

            val jogando = duplas.flatMap { listOf(it.first.id, it.second.id) }.toSet()
            times.forEach { time ->
                if (time.id in jogando) {
                    val sequencia = seguidos.getValue(time.id) + 1
                    seguidos[time.id] = sequencia
                    descanso[time.id] = 0
                    maiorSequencia = maxOf(maiorSequencia, sequencia)
                } else {
                    seguidos[time.id] = 0
                    descanso[time.id] = descanso.getValue(time.id) + 1
                }
            }
        }

        return Tentativa(rodadas, repetidos, maiorSequencia)
    }

    private fun escolherRodada(
        times: List<Team>,
        porRodada: Int,
        pendentes: Set<Par>,
        repeticoes: Map<Par, Int>,
        seguidos: Map<String, Int>,
        descanso: Map<String, Int>,
        limite: Int,
        random: Random,
    ): List<Pair<Team, Team>> {
        val opcoes = candidatos(times, pendentes, repeticoes, seguidos, descanso, limite, random)
        return buscar(opcoes, porRodada, 0, mutableSetOf(), intArrayOf(ORCAMENTO_BUSCA)) ?: emptyList()
    }

    private fun candidatos(
        times: List<Team>,
        pendentes: Set<Par>,
        repeticoes: Map<Par, Int>,
        seguidos: Map<String, Int>,
        descanso: Map<String, Int>,
        limite: Int,
        random: Random,
    ): List<Pair<Team, Team>> {
        val adversariosQueFaltam =
            times.associate { time -> time.id to pendentes.count { time.id in it } }

        fun excesso(id: String): Int =
            if (limite == SEM_LIMITE) 0 else (seguidos.getValue(id) + 1 - limite).coerceAtLeast(0)

        return duplasDe(times)
            .shuffled(random)
            .sortedWith(
                compareBy<Pair<Team, Team>> { par(it.first, it.second) !in pendentes }
                    .thenBy { excesso(it.first.id) + excesso(it.second.id) }
                    .thenBy { repeticoes[par(it.first, it.second)] ?: 0 }
                    .thenByDescending { descanso.getValue(it.first.id) + descanso.getValue(it.second.id) }
                    .thenByDescending {
                        adversariosQueFaltam.getValue(it.first.id) +
                            adversariosQueFaltam.getValue(it.second.id)
                    },
            )
    }

    private fun buscar(
        opcoes: List<Pair<Team, Team>>,
        faltam: Int,
        inicio: Int,
        usados: MutableSet<String>,
        orcamento: IntArray,
    ): List<Pair<Team, Team>>? {
        if (faltam == 0) return emptyList()
        if (orcamento[0]-- <= 0) return null

        for (indice in inicio until opcoes.size) {
            val (a, b) = opcoes[indice]
            if (a.id in usados || b.id in usados) continue

            usados += a.id
            usados += b.id
            val resto = buscar(opcoes, faltam - 1, indice + 1, usados, orcamento)
            usados -= a.id
            usados -= b.id

            if (resto != null) return listOf(opcoes[indice]) + resto
            if (orcamento[0] <= 0) return null
        }
        return null
    }

    private fun numerar(
        rodadas: List<List<ScheduledMatch>>,
        times: List<Team>,
        porRodada: Int,
    ): List<ScheduledRound> {
        val bloco = times.size / mdc(times.size, TIMES_POR_QUADRA * porRodada)
        return rodadas.mapIndexed { indice, partidas ->
            val jogando = partidas.flatMap { listOf(it.teamA.id, it.teamB.id) }.toSet()
            ScheduledRound(
                numero = indice + 1,
                fase = indice / bloco + 1,
                matches = partidas,
                folgam = times.filter { it.id !in jogando },
            )
        }
    }

    private fun limiteDeSeguidos(
        total: Int,
        porRodada: Int,
    ): Int {
        val emQuadra = TIMES_POR_QUADRA * porRodada
        val descansam = total - emQuadra
        return if (descansam <= 0) SEM_LIMITE else (emQuadra + descansam - 1) / descansam
    }

    private tailrec fun mdc(
        a: Int,
        b: Int,
    ): Int = if (b == 0) a else mdc(b, a % b)

    private fun par(
        a: Team,
        b: Team,
    ): Par = setOf(a.id, b.id)

    private fun paresDe(times: List<Team>): List<Par> = duplasDe(times).map { par(it.first, it.second) }

    private fun duplasDe(times: List<Team>): List<Pair<Team, Team>> =
        buildList {
            times.forEachIndexed { i, a ->
                for (j in i + 1 until times.size) add(a to times[j])
            }
        }
}
