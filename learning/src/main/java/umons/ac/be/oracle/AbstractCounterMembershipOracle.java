package umons.ac.be.oracle;

import net.automatalib.word.Word;

public abstract class AbstractCounterMembershipOracle<I> implements CounterMembershipOracle<I> {
    int totalNumberMQ = 0;
    long totalTime = 0;
    long totalSizeMQ = 0;

    @Override
    public int getNumberOfMQ() {
        return totalNumberMQ;
    }

    @Override
    public long getTotalTime() {
        return totalTime / 1_000_000;
    }

    @Override
    public int getMeanMQSize() {
        return (int) (totalSizeMQ / totalNumberMQ);
    }

    @Override
    public Boolean answerQuery(Word<I> prefix, Word<I> suffix) {
        return answerQuery(Word.fromWords(prefix, suffix));
    }

    @Override
    public Boolean answerQuery(Word<I> word) {
        totalNumberMQ++;
        totalSizeMQ += word.size();
        long startTime = System.nanoTime();
        boolean answer = getAnswer(word);
        totalTime += System.nanoTime() - startTime;
        return answer;
    }

    public abstract Boolean getAnswer(Word<I> word);
}
