package learner.ObservationTable.Row;

import net.automatalib.common.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class RecursiveContent implements Content<Pair<Integer, Integer>> {
    List<BitSet> content = new ArrayList<>();

    @Override
    public boolean get(Pair<Integer, Integer> idx) {
        if (content.size() > idx.getFirst())
            return content.get(idx.getFirst()).get(idx.getSecond());
        return false;
    }

    @Override
    public void set(Pair<Integer, Integer> idx) {
        while(content.size() < idx.getFirst()) {
            content.add(new BitSet());
        }
        content.get(idx.getFirst()).set(idx.getSecond());
    }

    @Override
    public int getSeparator(Content<Pair<Integer, Integer>> content) {
        // TODO
        return 0;
    }

    @Override
    public boolean isAccepting() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null
                || !other.getClass().equals(RecursiveContent.class)
                || content.size() != ((RecursiveContent) other).content.size())
            return false;

        for (int idx = 0; idx < content.size(); idx++) {
            if (!content.get(idx).equals(((RecursiveContent) other).content.get(idx))) {
                return false;
            }
        }
        return true;
    }
}
