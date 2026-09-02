export const DIAS_NO_HISTORICO = 12;

const PLAYER_COLUNAS = `id, nome, skill_level, genero, ativo, profile_id, posicao,
         foto_url, nascimento_dia, nascimento_mes, entrou_em`;

const PLAYER_COLUNAS_P = `p.id, p.nome, p.skill_level, p.genero, p.ativo, p.profile_id,
         p.posicao, p.foto_url, p.nascimento_dia, p.nascimento_mes, p.entrou_em`;

export const PLAYERS_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  ORDER BY nome COLLATE NOCASE
`;

export const ACTIVE_PLAYERS_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  WHERE ativo = 1
  ORDER BY skill_level DESC, nome COLLATE NOCASE
`;

export const TEAMS_SQL = `
  SELECT id, nome, cor_hex, sigla, ativo, ordem
  FROM teams
  WHERE ativo = 1
  ORDER BY ordem, nome COLLATE NOCASE
`;

export const ALL_TEAMS_SQL = `
  SELECT id, nome, cor_hex, sigla, ativo, ordem
  FROM teams
  ORDER BY ativo DESC, ordem, nome COLLATE NOCASE
`;

export const ROSTER_SQL = `
  SELECT ${PLAYER_COLUNAS_P}
  FROM team_players tp
  JOIN players p ON p.id = tp.player_id
  WHERE tp.team_id = ?
  ORDER BY p.skill_level DESC, p.nome COLLATE NOCASE
`;

export const ROSTERS_SQL = `
  SELECT
      t.id AS team_id, t.nome AS team_nome, t.cor_hex AS team_cor_hex,
      t.sigla AS team_sigla, t.ativo AS team_ativo, t.ordem AS team_ordem,
      p.id AS player_id, p.nome AS player_nome,
      p.skill_level AS player_skill_level, p.genero AS player_genero,
      p.ativo AS player_ativo
  FROM teams t
  LEFT JOIN team_players tp ON tp.team_id = t.id
  LEFT JOIN players p ON p.id = tp.player_id
  WHERE t.ativo = 1
  ORDER BY t.ordem, t.nome COLLATE NOCASE, p.skill_level DESC, p.nome COLLATE NOCASE
`;

export const FORMATO_SQL = `
  SELECT
      (SELECT COUNT(*) FROM teams WHERE ativo = 1)  AS times,
      (SELECT COALESCE(MAX(quadra), 0) FROM matches) AS quadras
`;

export const ROUNDS_SQL = `SELECT id, numero, fase FROM rounds ORDER BY numero`;

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

export const MATCHES_SQL = `${MATCH_CARD_SQL} ORDER BY r.numero, m.quadra`;

export const TEAM_HISTORY_SQL = `${MATCH_CARD_SQL} WHERE m.team_a_id = ? OR m.team_b_id = ? ORDER BY r.numero, m.quadra`;

export const MATCH_SQL = `${MATCH_CARD_SQL} WHERE m.id = ?`;

export const PROFILE_SQL = `SELECT id, email, nome, papel, is_admin FROM profiles WHERE id = ?`;

export const MEU_JOGADOR_SQL = `
  SELECT ${PLAYER_COLUNAS}
  FROM players
  WHERE profile_id = ?
`;

const PEDIDO_COLUNAS = `id, profile_id, player_id, profile_nome, status, criado_em`;

export const MEU_PEDIDO_SQL = `
  SELECT ${PEDIDO_COLUNAS}
  FROM vinculo_pedidos
  WHERE profile_id = ?
  ORDER BY criado_em DESC
  LIMIT 1
`;

export const PEDIDOS_PENDENTES_SQL = `
  SELECT ${PEDIDO_COLUNAS}
  FROM vinculo_pedidos
  WHERE status = 'pendente'
  ORDER BY criado_em
`;

export const MEU_CONTATO_SQL = `
  SELECT id, player_id, telefone, contato_emergencia, nascimento_ano
  FROM player_contatos
  WHERE player_id = ?
`;

export const CONFIG_GRUPO_SQL = `SELECT id, jogo_hora, jogo_local FROM config_grupo LIMIT 1`;

export const PRESENCAS_SQL = `
  SELECT id, player_id, data, status, origem
  FROM presencas
  WHERE data = ?
`;

export const POSTS_SQL = `
  SELECT
      p.id, p.autor_nome, p.titulo, p.corpo, p.fixado, p.publicado_em,
      (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id) AS reacoes,
      (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id AND r.profile_id = ?) AS reagi
  FROM posts p
  ORDER BY p.fixado DESC, p.publicado_em DESC
`;

export const EVENTOS_SQL = `
  SELECT id, titulo, descricao, tipo, inicio, local
  FROM eventos
  ORDER BY inicio
`;

export const PAGINAS_SQL = `
  SELECT id, slug, categoria, titulo, corpo, ordem
  FROM paginas
  ORDER BY categoria, ordem, titulo COLLATE NOCASE
`;

export const CONFIG_FINANCEIRO_SQL = `
  SELECT id, pix_chave, pix_nome, pix_cidade, mensalidade_centavos, diaria_centavos
  FROM config_financeiro
  LIMIT 1
`;

export const COBRANCAS_SQL = `
  SELECT id, titulo, tipo, valor_centavos, competencia, vence_em
  FROM cobrancas
  ORDER BY COALESCE(competencia, criado_em) DESC, criado_em DESC
`;

export const MEUS_PAGAMENTOS_SQL = `
  SELECT id, cobranca_id, player_id, valor_centavos, status, pago_em, observacao
  FROM pagamentos
  ORDER BY criado_em DESC
`;

export const EVOLUCAO_SQL = `
  SELECT player_id, total_avaliacoes, saque_media, passe_media, ataque_media,
         bloqueio_media, defesa_media, atitude_media
  FROM player_evolucao
  LIMIT 1
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
      SELECT id FROM game_days ORDER BY encerrado_em DESC LIMIT 4
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
      WHERE m.status = 'finalizado'
      UNION ALL
      SELECT
          m.team_b_id,
          m.score_b,
          m.score_a,
          CASE WHEN m.winner_id = m.team_b_id THEN 1 ELSE 0 END
      FROM matches m
      WHERE m.status = 'finalizado'
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
  WHERE t.ativo = 1
  GROUP BY t.id, t.nome, t.sigla, t.cor_hex, t.ordem
  ORDER BY vitorias DESC, saldo_pontos DESC, pontos_pro DESC, t.ordem
`;

export const ELENCOS_SQL = `
  SELECT s.day_id, s.team_id, s.player_id
  FROM player_day_stats s
  JOIN (
      SELECT id, encerrado_em
      FROM game_days
      ORDER BY encerrado_em DESC
      LIMIT ${DIAS_NO_HISTORICO}
  ) d ON d.id = s.day_id
  WHERE s.team_id IS NOT NULL
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
  GROUP BY p.id
`;
