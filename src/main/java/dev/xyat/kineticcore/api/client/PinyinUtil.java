package dev.xyat.kineticcore.api.client;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.Locale;

public class PinyinUtil {
    public static String getSearchData(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        StringBuilder syllables = new StringBuilder();
        boolean hasChinese = false;

        for (char c : str.toCharArray()) {
            if (!Pinyin.isChinese(c)) {
                continue;
            }

            String pinyin = Pinyin.toPinyin(c);
            if (pinyin == null || pinyin.isEmpty()) {
                continue;
            }

            String lower = pinyin.toLowerCase(Locale.ROOT);
            full.append(lower);
            initials.append(lower.charAt(0));

            if (!syllables.isEmpty()) {
                syllables.append(' ');
            }
            syllables.append(lower);

            hasChinese = true;
        }

        if (!hasChinese) {
            return "";
        }

        return full + " " + initials + " " + syllables;
    }
}