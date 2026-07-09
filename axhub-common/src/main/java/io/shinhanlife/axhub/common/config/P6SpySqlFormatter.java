package io.shinhanlife.axhub.common.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * @package io.shinhanlife.axhub.common.config
 * @className P6SpySqlFormatter
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
public class P6SpySqlFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                String category, String prepared, String sql, String url) {

        if (sql == null || sql.isBlank()) return "";
        if (Category.STATEMENT.getName().equals(category)) {

            String prettySQL = sql
                .replaceAll("(?i)\\bSELECT\\b", "\nSELECT")
                .replaceAll("(?i)\\bFROM\\b", "\n  FROM")
                .replaceAll("(?i)\\bWHERE\\b", "\n WHERE")
                .replaceAll("(?i)\\bAND\\b", "\n   AND")
                .replaceAll("(?i)\\bOR\\b", "\n    OR")
                .replaceAll("(?i)\\bINNER JOIN\\b", "\n  INNER JOIN")
                .replaceAll("(?i)\\bLEFT JOIN\\b", "\n   LEFT JOIN")
                .replaceAll("(?i)\\bORDER BY\\b", "\n ORDER BY")
                .replaceAll("(?i)\\bGROUP BY\\b", "\n GROUP BY")
                .replaceAll("(?i)\\bINSERT INTO\\b", "\nINSERT INTO")
                .replaceAll("(?i)\\bVALUES\\b", "\n VALUES")
                .replaceAll("(?i)\\bUPDATE\\b", "\nUPDATE")
                .replaceAll("(?i)\\bSET\\b", "\n   SET")
                .replaceAll("(?i)\\bDELETE FROM\\b", "\nDELETE FROM");

            return String.format("""
                \n┌─────────────────────────────────────────
                │ SQL [%dms]
                │%s
                └─────────────────────────────────────────
                """, elapsed, prettySQL.indent(2).stripTrailing());
        }
        return "";
    }
}