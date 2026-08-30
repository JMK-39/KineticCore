package dev.xyat.kineticcore.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 语言文件自动清理与补全工具 (保持原生排序版)
 */
public class LangCleanupTool {

    // ================= 配置区 =================
    // 修改为 true 才会真正写入文件
    private static final boolean EXECUTE_FIX = false;

    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "gui.kineticcore.recipehud.",
            "gui.kineticcore.recipehud.mode.",
            "gui.kineticcore.spawn.category.",
            "gui.kineticcore.modifier.armor.slot.",
            "cmd.kineticcore.author.",
            "gui.kineticcore.predicate.",
            "dmg.",
            "death.",
            "_",
            "item."
    );

    // ================= 主逻辑 =================
    public static void main(String[] args) {
        try {
            String assetPath = "src/main/resources/assets/kineticcore/lang/";
            Path enUsPath = Paths.get(assetPath + "en_us.json");
            Path zhCnPath = Paths.get(assetPath + "zh_cn.json");
            Path javaSrcPath = Paths.get("src/main/java");

            if (!Files.exists(enUsPath)) {
                System.err.println("错误：找不到 en_us.json，请检查路径: " + enUsPath.toAbsolutePath());
                return;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

            // 1. 扫描代码
            System.out.println("[1/5] 正在扫描代码库中的语言键引用...");
            String totalCode = readFolderContent(javaSrcPath);
            Set<String> keysInCode = extractKeysFromCode(totalCode);
            System.out.println(">>> 代码库中共引用了 " + keysInCode.size() + " 个键。");

            // 2. 加载当前 JSON 文件 (LinkedHashMap 会保持原生顺序)
            System.out.println("[2/5] 正在加载语言文件并解析...");
            Map<String, String> enUsData = loadJsonLeniently(enUsPath);
            Map<String, String> zhCnData = loadJsonLeniently(zhCnPath);
            System.out.println(">>> en_us.json 包含 " + enUsData.size() + " 个键。");
            System.out.println(">>> zh_cn.json 包含 " + zhCnData.size() + " 个键。");

            // 3. 分析差异
            System.out.println("[3/5] 正在对比分析差异...");
            List<String> missingKeys = new ArrayList<>();
            List<String> redundantKeys = new ArrayList<>();

            // 找缺失的（代码里有，配置里没有）
            for (String key : keysInCode) {
                if (key.endsWith(".")) continue;
                boolean isWhitelisted = WHITELIST_PREFIXES.stream().anyMatch(key::startsWith);
                if (isWhitelisted) continue;
                if (!enUsData.containsKey(key)) {
                    missingKeys.add(key);
                }
            }

            // 找多余的（配置里有，代码里没用，且不在白名单）
            for (String key : enUsData.keySet()) {
                boolean isWhitelisted = WHITELIST_PREFIXES.stream().anyMatch(key::startsWith);
                if (!isWhitelisted && !totalCode.contains(key)) {
                    redundantKeys.add(key);
                }
            }

            // 4. 输出直观分析报告
            System.out.println("[4/5] 分析结果如下：");
            printReport(missingKeys, redundantKeys);

            // 5. 执行修改 (直接使用原生的 LinkedHashMap，保持原顺序，新增的放末尾)
            if (EXECUTE_FIX) {
                System.out.println("[5/5] 正在执行自动补全与清理 (保持文件原有顺序)...");

                // 追加缺失的键 (由于是 LinkedHashMap，会自动排在文件末尾)
                for (String key : missingKeys) {
                    enUsData.put(key, "TODO: MISSING TRANSLATION");
                    zhCnData.putIfAbsent(key, "TODO: 缺失翻译");
                }

                // 移除冗余的键
                for (String key : redundantKeys) {
                    enUsData.remove(key);
                    zhCnData.remove(key);
                }

                // 直接将处理完的 LinkedHashMap 转换为 JSON 并写入
                Files.writeString(enUsPath, gson.toJson(enUsData), StandardCharsets.UTF_8);
                Files.writeString(zhCnPath, gson.toJson(zhCnData), StandardCharsets.UTF_8);

                System.out.println("✅ 处理完成！修改已保存至文件，且未破坏您的原生排版顺序。");
            } else {
                System.out.println("[5/5] 当前处于预览模式 (EXECUTE_FIX = false)，未修改文件。");
            }

        } catch (Exception e) {
            System.err.println("程序运行出错: " + e.getMessage());
        }
    }

    private static void printReport(List<String> missing, List<String> redundant) {
        Collections.sort(missing);
        Collections.sort(redundant);

        if (!missing.isEmpty()) {
            System.out.println("\n--- ❌ 待新增的语言键 (代码中有引用但 JSON 中缺失) ---");
            for (String key : missing) {
                System.out.println("  [+] " + key);
            }
        } else {
            System.out.println("\n✅ 缺失检查：所有代码引用的语言键均已存在。");
        }

        if (!redundant.isEmpty()) {
            System.out.println("\n--- ⚠️ 待清理的冗余键 (JSON 中存在但在代码中未发现) ---");
            for (String key : redundant) {
                System.out.println("  [-] " + key);
            }
        } else {
            System.out.println("\n✅ 冗余检查：未发现无用的语言键。");
        }
        System.out.println();
    }

    // 这里使用 LinkedHashMap 确保读取时记住文件的原生顺序
    private static Map<String, String> loadJsonLeniently(Path path) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.exists(path)) return map;

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement element = JsonParser.parseReader(jsonReader);

            if (element != null && element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            System.out.println(" [提示] 加载 " + path.getFileName() + " 失败，可能格式错误或为空。");
        }
        return map;
    }

    private static Set<String> extractKeysFromCode(String content) {
        Set<String> keys = new HashSet<>();
        Pattern pattern = Pattern.compile("\"([a-z0-9]+\\.kineticcore\\.[a-z0-9._-]+)\"");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private static String readFolderContent(Path root) throws IOException {
        if (!Files.exists(root)) return "";
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (IOException e) {
                            return "";
                        }
                    }).collect(Collectors.joining("\n"));
        }
    }
}
