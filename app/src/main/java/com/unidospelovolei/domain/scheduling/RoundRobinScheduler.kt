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
    const val TIMES_POR_QUADRA: Int = 3

    private const val MAX_FASES = 80
    private const val ORCAMENTO_BUSCA = 40_000

    fun generate(
        teams: List<Team>,
        quadras: Int,
        random: Random = Random.Default,
    ): List<ScheduledRound> {
        val times = teams.filter { it.ativo }.sortedWith(compareBy({ it.ordem }, { it.nome.lowercase() }))
        if (times.size < 2 || quadras < 1) return emptyList()

        val tamanhos = tamanhosDosGrupos(times.size, quadras)
        if (tamanhos.isEmpty()) return emptyList()

        val porFase = tamanhos.sum()
        val comTrios = tamanhos.contains(TIMES_POR_QUADRA)
        val pendentes = paresDe(times).toMutableSet()
        val foraDaFase = times.associate { it.id to 0 }.toMutableMap()
        val sequencia = times.associate { it.id to 0 }.toMutableMap()

        val rodadas = mutableListOf<ScheduledRound>()
        var fase = 1
        var numero = 1

        while (pendentes.isNotEmpty() && fase <= MAX_FASES) {
            val descanso = sequencia.takeIf { comTrios }
            val escalados = escalarParaAFase(times, porFase, foraDaFase, pendentes, random)
            val comAtraso =
                particionar(escalados, tamanhos, pendentes, descanso, random)
                    .map { grupo -> grupo.sortedBy { sequencia.getValue(it.id) } }
                    .map { grupo -> grupo to if (cabeNaSequencia(grupo, descanso)) 0 else 1 }
                    .sortedBy { (_, atraso) -> atraso }

            val grupos = comAtraso.map { it.first }
            val atrasos = comAtraso.map { it.second }

            val ineditos = grupos.sumOf { grupo -> paresDe(grupo).count { it in pendentes } }
            if (ineditos == 0 && rodadas.isNotEmpty()) break

            val rodadasNaFase =
                grupos.indices.maxOf { atrasos[it] + ordemDosConfrontos(grupos[it].size).size }

            repeat(rodadasNaFase) { indice ->
                val partidas =
                    grupos.mapIndexedNotNull { quadra, grupo ->
                        ordemDosConfrontos(grupo.size).getOrNull(indice - atrasos[quadra])?.let { (a, b) ->
                            ScheduledMatch(quadra = quadra + 1, teamA = grupo[a], teamB = grupo[b])
                        }
                    }
                val jogando = partidas.flatMap { listOf(it.teamA.id, it.teamB.id) }.toSet()

                rodadas +=
                    ScheduledRound(
                        numero = numero++,
                        fase = fase,
                        matches = partidas,
                        folgam = times.filter { it.id !in jogando },
                    )

                times.forEach { time ->
                    sequencia[time.id] = if (time.id in jogando) sequencia.getValue(time.id) + 1 else 0
                }
                partidas.forEach { pendentes -= par(it.teamA, it.teamB) }
            }

            val dentro = escalados.map { it.id }.toSet()
            times.forEach { time ->
                foraDaFase[time.id] = if (time.id in dentro) 0 else foraDaFase.getValue(time.id) + 1
            }
            fase++
        }

        return rodadas
    }

    private fun tamanhosDosGrupos(
        total: Int,
        quadras: Int,
    ): List<Int> {
        if (total <= 2 * quadras) return List(total / 2) { 2 }

        val trios = minOf(quadras, total / TIMES_POR_QUADRA)
        if (trios == 0) return List(total / 2) { 2 }

        val grupos = MutableList(trios) { TIMES_POR_QUADRA }
        if (grupos.size < quadras) {
            when (total - trios * TIMES_POR_QUADRA) {
                2 -> grupos += 2
                1 -> {
                    grupos[grupos.size - 1] = 2
                    grupos += 2
                }
            }
        }
        return grupos
    }

    private fun ordemDosConfrontos(tamanho: Int): List<Pair<Int, Int>> =
        when (tamanho) {
            3 -> listOf(0 to 1, 0 to 2, 1 to 2)
            2 -> listOf(0 to 1)
            else -> emptyList()
        }

    private fun escalarParaAFase(
        times: List<Team>,
        quantidade: Int,
        foraDaFase: Map<String, Int>,
        pendentes: Set<Par>,
        random: Random,
    ): List<Team> {
        if (quantidade >= times.size) return times
        return times
            .shuffled(random)
            .sortedWith(
                compareByDescending<Team> { foraDaFase.getValue(it.id) }
                    .thenByDescending { time -> pendentes.count { time.id in it } },
            ).take(quantidade)
    }

    private fun particionar(
        disponiveis: List<Team>,
        tamanhos: List<Int>,
        pendentes: Set<Par>,
        sequencia: Map<String, Int>?,
        random: Random,
    ): List<List<Team>> {
        val embaralhados = disponiveis.shuffled(random)
        return buscarSemRepetir(embaralhados, tamanhos, pendentes, sequencia, intArrayOf(ORCAMENTO_BUSCA))
            ?: buscarSemRepetir(embaralhados, tamanhos, pendentes, null, intArrayOf(ORCAMENTO_BUSCA))
            ?: agruparPermitindoRepetir(disponiveis, tamanhos, pendentes, random)
    }

    private fun buscarSemRepetir(
        restantes: List<Team>,
        tamanhos: List<Int>,
        pendentes: Set<Par>,
        sequencia: Map<String, Int>?,
        orcamento: IntArray,
    ): List<List<Team>>? {
        if (tamanhos.isEmpty()) return emptyList()
        if (orcamento[0]-- <= 0) return null

        val ancora = restantes.first()
        combinacoes(restantes.drop(1), tamanhos.first() - 1).forEach { companheiros ->
            val grupo = listOf(ancora) + companheiros
            if (paresDe(grupo).all { it in pendentes } && cabeNaSequencia(grupo, sequencia)) {
                val sobra = restantes.filterNot { time -> grupo.any { it.id == time.id } }
                val demais = buscarSemRepetir(sobra, tamanhos.drop(1), pendentes, sequencia, orcamento)
                if (demais != null) return listOf(grupo) + demais
            }
            if (orcamento[0] <= 0) return null
        }
        return null
    }

    private fun cabeNaSequencia(
        grupo: List<Team>,
        sequencia: Map<String, Int>?,
    ): Boolean {
        if (sequencia == null) return true
        val seguidos = grupo.map { sequencia.getValue(it.id) }.sorted()
        return if (grupo.size < TIMES_POR_QUADRA) {
            seguidos.all { it <= 1 }
        } else {
            seguidos[0] == 0 && seguidos[1] <= 1
        }
    }

    private fun agruparPermitindoRepetir(
        disponiveis: List<Team>,
        tamanhos: List<Int>,
        pendentes: Set<Par>,
        random: Random,
    ): List<List<Team>> {
        val restantes = disponiveis.shuffled(random).toMutableList()
        return tamanhos.map { tamanho ->
            val ancora =
                restantes.maxBy { time ->
                    restantes.count { outro -> outro.id != time.id && par(time, outro) in pendentes }
                }
            restantes.remove(ancora)
            val grupo = mutableListOf(ancora)
            repeat(tamanho - 1) {
                val escolhido =
                    restantes.maxBy { candidato -> grupo.count { par(it, candidato) in pendentes } }
                grupo += escolhido
                restantes.remove(escolhido)
            }
            grupo.toList()
        }
    }

    private fun <T> combinacoes(
        itens: List<T>,
        k: Int,
    ): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (itens.size < k) return emptyList()

        val resultado = mutableListOf<List<T>>()
        fun montar(
            inicio: Int,
            atual: MutableList<T>,
        ) {
            if (atual.size == k) {
                resultado += atual.toList()
                return
            }
            for (i in inicio..itens.size - (k - atual.size)) {
                atual += itens[i]
                montar(i + 1, atual)
                atual.removeAt(atual.size - 1)
            }
        }
        montar(0, mutableListOf())
        return resultado
    }

    private fun par(
        a: Team,
        b: Team,
    ): Par = setOf(a.id, b.id)

    private fun paresDe(times: List<Team>): List<Par> =
        buildList {
            times.forEachIndexed { i, a ->
                for (j in i + 1 until times.size) add(par(a, times[j]))
            }
        }
}
