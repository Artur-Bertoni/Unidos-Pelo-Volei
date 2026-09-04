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
    val profileId: String? = null,
    val fotoUrl: String? = null,
    val nascimentoDia: Int? = null,
    val nascimentoMes: Int? = null,
    val entrouEm: String? = null,
    val regime: Regime = Regime.MENSALISTA,
) {
    val vinculado: Boolean get() = profileId != null

    val aniversario: String?
        get() {
            val dia = nascimentoDia ?: return null
            val mes = nascimentoMes ?: return null
            return "%02d/%02d".format(dia, mes)
        }
}

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

enum class Papel(
    val value: String,
    val rotulo: String,
) {
    DIRETORIA("diretoria", "Diretoria"),
    ATLETA("atleta", "Atleta"),
    ;

    companion object {
        fun from(value: String?): Papel = entries.firstOrNull { it.value == value } ?: ATLETA
    }
}

data class UserProfile(
    val id: String,
    val email: String?,
    val nome: String?,
    val papel: Papel,
) {
    val isAdmin: Boolean get() = papel == Papel.DIRETORIA
}

enum class StatusVinculo(
    val value: String,
    val rotulo: String,
) {
    PENDENTE("pendente", "Aguardando"),
    APROVADO("aprovado", "Aprovado"),
    RECUSADO("recusado", "Recusado"),
    ;

    companion object {
        fun from(value: String?): StatusVinculo = entries.firstOrNull { it.value == value } ?: PENDENTE
    }
}

data class VinculoPedido(
    val id: String,
    val profileId: String,
    val playerId: String,
    val profileNome: String?,
    val status: StatusVinculo,
    val criadoEm: String?,
)

data class PlayerContato(
    val id: String,
    val playerId: String,
    val telefone: String?,
    val contatoEmergencia: String?,
    val nascimentoAno: Int?,
)

enum class Regime(
    val value: String,
    val rotulo: String,
) {
    MENSALISTA("mensalista", "Mensalista"),
    DIARISTA("diarista", "Diarista"),
    ISENTO("isento", "Isento"),
    ;

    companion object {
        val escolhiveisPeloAtleta: List<Regime> = listOf(MENSALISTA, DIARISTA)

        fun from(value: String?): Regime = entries.firstOrNull { it.value == value } ?: MENSALISTA
    }
}

enum class StatusPresenca(
    val value: String,
    val rotulo: String,
) {
    VOU("vou", "Vou"),
    TALVEZ("talvez", "Talvez"),
    NAO_VOU("nao_vou", "Não vou"),
    ;

    companion object {
        fun from(value: String?): StatusPresenca? = entries.firstOrNull { it.value == value }
    }
}

data class Presenca(
    val id: String,
    val playerId: String,
    val data: String,
    val status: StatusPresenca,
    val origem: String,
)

data class ResumoDaChamada(
    val vou: Int,
    val talvez: Int,
    val naoVou: Int,
    val semResposta: Int,
) {
    val responderam: Int get() = vou + talvez + naoVou
}

data class Aviso(
    val id: String,
    val tipo: String,
    val titulo: String,
    val corpo: String?,
    val criadoEm: String?,
)

data class ConfigGrupo(
    val id: String,
    val jogoHora: String,
    val jogoLocal: String?,
)

const val EMOJI_PADRAO: String = "👏"

val EMOJIS_DE_REACAO: List<String> =
    listOf(
        "👏",
        "🔥",
        "❤️",
        "😂",
        "👍",
        "🏐",
        "🎉",
        "😮",
    )

data class Post(
    val id: String,
    val autorNome: String?,
    val titulo: String,
    val corpo: String,
    val fixado: Boolean,
    val publicadoEm: String?,
    val imagemUrl: String? = null,
    val emoji: String = EMOJI_PADRAO,
    val reacoes: Int = 0,
    val reagi: Boolean = false,
)

enum class TipoEvento(
    val value: String,
    val rotulo: String,
) {
    JOGO("jogo", "Jogo"),
    CONFRATERNIZACAO("confraternizacao", "Confraternização"),
    CAMPEONATO("campeonato", "Campeonato"),
    ;

    companion object {
        fun from(value: String?): TipoEvento = entries.firstOrNull { it.value == value } ?: JOGO
    }
}

data class Evento(
    val id: String,
    val titulo: String,
    val descricao: String?,
    val tipo: TipoEvento,
    val inicio: String,
    val local: String?,
)

enum class TipoMarco(
    val rotulo: String,
) {
    ANIVERSARIO("Aniversário"),
    TEMPO_DE_CASA("Anos de Unidos"),
}

data class Marco(
    val playerId: String,
    val nome: String,
    val tipo: TipoMarco,
    val dia: Int,
    val mes: Int,
    val anos: Int?,
)

enum class CategoriaPagina(
    val value: String,
    val rotulo: String,
) {
    VOLEI("volei", "Regras do vôlei"),
    GRUPO("grupo", "Regras do grupo"),
    CAMPEONATO("campeonato", "Campeonatos"),
    ;

    companion object {
        fun from(value: String?): CategoriaPagina = entries.firstOrNull { it.value == value } ?: GRUPO
    }
}

data class Pagina(
    val id: String,
    val slug: String,
    val categoria: CategoriaPagina,
    val titulo: String,
    val corpo: String,
    val ordem: Int,
)

data class ConfigFinanceiro(
    val id: String,
    val pixChave: String?,
    val pixNome: String?,
    val pixCidade: String?,
    val mensalidadeCentavos: Int,
    val diariaCentavos: Int,
) {
    val pixConfigurado: Boolean get() = !pixChave.isNullOrBlank()
}

enum class TipoCobranca(
    val value: String,
    val rotulo: String,
) {
    MENSALIDADE("mensalidade", "Mensalidade"),
    DIARIA("diaria", "Diária"),
    AVULSA("avulsa", "Avulsa"),
    ;

    companion object {
        fun from(value: String?): TipoCobranca = entries.firstOrNull { it.value == value } ?: AVULSA
    }
}

data class Cobranca(
    val id: String,
    val titulo: String,
    val tipo: TipoCobranca,
    val valorCentavos: Int,
    val competencia: String?,
    val venceEm: String?,
)

enum class StatusPagamento(
    val value: String,
    val rotulo: String,
) {
    PENDENTE("pendente", "Em aberto"),
    PAGO("pago", "Pago"),
    ISENTO("isento", "Isento"),
    ;

    companion object {
        fun from(value: String?): StatusPagamento = entries.firstOrNull { it.value == value } ?: PENDENTE
    }
}

data class Pagamento(
    val id: String,
    val cobrancaId: String,
    val playerId: String,
    val valorCentavos: Int,
    val status: StatusPagamento,
    val pagoEm: String?,
    val observacao: String?,
)

data class ItemDoExtrato(
    val pagamento: Pagamento,
    val cobranca: Cobranca?,
) {
    val titulo: String get() = cobranca?.titulo ?: "Cobrança"
}

enum class Fundamento(
    val value: String,
    val rotulo: String,
) {
    SAQUE("saque", "Saque"),
    PASSE("passe", "Passe"),
    ATAQUE("ataque", "Ataque"),
    BLOQUEIO("bloqueio", "Bloqueio"),
    DEFESA("defesa", "Defesa"),
    ATITUDE("atitude", "Atitude"),
    ;

    companion object {
        fun from(value: String?): Fundamento? = entries.firstOrNull { it.value == value }
    }
}

data class NotasDaAvaliacao(
    val saque: Int = 3,
    val passe: Int = 3,
    val ataque: Int = 3,
    val bloqueio: Int = 3,
    val defesa: Int = 3,
    val atitude: Int = 3,
) {
    fun de(fundamento: Fundamento): Int =
        when (fundamento) {
            Fundamento.SAQUE -> saque
            Fundamento.PASSE -> passe
            Fundamento.ATAQUE -> ataque
            Fundamento.BLOQUEIO -> bloqueio
            Fundamento.DEFESA -> defesa
            Fundamento.ATITUDE -> atitude
        }

    fun com(
        fundamento: Fundamento,
        nota: Int,
    ): NotasDaAvaliacao =
        when (fundamento) {
            Fundamento.SAQUE -> copy(saque = nota)
            Fundamento.PASSE -> copy(passe = nota)
            Fundamento.ATAQUE -> copy(ataque = nota)
            Fundamento.BLOQUEIO -> copy(bloqueio = nota)
            Fundamento.DEFESA -> copy(defesa = nota)
            Fundamento.ATITUDE -> copy(atitude = nota)
        }
}

data class AvaliacaoPendente(
    val dayId: String,
    val avaliadoPlayerId: String,
    val avaliadoNome: String,
)

data class Evolucao(
    val playerId: String,
    val totalAvaliacoes: Int,
    val medias: Map<Fundamento, Double>,
) {
    val liberado: Boolean get() = medias.isNotEmpty()

    val maisFraco: Fundamento? get() = medias.minByOrNull { it.value }?.key
}

data class Dica(
    val id: String,
    val fundamento: Fundamento,
    val faixaMax: Double,
    val titulo: String,
    val texto: String,
)

const val MINIMO_DE_AVALIACOES: Int = 5
