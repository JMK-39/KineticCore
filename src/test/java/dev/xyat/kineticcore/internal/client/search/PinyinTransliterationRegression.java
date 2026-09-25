package dev.xyat.kineticcore.internal.client.search;

/** Regression coverage for pinyin search over the full Unicode Han repertoire. */
public final class PinyinTransliterationRegression {
    public static void main(String[] args) {
        String cjkExtensionB = "\uD840\uDC00"; // U+20000, Unihan kMandarin: hē.

        check(KineticPinyin.readingCount() >= 44_000,
                "the bundled table must cover the broad Unicode Mandarin reading repertoire");
        check(KineticSearchRuntime.match(cjkExtensionB, "he"),
                "supplementary Han characters with a Mandarin reading must support pinyin search");
        check("he h he".equals(KineticSearchRuntime.pinyinSearchData(cjkExtensionB)),
                "supplementary Han pinyin must produce the same full/initial/spaced index fields");
        check("qiu q qiu".equals(KineticSearchRuntime.pinyinSearchData("㐀")),
                "CJK Extension A characters must be represented in the built-in pinyin data");
        check("zuanshijian zsj zuan shi jian".equals(KineticSearchRuntime.pinyinSearchData("钻石剑")),
                "common item names must preserve full, initials, and syllable-spaced search data");
        check(KineticSearchRuntime.match("钻石剑", "zuanshijian"),
                "full pinyin queries must continue to match");
        check(KineticSearchRuntime.match("钻石剑", "zsj"),
                "initial-only queries must continue to match");
        check(KineticSearchRuntime.prepare("钻石剑").matchRank("zuanshijian") == 2,
                "full-pinyin query ranking must remain stable");
        check(KineticSearchRuntime.prepare("钻石剑").matchRank("zsj") == 3,
                "initial-only query ranking must remain stable");
        check(KineticSearchRuntime.match("钻石剑", "zuansj"),
                "mixed full-pinyin and syllable-initial queries must continue to match");
        check("long l long".equals(KineticSearchRuntime.pinyinSearchData("龍")),
                "traditional Han characters must receive Mandarin readings");
        check("lv l lv".equals(KineticSearchRuntime.pinyinSearchData("吕")),
                "ü readings must normalize to searchable ASCII v notation");
        check(KineticSearchRuntime.match("𠮷", "𠮷"),
                "unsupported literal Han characters must remain directly searchable");
        System.out.println("PASS: KineticCore pinyin transliteration and search");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
