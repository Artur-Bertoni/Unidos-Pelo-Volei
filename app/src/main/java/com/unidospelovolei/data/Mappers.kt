package com.unidospelovolei.data

import com.powersync.db.SqlCursor
import com.powersync.db.getBooleanOptional
import com.powersync.db.getLongOptional
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Standing
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.UserProfile

internal fun SqlCursor.int(
    name: String,
    padrao: Int = 0,
): Int = getLongOptional(name)?.toInt() ?: padrao

internal fun SqlCursor.bool(
    name: String,
    padrao: Boolean = false,
): Boolean = getBooleanOptional(name) ?: padrao

internal fun SqlCursor.toPlayer(): Player =
    Player(
        id = getString("id"),
        nome = getStringOptional("nome").orEmpty(),
        skillLevel = int("skill_level", 3),
        genero = Genero.from(getStringOptional("genero")),
        ativo = bool("ativo", true),
    )

internal fun SqlCursor.toTeam(prefixo: String = ""): Team =
    Team(
        id = getString(prefixo + "id"),
        nome = getStringOptional(prefixo + "nome").orEmpty(),
        corHex = getStringOptional(prefixo + "cor_hex") ?: "#6B7280",
        sigla = getStringOptional(prefixo + "sigla").orEmpty(),
        ativo = bool(prefixo + "ativo", true),
        ordem = int(prefixo + "ordem"),
    )

internal fun SqlCursor.toMatchCard(): MatchCard =
    MatchCard(
        id = getString("id"),
        roundNumero = int("numero"),
        fase = int("fase", 1),
        quadra = int("quadra", 1),
        scoreA = int("score_a"),
        scoreB = int("score_b"),
        status = MatchStatus.from(getStringOptional("status")),
        winnerId = getStringOptional("winner_id"),
        teamA = toTeam("a_"),
        teamB = toTeam("b_"),
    )

internal fun SqlCursor.toStanding(): Standing =
    Standing(
        teamId = getString("team_id"),
        nome = getStringOptional("nome").orEmpty(),
        sigla = getStringOptional("sigla").orEmpty(),
        corHex = getStringOptional("cor_hex") ?: "#6B7280",
        jogos = int("jogos"),
        vitorias = int("vitorias"),
        derrotas = int("derrotas"),
        saldoPontos = int("saldo_pontos"),
        pontosPro = int("pontos_pro"),
    )

internal fun SqlCursor.toUserProfile(): UserProfile =
    UserProfile(
        id = getString("id"),
        email = getStringOptional("email"),
        nome = getStringOptional("nome"),
        isAdmin = bool("is_admin"),
    )
