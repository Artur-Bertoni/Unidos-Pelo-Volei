export const DIAS_NO_HISTORICO = 12;

const PLAYER_COLUNAS = `id, nome, skill_level, genero, ativo, profile_id,
         nota_saque, nota_passe, nota_ataque, nota_bloqueio, nota_defesa, nota_atitude, nota_media,
         foto_url, nascimento_dia, nascimento_mes, entrou_em, regime`;

const PLAYER_COLUNAS_P = `p.id, p.nome, p.skill_level, p.genero, p.ativo, p.profile_id,
         p.nota_saque, p.nota_passe, p.nota_ataque, p.nota_bloqueio, p.nota_defesa,
         p.nota_atitude, p.nota_media,
         p.foto_url, p.nascimento_dia, p.nascimento_mes, p.entrou_em, p.regime`;

export const MEUS_GRUPOS_SQL = `
  SELECT g.id, g.nome, g.cidade, g.logo_url, g.ativo, m.papel
  FROM grupo_membros m
  JOIN grupos g ON g.id = m.grupo_id
  WHERE m.profile_id = ?
  ORDER BY g.nome COLLATE NOCASE
`;

export const PLAYERS_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  WHERE grupo_id = ?
  ORDER BY nome COLLATE NOCASE
`;

export const ACTIVE_PLAYERS_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  WHERE grupo_id = ? AND ativo = 1
  ORDER BY nota_media DESC, nome COLLATE NOCASE
`;

export const TEAMS_SQL = `
  SELECT id, nome, cor_hex, sigla, ativo, ordem
  FROM teams
  WHERE grupo_id = ? AND ativo = 1
  ORDER BY ordem, nome COLLATE NOCASE
`;

export const ALL_TEAMS_SQL = `
  SELECT id, nome, cor_hex, sigla, ativo, ordem
  FROM teams
  WHERE grupo_id = ?
  ORDER BY ativo DESC, ordem, nome COLLATE NOCASE
`;

export const ROSTER_SQL = `
  SELECT ${PLAYER_COLUNAS_P}
  FROM team_players tp
  JOIN players p ON p.id = tp.player_id
  WHERE tp.team_id = ?
  ORDER BY p.nota_media DESC, p.nome COLLATE NOCASE
`;

export const ROSTERS_SQL = `
  SELECT
      t.id AS team_id, t.nome AS team_nome, t.cor_hex AS team_cor_hex,
      t.sigla AS team_sigla, t.ativo AS team_ativo, t.ordem AS team_ordem,
      p.id AS player_id, p.nome AS player_nome,
      p.nota_saque AS player_nota_saque, p.nota_passe AS player_nota_passe,
      p.nota_ataque AS player_nota_ataque, p.nota_bloqueio AS player_nota_bloqueio,
      p.nota_defesa AS player_nota_defesa, p.nota_atitude AS player_nota_atitude,
      p.nota_media AS player_nota_media, p.genero AS player_genero,
      p.ativo AS player_ativo
  FROM teams t
  LEFT JOIN team_players tp ON tp.team_id = t.id
  LEFT JOIN players p ON p.id = tp.player_id
  WHERE t.grupo_id = ? AND t.ativo = 1
  ORDER BY t.ordem, t.nome COLLATE NOCASE, p.nota_media DESC, p.nome COLLATE NOCASE
`;

export const FORMATO_SQL = `
  SELECT
      (SELECT COUNT(*) FROM teams WHERE grupo_id = ? AND ativo = 1)     AS times,
      (SELECT COALESCE(MAX(quadra), 0) FROM matches WHERE grupo_id = ?) AS quadras
`;

export const ROUNDS_SQL = `SELECT id, numero, fase FROM rounds WHERE grupo_id = ? ORDER BY numero`;

const MATCH_CARD_SQL = `
  SELECT
      m.id, r.numero, r.fase, m.quadra,
      m.score_a, m.score_b, m.status, m.winner_id,
      ta.id AS a_id, ta.nome AS a_nome, ta.cor_hex AS a_cor_hex,
      ta.sigla AS a_sigla, ta.ativo AS a_ativo, ta.ordem AS a_ordem,
      tb.id AS b_id, tb.nome AS b_nome, tb.cor_hex AS b_cor_hex,
      tb.sigla AS b_sigla, tb.ativo AS b_ativo, tb.ordem AS b_ordem
  FROM matches m
  JOIN rounds r ON r.id = m.round_id
  JOIN teams ta ON ta.id = m.team_a_id
  JOIN teams tb ON tb.id = m.team_b_id
`;

export const MATCHES_SQL = `${MATCH_CARD_SQL} WHERE m.grupo_id = ? ORDER BY r.numero, m.quadra`;

export const TEAM_HISTORY_SQL = `${MATCH_CARD_SQL} WHERE m.team_a_id = ? OR m.team_b_id = ? ORDER BY r.numero, m.quadra`;

export const MATCH_SQL = `${MATCH_CARD_SQL} WHERE m.id = ?`;

export const PROFILE_SQL = `
  SELECT p.id, p.email, p.nome, COALESCE(m.papel, 'atleta') AS papel
  FROM profiles p
  LEFT JOIN grupo_membros m ON m.profile_id = p.id AND m.grupo_id = ?
  WHERE p.id = ?
`;

export const MEU_JOGADOR_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  WHERE grupo_id = ? AND profile_id = ?
`;

const PEDIDO_COLUNAS = `id, profile_id, player_id, profile_nome, status, criado_em`;

export const MEU_PEDIDO_SQL = `
  SELECT ${PEDIDO_COLUNAS}
  FROM vinculo_pedidos
  WHERE grupo_id = ? AND profile_id = ?
  ORDER BY criado_em DESC
  LIMIT 1
`;

export const PEDIDOS_PENDENTES_SQL = `
  SELECT ${PEDIDO_COLUNAS}
  FROM vinculo_pedidos
  WHERE grupo_id = ? AND status = 'pendente'
  ORDER BY criado_em
`;

export const MEU_CONTATO_SQL = `
  SELECT id, player_id, telefone, contato_emergencia, nascimento_ano
  FROM player_contatos
  WHERE player_id = ?
`;

export const CONFIG_GRUPO_SQL = `
  SELECT id, jogo_hora, jogo_local FROM config_grupo WHERE grupo_id = ? LIMIT 1
`;

export const PRESENCAS_SQL = `
  SELECT id, player_id, data, status, origem
  FROM presencas
  WHERE grupo_id = ? AND data = ?
`;

export const POSTS_SQL = `
  WITH meus AS (SELECT * FROM posts WHERE grupo_id = ?)
  SELECT
      p.id, p.autor_nome, p.titulo, p.corpo, p.imagem_url, p.emoji, p.fixado, p.publicado_em,
      (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id) AS reacoes,
      (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id AND r.profile_id = ?) AS reagi
  FROM meus p
  ORDER BY p.fixado DESC, p.publicado_em DESC
`;

export const EVENTOS_SQL = `
  SELECT id, titulo, descricao, tipo, inicio, local
  FROM eventos
  WHERE grupo_id = ?
  ORDER BY inicio
`;

export const PAGINAS_SQL = `
  SELECT id, slug, categoria, titulo, corpo, ordem
  FROM paginas
  WHERE grupo_id = ?
  ORDER BY categoria, ordem, titulo COLLATE NOCASE
`;

export const CONFIG_FINANCEIRO_SQL = `
  SELECT id, pix_chave, pix_tipo, pix_nome, pix_cidade, mensalidade_centavos, diaria_centavos
  FROM config_financeiro
  WHERE grupo_id = ?
  LIMIT 1
`;

export const COBRANCAS_SQL = `
  SELECT id, titulo, tipo, valor_centavos, competencia, vence_em
  FROM cobrancas
  WHERE grupo_id = ?
  ORDER BY COALESCE(competencia, criado_em) DESC, criado_em DESC
`;

export const MEUS_PAGAMENTOS_SQL = `
  SELECT id, cobranca_id, player_id, valor_centavos, status, pago_em, observacao
  FROM pagamentos
  WHERE grupo_id = ?
  ORDER BY criado_em DESC
`;

export const EVOLUCAO_SQL = `
  SELECT player_id, total_avaliacoes, saque_media, passe_media, ataque_media,
         bloqueio_media, defesa_media, atitude_media
  FROM player_evolucao
  WHERE grupo_id = ?
  LIMIT 1
`;

export const HISTORICO_DA_NOTA_SQL = `
  SELECT h.id, h.origem, h.avaliadores, h.nota_saque, h.nota_passe, h.nota_ataque,
         h.nota_bloqueio, h.nota_defesa, h.nota_atitude, h.media, h.registrado_em
  FROM player_nota_historico h
  WHERE h.grupo_id = ? AND h.player_id = ?
  ORDER BY h.registrado_em, h.id
`;

export const DICAS_SQL = `
  SELECT id, atributo, faixa_max, titulo, texto
  FROM dicas
  ORDER BY atributo, faixa_max, ordem
`;

export const AVALIACOES_PENDENTES_SQL = `
  SELECT
      meu.day_id       AS day_id,
      colega.player_id AS avaliado_player_id,
      p.nome           AS avaliado_nome
  FROM player_day_stats meu
  JOIN player_day_stats colega
      ON colega.day_id = meu.day_id
      AND colega.team_id = meu.team_id
      AND colega.player_id <> meu.player_id
  JOIN players p ON p.id = colega.player_id
  JOIN (
      SELECT id FROM game_days WHERE grupo_id = ? ORDER BY encerrado_em DESC LIMIT 4
  ) d ON d.id = meu.day_id
  WHERE meu.player_id = ?
      AND meu.team_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM avaliacao_registros r
          WHERE r.day_id = meu.day_id
              AND r.avaliador_player_id = ?
              AND r.avaliado_player_id = colega.player_id
      )
  ORDER BY meu.day_id DESC, p.nome COLLATE NOCASE
`;

export const STANDINGS_SQL = `
  WITH lados AS (
      SELECT
          m.team_a_id AS team_id,
          m.score_a   AS pontos_pro,
          m.score_b   AS pontos_contra,
          CASE WHEN m.winner_id = m.team_a_id THEN 1 ELSE 0 END AS venceu
      FROM matches m
      WHERE m.grupo_id = ? AND m.status = 'finalizado'
      UNION ALL
      SELECT
          m.team_b_id,
          m.score_b,
          m.score_a,
          CASE WHEN m.winner_id = m.team_b_id THEN 1 ELSE 0 END
      FROM matches m
      WHERE m.grupo_id = ? AND m.status = 'finalizado'
  )
  SELECT
      t.id      AS team_id,
      t.nome    AS nome,
      t.sigla   AS sigla,
      t.cor_hex AS cor_hex,
      COUNT(l.team_id)                                    AS jogos,
      COALESCE(SUM(l.venceu), 0)                          AS vitorias,
      COALESCE(SUM(1 - l.venceu), 0)                      AS derrotas,
      COALESCE(SUM(l.pontos_pro - l.pontos_contra), 0)    AS saldo_pontos,
      COALESCE(SUM(l.pontos_pro), 0)                      AS pontos_pro,
      COALESCE(SUM(l.pontos_contra), 0)                   AS pontos_contra
  FROM teams t
  LEFT JOIN lados l ON l.team_id = t.id
  WHERE t.grupo_id = ? AND t.ativo = 1
  GROUP BY t.id, t.nome, t.sigla, t.cor_hex, t.ordem
  ORDER BY vitorias DESC, saldo_pontos DESC, pontos_pro DESC, t.ordem
`;

export const ELENCOS_SQL = `
  SELECT s.day_id, s.team_id, s.player_id
  FROM player_day_stats s
  JOIN (
      SELECT id, encerrado_em
      FROM game_days
      WHERE grupo_id = ?
      ORDER BY encerrado_em DESC
      LIMIT ${DIAS_NO_HISTORICO}
  ) d ON d.id = s.day_id
  WHERE s.grupo_id = ? AND s.team_id IS NOT NULL
  ORDER BY d.encerrado_em DESC, s.day_id, s.team_id
`;

export const PERFORMANCE_SQL = `
  SELECT
      p.id                              AS player_id,
      COUNT(s.id)                       AS dias,
      COALESCE(SUM(s.jogos), 0)         AS jogos,
      COALESCE(SUM(s.vitorias), 0)      AS vitorias,
      COALESCE(SUM(s.derrotas), 0)      AS derrotas,
      COALESCE(SUM(s.pontos_pro), 0)    AS pontos_pro,
      COALESCE(SUM(s.pontos_contra), 0) AS pontos_contra
  FROM players p
  LEFT JOIN player_day_stats s ON s.player_id = p.id
  WHERE p.grupo_id = ?
  GROUP BY p.id
`;
