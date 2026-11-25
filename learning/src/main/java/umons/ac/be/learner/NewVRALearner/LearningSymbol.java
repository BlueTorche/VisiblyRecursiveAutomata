package umons.ac.be.learner.NewVRALearner;

import net.automatalib.common.util.Triple;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LearningSymbol<I> {
    static final Map<Object, LearningSymbol<?>> mapSymb = new HashMap<>();

    public static <I>  LearningSymbol<I> getLearningSymbol(I symbol) {
        if (mapSymb.containsKey(symbol)) {
            return (LearningSymbol<I>) mapSymb.get(symbol);
        }
        return new LearningSymbol<>(symbol);
    }

    public static <I>  LearningSymbol<I> getLearningSymbol(I c, Word<LearningSymbol<I>> word, I r) {
        Triple<I, Word<LearningSymbol<I>>, I> triple = Triple.of(c, word, r);
        if (mapSymb.containsKey(triple)) {
            return (LearningSymbol<I>) mapSymb.get(triple);
        }
        return new LearningSymbol<>(c, word, r);
    }

    Word<I> expansion;

    private LearningSymbol(I symbol) {
        this.expansion = Word.fromLetter(symbol);
    }

    private LearningSymbol(I c, Word<LearningSymbol<I>> word, I r) {
        this.expansion = Word.fromWords(
                Word.fromLetter(c),
                expand(word),
                Word.fromLetter(r)
        );
    }

    public Word<I> expand() {
        return expansion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LearningSymbol<?> that = (LearningSymbol<?>) o;
        return expansion.equals(that.expansion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expansion);
    }

    @Override
    public String toString() {
        return expansion.toString();
    }

    public static <I> Word<I> expand(Word<LearningSymbol<I>> projection) {
        WordBuilder<I> wb = new WordBuilder<>();
        for (LearningSymbol<I> s : projection) {
            wb.append(s.expansion);
        }
        return wb.toWord();
    }
}
