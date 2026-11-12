package com.nextfit.ui.fx;

import com.nextfit.model.Member;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Matching rules of the global search. It ignores case and accents and requires every
 * word typed by the user to appear somewhere in the searched fields.
 */
public final class GlobalSearch {

    private GlobalSearch() {
    }

    /**
     * @param text any text, may be null
     * @return the text in lower case and without accents
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * @param query  what the user typed
     * @param fields the texts where the words may appear
     * @return true when the query is not blank and every word of it is inside some field
     */
    public static boolean matches(String query, String... fields) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) {
            return false;
        }
        StringBuilder all = new StringBuilder();
        for (String field : fields) {
            all.append(normalize(field)).append(' ');
        }
        for (String word : wanted.split("\\s+")) {
            if (!all.toString().contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param query    what the user typed
     * @param keywords alternative names of something, each one is tried alone
     * @return true when the query matches at least one of the keywords
     */
    public static boolean matchesAny(String query, List<String> keywords) {
        for (String keyword : keywords) {
            if (matches(query, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param moduleId the module that shows the members
     * @param query    what the user typed
     * @param members  the members to look into
     * @param limit    the maximum number of results
     * @return the members whose name, document or phone match, as results
     */
    public static List<SearchHit> memberHits(String moduleId, String query, List<Member> members, int limit) {
        List<SearchHit> hits = new ArrayList<>();
        for (Member member : members) {
            if (hits.size() >= limit) {
                break;
            }
            if (matches(query, member.getFullName(), member.getDocumentNumber(), member.getPhone())) {
                String state = member.isActive() ? "" : " · Inactivo";
                hits.add(new SearchHit(moduleId, String.valueOf(member.getId()), member.getFullName(),
                        "Doc. " + member.getDocumentNumber() + " · " + member.getPhone() + state));
            }
        }
        return hits;
    }
}
