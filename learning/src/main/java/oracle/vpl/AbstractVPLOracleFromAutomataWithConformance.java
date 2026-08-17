//package oracle.vpl;
//
//import net.automatalib.alphabet.Alphabet;
//import net.automatalib.alphabet.VPAlphabet;
//import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
//import net.automatalib.word.Word;
//import oracle.rl.Oracle;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public abstract class AbstractVPLOracleFromAutomataWithConformance<I, M extends DeterministicAcceptorTS<?, I>> implements Oracle<I, M> {
//    private final VPAlphabet<I> alphabet;
//    private final DeterministicAcceptorTS<?, I> teacherAutomata;
//    private final int maxLength = 10;
//    private int equivalenceCounter = 0;
//    private int membershipCounter = 0;
//
//    public AbstractVPLOracleFromAutomataWithConformance(VPAlphabet<I> alphabet,
//                                                        DeterministicAcceptorTS<?, I> teacherAutomata) {
//        this.alphabet = alphabet;
//        this.teacherAutomata = teacherAutomata;
//    }
//
//    /**
//     * Membership query.
//     */
//    @Override
//    public boolean MembershipQuery(Word<I> word) {
//        membershipCounter++;
//        return teacherAutomata.accepts(word);
//    }
//
//    /**
//     * Equivalence query approximative par conformance testing.
//     *
//     * Retourne :
//     * - un contre-exemple si trouvé
//     * - null sinon
//     */
//    public Word<I> EquivalenceQuery(M hypothesis) {
//        this.equivalenceCounter++;
//
//        System.out.print("New Equivalence Query #");
//        System.out.println(equivalenceCounter);
//
//        for(int length = 1;  length < maxLength; length++) {
//            List<Word<I>> toTest = generateWellMatchedWords(length, 0);
//            System.out.println("Testing word of size " + length);
//            for (Word<I> word : toTest) {
//                if (hypothesis.accepts(word)
//                        != teacherAutomata.accepts(word)) {
//                    return word;
//                }
//                // if (word.equals(Word.fromSymbols("c2", "c1", "c1", "r2", "r1", "r2"))) { throw  new RuntimeException("Error"); }
//            }
//        }
//
//        return null;
//    }
//
//
//    /**
//     * Génère tous les mots bien parenthésés jusqu’à une certaine taille.
//     */
//    private List<Word<I>> generateWellMatchedWords(int size, int currentDepth) {
//        final List<Word<I>> words = new ArrayList<>();
//        if (size == 0) {
//            words.add(Word.epsilon());
//            return words;
//        }
//        if (currentDepth == size) {
//            final List<Word<I>> suffixes = generateWellMatchedWords(size-1, currentDepth-1);
//            for(I r: alphabet.getReturnAlphabet()){
//                for (Word<I> suffix : suffixes) {
//                    words.add(Word.fromWords(Word.fromLetter(r), suffix));
//                }
//            }
//            return words;
//        }
//
//        for(I i: alphabet){
//            List<Word<I>> suffixes = new ArrayList<>();
//            if (alphabet.isCallSymbol(i) && size - 1 > currentDepth) {
//                suffixes = generateWellMatchedWords(size-1, currentDepth+1);
//            } else if (alphabet.isReturnSymbol(i) && currentDepth > 0) {
//                suffixes = generateWellMatchedWords(size-1, currentDepth-1);
//            } else if (alphabet.isInternalSymbol(i)) {
//                suffixes = generateWellMatchedWords(size-1, currentDepth);
//            }
//            for (Word<I> suffix : suffixes) {
//                words.add(Word.fromWords(Word.fromLetter(i), suffix));
//            }
//        }
//        return words;
//    }
//
//    @Override
//    public void displayStats() {
//        System.out.println("Number of MQ: " + membershipCounter);
//        System.out.println("Number of EQ: " + equivalenceCounter);
//    }
//
//    @Override
//    public Alphabet<I> getInputAlphabet() {
//        return alphabet;
//    }
//}
