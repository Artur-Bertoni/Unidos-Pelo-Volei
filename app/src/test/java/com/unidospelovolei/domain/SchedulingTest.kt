package com.unidospelovolei.domain

import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.scheduling.RoundRobinScheduler
import com.unidospelovolei.domain.scheduling.SnakeDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingTest {
    private fun times(quantidade: Int): List<Team> =
        (1..quantidade).map {
            Team(id = "t$it", nome = "Time $it", corHex = "#FFFFFF", sigla = "T$it", ativo = true, ordem = it)
        }

    private fun jogadores(niveis: List<Int>): List<Player> =
        niveis.mapIndexed { indice, nivel ->
            Player(id = "p$indice", nome = "Jogador $indice", skillLevel = nivel, ativo = true)
        }

    @Test
    fun `snake draft mantem os times equilibrados`() {
        val elencos = SnakeDraft.distribute(jogadores(List(36) { 5 - it / 8 }), times(9))

        assertEquals(9, elencos.size)
        assertEquals(36, elencos.sumOf { it.players.size })
        val forcas = elencos.map { it.forcaTotal }
        assertTrue("forcas=$forcas", forcas.max() - forcas.min() <= 2)
    }

    @Test
    fun `snake draft ignora jogadores inativos`() {
        val comInativo =
            jogadores(List(9) { 3 }) + Player(id = "x", nome = "Fora", skillLevel = 5, ativo = false)
        val elencos = SnakeDraft.distribute(comInativo, times(3))

        assertEquals(9, elencos.sumOf { it.players.size })
    }

    @Test
    fun `chaveamento de 9 times em 3 quadras cobre o round-robin completo`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3)

        val confrontos =
            rodadas
                .flatMap { it.matches }
                .map { setOf(it.teamA.id, it.teamB.id) }

        // 9 times: 9 * 8 / 2 = 36 confrontos, todos distintos.
        assertEquals(36, confrontos.size)
        assertEquals(36, confrontos.toSet().size)
        assertEquals(12, rodadas.size)
        assertTrue(rodadas.all { it.matches.size == 3 })
    }

    @Test
    fun `nenhum time joga duas vezes na mesma rodada e as folgas fecham`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3)

        rodadas.forEach { rodada ->
            val jogando = rodada.matches.flatMap { listOf(it.teamA.id, it.teamB.id) }
            assertEquals(jogando.size, jogando.toSet().size)
            assertEquals(3, rodada.folgam.size)
            assertEquals(9, jogando.size + rodada.folgam.size)
        }
    }

    @Test
    fun `fases agrupam um ciclo completo de folgas`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3)

        // 3 times folgam por rodada, entao a fase tem 3 rodadas: 12 rodadas = 4 fases.
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4), rodadas.map { it.fase })
    }

    @Test
    fun `numero par de times sem folga tambem funciona`() {
        val rodadas = RoundRobinScheduler.generate(times(6), quadras = 3)

        assertEquals(5, rodadas.size)
        assertTrue(rodadas.all { it.matches.size == 3 && it.folgam.isEmpty() })
        assertEquals(15, rodadas.sumOf { it.matches.size })
    }

    @Test
    fun `mais quadras do que confrontos simultaneos nao quebra`() {
        val rodadas = RoundRobinScheduler.generate(times(4), quadras = 5)

        assertEquals(6, rodadas.sumOf { it.matches.size })
        assertTrue(rodadas.all { it.matches.size <= 2 })
    }
}
