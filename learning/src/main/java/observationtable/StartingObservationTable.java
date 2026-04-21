package observationtable;

import java.util.*;
import umons.ac.be.dfa.Word;

/**
 * Observation table for active learning of automata (Angluin-style L*).
 *
 * R: finite prefix-closed set of representatives
 * S: finite suffix-closed set of separators
 * T: (R ∪ RΣ) × S → {true, false}
 */
public class StartingObservationTable {
    private final Set<Word> R = new HashSet<>();
    private final Set<Word> S = new HashSet<>();
    private final Set<String> alphabet = new HashSet<>();

    private final Map<Word, Map<Word, Boolean>> T = new HashMap<>();

    public StartingObservationTable(Set<String> alphabet) {
        this.alphabet.addAll(alphabet);
    }

    /* ---------------- R / S ---------------- */
    public void addToR(Word r) {
        for (Word w : r.getPrefixes()) {
            if (!R.contains(w)) {
                R.add(w);
                ensureRow(w);

                for (String a : alphabet) {
                    ensureRow(w.append(a));
                }
            }
        }
    }

    public void addToS(Word s) {
        for (Word w : s.getSuffixes()) {
            if (!S.contains(w)) {
                 S.add(s);
                 ensureColumn(s);
            }
        }
    }

    /* ---------------- T ---------------- */
    public void setValue(Word prefix, Word suffix, boolean value) {
        ensureRow(prefix);
        ensureColumn(suffix);

        T.get(prefix).put(suffix, value);
    }

    public Boolean getValue(Word prefix, Word suffix) {
        return T.getOrDefault(prefix, Map.of()).get(suffix);
    }

    /* ---------------- RΣ ---------------- */
    public Set<Word> getRSigma() {
        Set<Word> result = new HashSet<>();
        for (Word r : R) {
            for (String a : alphabet) {
                result.add(r.append(a));
            }
        }
        return result;
    }

    public Set<Word> getRUnionRSigma() {
        Set<Word> result = new HashSet<>(R);
        result.addAll(getRSigma());
        return result;
    }

    /* ---------------- maintenance ---------------- */
    private void ensureRow(Word w) {
        T.computeIfAbsent(w, k -> new HashMap<>());

        for (Word s : S) {
            T.get(w).putIfAbsent(s, false);
        }
    }

    private void ensureColumn(Word s) {
        for (Word r : getRUnionRSigma()) {
            T.computeIfAbsent(r, k -> new HashMap<>());
            T.get(r).putIfAbsent(s, false);
        }
    }

    /* ---------------- debug ---------------- */
    public void print() {
        System.out.println("Observation Table");

        Set<Word> rows = getRUnionRSigma();

        for (Word r : rows) {
            System.out.print(r + " | ");
            for (Word s : S) {
                System.out.print(getValue(r, s) + " ");
            }
            System.out.println();
        }
    }
}