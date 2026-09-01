package com.unidospelovolei.domain.model

enum class Genero(
    val value: String,
    val rotulo: String,
) {
    MASCULINO("masculino", "Masculino"),
    FEMININO("feminino", "Feminino"),
    ;

    companion object {
        fun from(value: String?): Genero = entries.firstOrNull { it.value == value } ?: MASCULINO
    }
}

data class Player(
    val id: String,
    val nome: String,
    val skillLevel: Int,
    val genero: Genero,
    val ativo: Boolean,
)

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

data class Round(
    val id: String,
    val numero: Int,
    val fase: Int,
)

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

data class RoundSchedule(
    val round: Round,
    val matches: List<MatchCard>,
    val folgam: List<Team>,
)

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

data class TeamRoster(
    val team: Team,
    val players: List<Player>,
) {
    val forcaTotal: Int get() = players.sumOf { it.skillLevel }

    val homens: Int get() = players.count { it.genero == Genero.MASCULINO }

    val mulheres: Int get() = players.count { it.genero == Genero.FEMININO }
}

data class PlayerPerformance(
    val playerId: String,
    val dias: Int,
    val jogos: Int,
    val vitorias: Int,
    val derrotas: Int,
    val pontosPro: Int,
    val pontosContra: Int,
) {
    val saldoPontos: Int get() = pontosPro - pontosContra
}

data class ResumoDoDia(
    val partidas: Int,
    val atletas: Int,
    val presencas: Int,
)

data class UserProfile(
    val id: String,
    val email: String?,
    val nome: String?,
    val isAdmin: Boolean,
)
