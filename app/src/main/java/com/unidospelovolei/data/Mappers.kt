package com.unidospelovolei.data

import com.powersync.db.SqlCursor
import com.powersync.db.getBooleanOptional
import com.powersync.db.getDoubleOptional
import com.powersync.db.getLongOptional
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.unidospelovolei.domain.model.Aviso
import com.unidospelovolei.domain.model.CategoriaPagina
import com.unidospelovolei.domain.model.Cobranca
import com.unidospelovolei.domain.model.ConfigFinanceiro
import com.unidospelovolei.domain.model.ConfigGrupo
import com.unidospelovolei.domain.model.Dica
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Evolucao
import com.unidospelovolei.domain.model.Fundamento
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.Pagamento
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.Papel
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerContato
import com.unidospelovolei.domain.model.Posicao
import com.unidospelovolei.domain.model.Post
import com.unidospelovolei.domain.model.Presenca
import com.unidospelovolei.domain.model.Regime
import com.unidospelovolei.domain.model.Standing
import com.unidospelovolei.domain.model.StatusPagamento
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.domain.model.StatusVinculo
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TipoCobranca
import com.unidospelovolei.domain.model.TipoEvento
import com.unidospelovolei.domain.model.UserProfile
import com.unidospelovolei.domain.model.VinculoPedido

internal fun SqlCursor.int(
    name: String,
    padrao: Int = 0,
): Int = getLongOptional(name)?.toInt() ?: padrao

internal fun SqlCursor.bool(
    name: String,
    padrao: Boolean = false,
): Boolean = getBooleanOptional(name) ?: padrao

internal fun SqlCursor.intOrNull(name: String): Int? = getLongOptional(name)?.toInt()

internal fun SqlCursor.toPlayer(): Player =
    Player(
        id = getString("id"),
        nome = getStringOptional("nome").orEmpty(),
        skillLevel = int("skill_level", 3),
        genero = Genero.from(getStringOptional("genero")),
        ativo = bool("ativo", true),
        profileId = getStringOptional("profile_id"),
        posicao = Posicao.from(getStringOptional("posicao")),
        fotoUrl = getStringOptional("foto_url"),
        nascimentoDia = intOrNull("nascimento_dia"),
        nascimentoMes = intOrNull("nascimento_mes"),
        entrouEm = getStringOptional("entrou_em"),
        regime = Regime.from(getStringOptional("regime")),
    )

internal fun SqlCursor.realOrNull(name: String): Double? = getDoubleOptional(name)

internal fun SqlCursor.toPresenca(): Presenca =
    Presenca(
        id = getString("id"),
        playerId = getStringOptional("player_id").orEmpty(),
        data = getStringOptional("data").orEmpty(),
        status = StatusPresenca.from(getStringOptional("status")) ?: StatusPresenca.VOU,
        origem = getStringOptional("origem") ?: "atleta",
    )

internal fun SqlCursor.toAviso(): Aviso =
    Aviso(
        id = getString("id"),
        tipo = getStringOptional("tipo") ?: "lembrete",
        titulo = getStringOptional("titulo").orEmpty(),
        corpo = getStringOptional("corpo"),
        criadoEm = getStringOptional("criado_em"),
    )

internal fun SqlCursor.toConfigGrupo(): ConfigGrupo =
    ConfigGrupo(
        id = getString("id"),
        jogoHora = getStringOptional("jogo_hora") ?: "09:00",
        jogoLocal = getStringOptional("jogo_local"),
    )

internal fun SqlCursor.toPost(): Post =
    Post(
        id = getString("id"),
        autorNome = getStringOptional("autor_nome"),
        titulo = getStringOptional("titulo").orEmpty(),
        corpo = getStringOptional("corpo").orEmpty(),
        fixado = bool("fixado"),
        publicadoEm = getStringOptional("publicado_em"),
        reacoes = int("reacoes"),
        reagi = bool("reagi"),
    )

internal fun SqlCursor.toEvento(): Evento =
    Evento(
        id = getString("id"),
        titulo = getStringOptional("titulo").orEmpty(),
        descricao = getStringOptional("descricao"),
        tipo = TipoEvento.from(getStringOptional("tipo")),
        inicio = getStringOptional("inicio").orEmpty(),
        local = getStringOptional("local"),
    )

internal fun SqlCursor.toPagina(): Pagina =
    Pagina(
        id = getString("id"),
        slug = getStringOptional("slug").orEmpty(),
        categoria = CategoriaPagina.from(getStringOptional("categoria")),
        titulo = getStringOptional("titulo").orEmpty(),
        corpo = getStringOptional("corpo").orEmpty(),
        ordem = int("ordem"),
    )

internal fun SqlCursor.toConfigFinanceiro(): ConfigFinanceiro =
    ConfigFinanceiro(
        id = getString("id"),
        pixChave = getStringOptional("pix_chave"),
        pixNome = getStringOptional("pix_nome"),
        pixCidade = getStringOptional("pix_cidade"),
        mensalidadeCentavos = int("mensalidade_centavos"),
        diariaCentavos = int("diaria_centavos"),
    )

internal fun SqlCursor.toCobranca(): Cobranca =
    Cobranca(
        id = getString("id"),
        titulo = getStringOptional("titulo").orEmpty(),
        tipo = TipoCobranca.from(getStringOptional("tipo")),
        valorCentavos = int("valor_centavos"),
        competencia = getStringOptional("competencia"),
        venceEm = getStringOptional("vence_em"),
    )

internal fun SqlCursor.toPagamento(): Pagamento =
    Pagamento(
        id = getString("id"),
        cobrancaId = getStringOptional("cobranca_id").orEmpty(),
        playerId = getStringOptional("player_id").orEmpty(),
        valorCentavos = int("valor_centavos"),
        status = StatusPagamento.from(getStringOptional("status")),
        pagoEm = getStringOptional("pago_em"),
        observacao = getStringOptional("observacao"),
    )

internal fun SqlCursor.toEvolucao(): Evolucao {
    val medias =
        Fundamento.entries.mapNotNull { fundamento ->
            realOrNull("${fundamento.value}_media")?.let { fundamento to it }
        }
    return Evolucao(
        playerId = getStringOptional("player_id").orEmpty(),
        totalAvaliacoes = int("total_avaliacoes"),
        medias = medias.toMap(),
    )
}

internal fun SqlCursor.toDica(): Dica =
    Dica(
        id = getString("id"),
        fundamento = Fundamento.from(getStringOptional("atributo")) ?: Fundamento.ATITUDE,
        faixaMax = realOrNull("faixa_max") ?: 5.0,
        titulo = getStringOptional("titulo").orEmpty(),
        texto = getStringOptional("texto").orEmpty(),
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
        papel =
            getStringOptional("papel")
                ?.let(Papel::from)
                ?: if (bool("is_admin")) Papel.DIRETORIA else Papel.ATLETA,
    )

internal fun SqlCursor.toVinculoPedido(): VinculoPedido =
    VinculoPedido(
        id = getString("id"),
        profileId = getStringOptional("profile_id").orEmpty(),
        playerId = getStringOptional("player_id").orEmpty(),
        profileNome = getStringOptional("profile_nome"),
        status = StatusVinculo.from(getStringOptional("status")),
        criadoEm = getStringOptional("criado_em"),
    )

internal fun SqlCursor.toPlayerContato(): PlayerContato =
    PlayerContato(
        id = getString("id"),
        playerId = getStringOptional("player_id").orEmpty(),
        telefone = getStringOptional("telefone"),
        contatoEmergencia = getStringOptional("contato_emergencia"),
        nascimentoAno = intOrNull("nascimento_ano"),
    )
