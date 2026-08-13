package learner.ObservationTable.Row;

import net.automatalib.common.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class RecursiveContent implements Content<Pair<Integer, Integer>> {
    List<BitSet> content = new ArrayList<>();

    public RecursiveContent(int separatorSize) {
        for (int i = 0; i < separatorSize; i++) {
            content.add(new BitSet());
        }
    }

    @Override
    public boolean get(Pair<Integer, Integer> idx) {
        if (content.size() > idx.getFirst())
            return content.get(idx.getFirst()).get(idx.getSecond());
        return false;
    }

    @Override
    public void set(Pair<Integer, Integer> idx) {
        while(content.size() <= idx.getFirst()) {
            content.add(new BitSet());
        }
        content.get(idx.getFirst()).set(idx.getSecond());
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

    @Override
    public boolean isAccepting() {
        return false;
    }

    public BitSet getContentVal(int idx) {
        if (content.size() <= idx)
            return new BitSet();
        return content.get(idx);
    }

    @Override
    public String toString() {
        return "RecursiveContent{" + "content=" + content + '}';
    }

    private int getFirstNonemptyContent(int startIdx) {
//        System.out.println("getFirstNonemptyContent " + startIdx + " " + content.size() + " " + content);
        for (int i = startIdx; i < content.size(); i++) {
            if (!content.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int contextEquivalent(RecursiveContent other, BitSet context){
        int maxSize = Math.max(content.size(), other.content.size());
//        System.out.println(other.content + " " + content + " " + maxSize);

        for (int i = 0; i < maxSize; i++) {
            if (other.content.size() <= i) {
                if (content.get(i).equals(context))
                    return i;
            }
            else if (content.size() <= i) {
                if (other.content.get(i).equals(context))
                    return i;
            }
            else {
//                System.out.println(content.get(i) + " " + other.content.get(i) + " " + context);
//                System.out.println(content.get(i).equals(context) && !other.content.get(i).equals(context));
//                System.out.println(!content.get(i).equals(context) && other.content.get(i).equals(context));
                if (
                        content.get(i).equals(context) && !other.content.get(i).equals(context)
                     || !content.get(i).equals(context) && other.content.get(i).equals(context)
                )
                    return i;

            }
        }
//        System.out.println("return -1");
        return -1;
    }

    @Override
    public int getSeparator(Content<Pair<Integer, Integer>> c) {
        if (content.size() < ((RecursiveContent) c).content.size()) {
            return ((RecursiveContent) c).getFirstNonemptyContent(content.size());
        }
        if (content.size() > ((RecursiveContent) c).content.size()) {
            return getFirstNonemptyContent(((RecursiveContent) c).content.size());
        }
        for (int i = 0; i < content.size(); i++) {
            if (!content.get(i).equals(((RecursiveContent) c).content.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public void addSeparator() {
        content.add(new BitSet());
    }

//    public int getContextSeparator(RecursiveContent other, BitSet context){
//        int maxSize = Math.max(content.size(), other.content.size());
//        for (int i = 0; i < maxSize; i++) {
//            if (
//                    content.get(i).equals(context) && !(other.content.size() < maxSize || other.content.get(i).equals(context))
//                    || (content.size() < maxSize || !content.get(i).equals(context)) && other.content.get(i).equals(context)
//            ) {
//                return i;
//            }
//        }
//        return -1;
//    }
}
