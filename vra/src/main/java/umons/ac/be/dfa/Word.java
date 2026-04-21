package umons.ac.be.dfa;

import java.util.*;

public final class Word {

    private final List<String> symbols;
    private int hash = 0;

    public Word(List<String> symbols) {
        this.symbols = List.copyOf(symbols); // immuable
        this.hash = Objects.hash(this.symbols);
    }

    public static Word of(String... symbols) {
        return new Word(Arrays.asList(symbols));
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public Word append(String symbol) {
        List<String> newWord = new ArrayList<>(symbols);
        newWord.add(symbol);
        return new Word(newWord);
    }

    public Word concat(Word other) {
        List<String> newWord = new ArrayList<>(symbols);
        newWord.addAll(other.symbols);
        return new Word(newWord);
    }

    public List<Word> getPrefixes() {
        List<Word> prefixes = new ArrayList<>(symbols.size() + 1);
        prefixes.add(Word.of());
        for (int i = 0; i < symbols.size(); i++) {
            prefixes.add(prefixes.get(i).concat(Word.of(symbols.get(i))));
        }
        return prefixes;
    }

    public List<Word> getSuffixes() {
        List<Word> suffixes = new ArrayList<>(symbols.size() + 1);
        suffixes.add(Word.of());
        for (int i = 0; i < symbols.size(); i++) {
            suffixes.add(Word.of(symbols.get(symbols.size() - i - 1)).concat(suffixes.get(i)));
        }
        return suffixes;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Word w)) return false;
        return symbols.equals(w.symbols);
    }

    @Override
    public int hashCode() {
        if (this.hash == 0) {
            this.hash = Objects.hash(this.symbols);
        }
        return this.hash;
    }

    @Override
    public String toString() {
        return String.join("·", symbols);
    }
}
