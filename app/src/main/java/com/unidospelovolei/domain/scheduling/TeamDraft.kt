package com.unidospelovolei.domain.scheduling

import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TeamRoster
import kotlin.math.abs
import kotlin.random.Random

data class ElencoPassado(
    val jogadores: List<String>,
    val peso: Double,
)

class HistoricoDeDuplas private constructor(
    private val pesos: Map<String, Map<String, Double>>,
) {
    fun peso(
        a: String,
        b: String,
    ): Double = pesos[a]?.get(b) ?: 0.0

    fun penalidade(
        jogadorId: String,
        elenco: List<Player>,
    ): Double = elenco.sumOf { peso(jogadorId, it.id) }

    fun repeticoes(elencos: List<List<Player>>): Double =
        elencos.sumOf { elenco ->
            var soma = 0.0
            elenco.forEachIndexed { i, a ->
                for (j in i + 1 until elenco.size) soma += peso(a.id, elenco[j].id)
            }
            soma
        }

    companion object {
        val VAZIO = HistoricoDeDuplas(emptyMap())

        fun de(elencos: List<ElencoPassado>): HistoricoDeDuplas {
            val acumulado = mutableMapOf<String, MutableMap<String, Double>>()
            elencos.forEach { elenco ->
                elenco.jogadores.forEachIndexed { i, a ->
                    for (j in i + 1 until elenco.jogadores.size) {
                        val b = elenco.jogadores[j]
                        acumulado.getOrPut(a) { mutableMapOf() }.merge(b, elenco.peso, Double::plus)
                        acumulado.getOrPut(b) { mutableMapOf() }.merge(a, elenco.peso, Double::plus)
                    }
                }
            }
            return HistoricoDeDuplas(acumulado)
        }
    }
}

object TeamDraft {
    const val ALVO_POR_GENERO: Int = 2

    const val JOGADORES_POR_TIME: Int = ALVO_POR_GENERO * 2

    const val TENTATIVAS_PADRAO: Int = 240

    private const val PESO_HISTORICO_NA_ESCOLHA = 2.5
    private const val RUIDO_NA_ESCOLHA = 1.5

    private const val PESO_AMPLITUDE = 1.0
    private const val PESO_REPETICAO = 2.5
    private const val PESO_GENERO = 12.0

    fun distribute(
        players: List<Player>,
        teams: List<Team>,
        historico: HistoricoDeDuplas = HistoricoDeDuplas.VAZIO,
        random: Random = Random.Default,
        tentativas: Int = TENTATIVAS_PADRAO,
    ): List<TeamRoster> {
        val timesEmOrdem = teams.sortedWith(compareBy({ it.ordem }, { it.nome.lowercase() }))
        if (timesEmOrdem.isEmpty()) return emptyList()

        val ativos = players.filter { it.ativo }
        if (ativos.isEmpty()) return timesEmOrdem.map { TeamRoster(it, emptyList()) }

        var melhor: List<List<Player>> = montar(ativos, timesEmOrdem.size, historico, random)
        var melhorCusto = custo(melhor, historico)
        repeat((tentativas - 1).coerceAtLeast(0)) {
            val candidato = montar(ativos, timesEmOrdem.size, historico, random)
            val custo = custo(candidato, historico)
            if (custo < melhorCusto) {
                melhorCusto = custo
                melhor = candidato
            }
        }

        return timesEmOrdem.mapIndexed { indice, time ->
            TeamRoster(
                team = time,
                players =
                    melhor[indice].sortedWith(
                        compareByDescending<Player> { it.skillLevel }.thenBy { it.nome.lowercase() },
                    ),
            )
        }
    }

    private fun montar(
        ativos: List<Player>,
        totalTimes: Int,
        historico: HistoricoDeDuplas,
        random: Random,
    ): List<List<Player>> {
        val capacidade = capacidades(ativos.size, totalTimes, random)
        val porGenero = Genero.entries.associateWith { genero -> ativos.filter { it.genero == genero } }
        val vagas = cotasPorGenero(capacidade, porGenero.mapValues { it.value.size }, random)

        val elencos = List(totalTimes) { mutableListOf<Player>() }

        listOf(Genero.FEMININO, Genero.MASCULINO).forEach { genero ->
            val restante = vagas.getValue(genero)

            val fila = porGenero.getValue(genero).shuffled(random).sortedByDescending { it.skillLevel }
            fila.forEach { jogador ->
                val destino = melhorDestino(jogador, elencos, restante, capacidade, historico, random)
                elencos[destino] += jogador
                restante[destino] -= 1
            }
        }

        return elencos.map { it.toList() }
    }

    private fun capacidades(
        total: Int,
        totalTimes: Int,
        random: Random,
    ): IntArray {
        val capacidade = IntArray(totalTimes) { total / totalTimes }
        (0 until totalTimes).shuffled(random).take(total % totalTimes).forEach { capacidade[it] += 1 }
        return capacidade
    }

    private fun cotasPorGenero(
        capacidade: IntArray,
        totais: Map<Genero, Int>,
        random: Random,
    ): Map<Genero, IntArray> {
        val n = capacidade.size
        val feminino = IntArray(n) { minOf(ALVO_POR_GENERO, capacidade[it]) }
        val masculino = IntArray(n) { capacidade[it] - feminino[it] }
        val mulheres = totais[Genero.FEMININO] ?: 0

        var soma = feminino.sum()
        while (soma > mulheres) {
            val time = escolher(random, (0 until n).filter { feminino[it] > 0 }) { feminino[it] } ?: break
            feminino[time] -= 1
            masculino[time] += 1
            soma -= 1
        }
        while (soma < mulheres) {
            val time = escolher(random, (0 until n).filter { masculino[it] > 0 }) { -feminino[it] } ?: break
            feminino[time] += 1
            masculino[time] -= 1
            soma += 1
        }

        return mapOf(Genero.FEMININO to feminino, Genero.MASCULINO to masculino)
    }

    private fun escolher(
        random: Random,
        candidatos: List<Int>,
        chave: (Int) -> Int,
    ): Int? {
        if (candidatos.isEmpty()) return null
        val melhor = candidatos.maxOf(chave)
        return candidatos.filter { chave(it) == melhor }.random(random)
    }

    private fun melhorDestino(
        jogador: Player,
        elencos: List<List<Player>>,
        vagasDoGenero: IntArray,
        capacidade: IntArray,
        historico: HistoricoDeDuplas,
        random: Random,
    ): Int {
        val disponiveis =
            vagasDoGenero.indices.filter { vagasDoGenero[it] > 0 }.ifEmpty {
                elencos.indices.filter { elencos[it].size < capacidade[it] }.ifEmpty {
                    val menor = elencos.minOf { it.size }
                    elencos.indices.filter { elencos[it].size == menor }
                }
            }

        return disponiveis.minBy { time ->
            elencos[time].sumOf { it.skillLevel } +
                PESO_HISTORICO_NA_ESCOLHA * historico.penalidade(jogador.id, elencos[time]) +
                random.nextDouble() * RUIDO_NA_ESCOLHA
        }
    }

    private fun custo(
        elencos: List<List<Player>>,
        historico: HistoricoDeDuplas,
    ): Double {
        val forcas = elencos.map { elenco -> elenco.sumOf { it.skillLevel } }
        val amplitude = (forcas.max() - forcas.min()).toDouble()
        val desequilibrio =
            elencos.sumOf { elenco ->
                val mulheres = elenco.count { it.genero == Genero.FEMININO }
                abs(mulheres - minOf(ALVO_POR_GENERO, elenco.size))
            }.toDouble()

        return PESO_AMPLITUDE * amplitude +
            PESO_REPETICAO * historico.repeticoes(elencos) +
            PESO_GENERO * desequilibrio
    }
}
