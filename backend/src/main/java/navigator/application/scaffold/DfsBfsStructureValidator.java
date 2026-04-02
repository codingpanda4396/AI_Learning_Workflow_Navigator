package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * DFS/BFS 的 STRUCTURE：禁止实现细节、复杂度、题型与长段机制。
 */
@Component
public class DfsBfsStructureValidator implements StructureValidator {

    public static final String PACK_ID = "ds_dfs_bfs";
    public static final String STAGE_KEY = "STRUCTURE";

    private static final int MIN_CHARS = 14;
    private static final Pattern CODEISH = Pattern.compile(
            "(public\\s+static|void\\s+main|import\\s+java|\\bdef\\b|\\bint\\s+\\w+\\s*[=;]|for\\s*\\(|while\\s*\\(|#include)",
            Pattern.CASE_INSENSITIVE);

    /** 越界：实现/复杂度/题型/机制细讲 */
    private static final String[] BOUNDARY_TERMS_ZH = {
            "复杂度", "时间复杂度", "空间复杂度", "o(", "O（", "O(",
            "递归栈", "栈", "队列",
            "最短路", "最短路径", "dijkstra",
            "刷题", "leetcode", "力扣", "题解", "模板",
            "代码", "实现", "伪代码",
            "入队", "出队", "回溯",
    };

    @Override
    public ValidationResult validate(StructureValidationContext ctx) {
        String raw = ctx.getUserInput() != null ? ctx.getUserInput().trim() : "";
        if (isStructureMcqSubmission(raw)) {
            return ValidationResult.builder()
                    .passed(true)
                    .errorType(null)
                    .message("通过")
                    .suggestions(List.of())
                    .build();
        }
        if (raw.length() < MIN_CHARS) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("INSUFFICIENT_CONTENT")
                    .message("先用两三句话写清「位置 / 作用 / 边界 / 差异」之一，不要太短。")
                    .suggestions(List.of("避免一句话带过", "不写实现与复杂度"))
                    .build();
        }
        String norm = raw.toLowerCase(Locale.ROOT);
        String compactZh = raw.replaceAll("\\s+", "");

        List<String> hits = new ArrayList<>();
        for (String t : BOUNDARY_TERMS_ZH) {
            if (t.length() <= 1) {
                continue;
            }
            if (compactZh.contains(t) || norm.contains(t.toLowerCase(Locale.ROOT))) {
                hits.add(t);
            }
        }
        if (CODEISH.matcher(raw).find()) {
            hits.add("代码片段");
        }

        if (!hits.isEmpty()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("BOUNDARY_VIOLATION")
                    .message("当前阶段只建立结构：不写实现、不讲复杂度、不刷题型细节。")
                    .suggestions(List.of(
                            "只说「解决什么问题 / 在体系里站哪 / 和相邻概念差在哪」",
                            "把 \"" + hits.get(0) + "\" 留到下一阶段再展开"
                    ))
                    .build();
        }

        return ValidationResult.builder()
                .passed(true)
                .errorType(null)
                .message("通过")
                .suggestions(List.of())
                .build();
    }

    private static boolean isStructureMcqSubmission(String raw) {
        return raw.startsWith("STRUCTURE:sq1:")
                || raw.startsWith("STRUCTURE:sq2:")
                || raw.startsWith("STRUCTURE:sq3:");
    }
}
