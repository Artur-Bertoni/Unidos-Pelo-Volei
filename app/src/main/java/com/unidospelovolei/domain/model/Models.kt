package com.unidospelovolei.domain.model

/** Jogador do grupo. `skillLevel` vai de 1 a 5 e alimenta o snake draft. */
data class Player(
    val id: String,
    val nome: String,
    val skillLevel: Int,
    val ativo: Boolean,
)

/** Time colorido. A `sigla` de 2 letras e o que aparece dentro do circulo. */
data class Team(
    val id: String,
    val nome: String,
    val corHex: String,
    val sigla: String,
    val ativo: Boolean,
    val ordem: Int,
)

enum class MatchStatus(val value: String) {
    AGENDADO("agendado"),
    FINALIZADO("finalizado"),
    ;

    companion object {
        fun from(value: String?): MatchStatus = entries.firstOrNull { it.value == value } ?: AGENDADO
    }
}

/** Rodada do chaveamento. Rodadas consecutivas sao agrupadas em fases. */
data class Round(
    val id: String,
    val numero: Int,
    val fase: Int,
)

/** Uma partida em uma quadra de uma rodada. */
data class Match(
    val id: String,
    val roundId: String,
    val quadra: Int,
    val teamAId: String,
    val teamBId: String,
    val scoreA: Int,
    val scoreB: Int,
    val status: MatchStatus,
    val winnerId: String?,
)

/** Partida ja resolvida com os dados dos dois times, pronta para a tela. */
data class MatchCard(
    val id: String,
    val roundNumero: Int,
    val fase: Int,
    val quadra: Int,
    val scoreA: Int,
    val scoreB: Int,
    val status: MatchStatus,
    val winnerId: String?,
    val teamA: Team,
    val teamB: Team,
)

/** Uma rodada com suas partidas e os times que folgam nela. */
data class RoundSchedule(
    val round: Round,
    val matches: List<MatchCard>,
    val folgam: List<Team>,
)

/** Linha da classificacao geral, lida da view `standings`. */
data class Standing(
    val teamId: String,
    val nome: String,
    val sigla: String,
    val corHex: String,
    val jogos: Int,
    val vitorias: Int,
    val derrotas: Int,
    val saldoPontos: Int,
    val pontosPro: Int,
)

/** Time com o elenco atual e a soma das habilidades (a "forca" do time). */
data class TeamRoster(
    val team: Team,
    val players: List<Player>,
) {
    val forcaTotal: Int get() = players.sumOf { it.skillLevel }
}

/** Perfil do usuario logado. So admin escreve. */
data class UserProfile(
    val id: String,
    val email: String?,
    val nome: String?,
    val isAdmin: Boolean,
)
