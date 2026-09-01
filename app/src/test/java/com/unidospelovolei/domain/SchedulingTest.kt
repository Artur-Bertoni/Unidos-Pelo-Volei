package com.unidospelovolei.domain

import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.scheduling.ElencoPassado
import com.unidospelovolei.domain.scheduling.HistoricoDeDuplas
import com.unidospelovolei.domain.scheduling.RoundRobinScheduler
import com.unidospelovolei.domain.scheduling.TeamDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SchedulingTest {
    private fun times(quantidade: Int): List<Team> =
        (1..quantidade).map {
            Team(id = "t$it", nome = "Time $it", corHex = "#FFFFFF", sigla = "T$it", ativo = true, ordem = it)
        }

    private fun jogadores(
        niveis: List<Int>,
        genero: Genero = Genero.MASCULINO,
        prefixo: String = "p",
    ): List<Player> =
        niveis.mapIndexed { indice, nivel ->
            Player(
                id = "$prefixo$indice",
                nome = "Jogador $prefixo$indice",
                skillLevel = nivel,
                genero = genero,
                ativo = true,
            )
        }

    private fun grupoMisto(
        homens: Int,
        mulheres: Int,
    ): List<Player> =
        jogadores(List(homens) { 5 - it % 5 }, Genero.MASCULINO, "h") +
            jogadores(List(mulheres) { 5 - it % 5 }, Genero.FEMININO, "m")

    @Test
    fun `sorteio mantem os times equilibrados`() {
        val elencos =
            TeamDraft.distribute(
                players = grupoMisto(homens = 18, mulheres = 18),
                teams = times(9),
                random = Random(7),
            )

        assertEquals(9, elencos.size)
        assertEquals(36, elencos.sumOf { it.players.size })
        val forcas = elencos.map { it.forcaTotal }
        assertTrue("forcas=$forcas", forcas.max() - forcas.min() <= 3)
    }

    @Test
    fun `sorteio poe 2 homens e 2 mulheres em cada time`() {
        val elencos =
            TeamDraft.distribute(
                players = grupoMisto(homens = 18, mulheres = 18),
                teams = times(9),
                random = Random(11),
            )

        elencos.forEach { elenco ->
            assertEquals("time ${elenco.team.nome}", 2, elenco.homens)
            assertEquals("time ${elenco.team.nome}", 2, elenco.mulheres)
        }
    }

    @Test
    fun `com menos mulheres do que vagas elas ficam espalhadas`() {
        val elencos =
            TeamDraft.distribute(
                players = grupoMisto(homens = 26, mulheres = 10),
                teams = times(9),
                random = Random(3),
            )

        assertEquals(10, elencos.sumOf { it.mulheres })
        assertTrue(
            "mulheres=${elencos.map { it.mulheres }}",
            elencos.all { it.mulheres >= 1 },
        )
    }

    @Test
    fun `sorteio ignora jogadores inativos`() {
        val comInativo =
            grupoMisto(homens = 5, mulheres = 4) +
                Player(id = "x", nome = "Fora", skillLevel = 5, genero = Genero.MASCULINO, ativo = false)

        val elencos = TeamDraft.distribute(comInativo, times(3), random = Random(1))

        assertEquals(9, elencos.sumOf { it.players.size })
        assertTrue(elencos.none { elenco -> elenco.players.any { it.id == "x" } })
    }

    @Test
    fun `dois sorteios seguidos nao devolvem os mesmos times`() {
        val jogadores = grupoMisto(homens = 18, mulheres = 18)
        val times = times(9)

        val primeiro = TeamDraft.distribute(jogadores, times, random = Random(21))
        val segundo = TeamDraft.distribute(jogadores, times, random = Random(99))

        assertNotEquals(
            primeiro.map { elenco -> elenco.players.map { it.id }.toSet() },
            segundo.map { elenco -> elenco.players.map { it.id }.toSet() },
        )
    }

    @Test
    fun `historico afasta quem ja jogou junto`() {
        val jogadores = grupoMisto(homens = 18, mulheres = 18)
        val times = times(9)

        val ontem = TeamDraft.distribute(jogadores, times, random = Random(5))
        val historico =
            HistoricoDeDuplas.de(
                ontem.map { elenco -> ElencoPassado(elenco.players.map { it.id }, peso = 1.0) },
            )

        val hoje = TeamDraft.distribute(jogadores, times, historico, random = Random(5))

        val duplasDeOntem = duplas(ontem.map { elenco -> elenco.players.map { it.id } })
        val duplasDeHoje = duplas(hoje.map { elenco -> elenco.players.map { it.id } })
        val repetidas = duplasDeHoje.count { it in duplasDeOntem }

        assertTrue("repetidas=$repetidas de ${duplasDeHoje.size}", repetidas <= 2)
    }

    private fun duplas(elencos: List<List<String>>): Set<Set<String>> =
        elencos
            .flatMap { elenco ->
                elenco.flatMapIndexed { i, a ->
                    elenco.drop(i + 1).map { b -> setOf(a, b) }
                }
            }.toSet()

    @Test
    fun `cada quadra recebe um trio que joga A-B, A-C e B-C na fase`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3, random = Random(4))

        rodadas
            .flatMap { rodada -> rodada.matches.map { rodada.fase to it } }
            .groupBy { (fase, partida) -> fase to partida.quadra }
            .forEach { (chave, partidas) ->
                val confrontos = partidas.map { (_, p) -> setOf(p.teamA.id, p.teamB.id) }
                val trio = confrontos.flatten().toSet()

                assertEquals("$chave", 3, trio.size)
                assertEquals("$chave", 3, confrontos.size)
                assertEquals(
                    "$chave",
                    trio.flatMapIndexed { i, a -> trio.drop(i + 1).map { b -> setOf(a, b) } }.toSet(),
                    confrontos.toSet(),
                )
            }
    }

    @Test
    fun `nenhum time joga mais de dois jogos seguidos`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3, random = Random(4))

        times(9).forEach { time ->
            var seguidos = 0
            rodadas.forEach { rodada ->
                val jogou = rodada.matches.any { it.teamA.id == time.id || it.teamB.id == time.id }
                seguidos = if (jogou) seguidos + 1 else 0
                assertTrue("${time.nome} jogou $seguidos seguidos", seguidos <= 2)
            }
        }
    }

    @Test
    fun `o trio que chega cansado entra uma rodada depois`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3, random = Random(4))
        val porFase = rodadas.groupBy { it.fase }

        assertEquals(listOf(3, 3, 3, 4), porFase.keys.sorted().map { porFase.getValue(it).size })
        assertEquals(13, rodadas.size)
        assertEquals((1..13).toList(), rodadas.map { it.numero })
    }

    @Test
    fun `chaveamento de 9 times em 3 quadras cobre o round-robin completo`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3, random = Random(4))

        val confrontos =
            rodadas
                .flatMap { it.matches }
                .map { setOf(it.teamA.id, it.teamB.id) }

        assertEquals(36, confrontos.size)
        assertEquals(36, confrontos.toSet().size)
        assertEquals(4, rodadas.map { it.fase }.toSet().size)
    }

    @Test
    fun `nenhum time joga duas vezes na mesma rodada e as folgas fecham`() {
        val rodadas = RoundRobinScheduler.generate(times(9), quadras = 3, random = Random(4))

        rodadas.forEach { rodada ->
            val jogando = rodada.matches.flatMap { listOf(it.teamA.id, it.teamB.id) }
            assertEquals(jogando.size, jogando.toSet().size)
            assertEquals(9, jogando.size + rodada.folgam.size)
            assertTrue(rodada.matches.size <= 3)
        }
    }

    @Test
    fun `com todos cabendo em quadra ao mesmo tempo ninguem espera`() {
        val rodadas = RoundRobinScheduler.generate(times(6), quadras = 3, random = Random(2))

        assertTrue(rodadas.all { it.matches.size == 3 && it.folgam.isEmpty() })
        assertEquals(5, rodadas.size)
        assertEquals(15, rodadas.sumOf { it.matches.size })
    }

    @Test
    fun `todo confronto acontece e ninguem passa de dois jogos seguidos`() {
        listOf(4 to 1, 5 to 2, 7 to 3, 8 to 3, 9 to 3, 10 to 3, 12 to 4, 15 to 5).forEach { (n, quadras) ->
            val equipes = times(n)
            val rodadas = RoundRobinScheduler.generate(equipes, quadras, random = Random(n.toLong()))
            val caso = "$n times em $quadras quadras"

            val jogados = rodadas.flatMap { it.matches }.map { setOf(it.teamA.id, it.teamB.id) }.toSet()
            val todos =
                equipes
                    .flatMapIndexed { i, a -> equipes.drop(i + 1).map { b -> setOf(a.id, b.id) } }
                    .toSet()
            assertEquals(caso, todos, jogados)
            assertTrue(caso, rodadas.all { it.matches.size <= quadras })

            equipes.forEach { time ->
                var seguidos = 0
                rodadas.forEach { rodada ->
                    val jogou = rodada.matches.any { it.teamA.id == time.id || it.teamB.id == time.id }
                    seguidos = if (jogou) seguidos + 1 else 0
                    assertTrue("$caso: ${time.nome} jogou $seguidos seguidos", seguidos <= 2)
                }
            }
        }
    }

    @Test
    fun `mais quadras do que times nao quebra`() {
        val rodadas = RoundRobinScheduler.generate(times(4), quadras = 5, random = Random(1))

        assertEquals(6, rodadas.sumOf { it.matches.size })
        assertTrue(rodadas.all { it.matches.size <= 2 })
    }
}
