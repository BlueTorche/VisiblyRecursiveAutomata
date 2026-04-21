package umons.ac.be.vra;

import java.util.*;

public class VRAAlphabet {
    private final Set<String> callSymbols = new HashSet<>();
    private final Set<String> returnSymbols = new HashSet<>();
    private final Set<String> internalSymbols = new HashSet<>();
    private final Set<String> proceduralSymbols = new HashSet<>();

    private final Map<String, String> proceduralToCall = new HashMap<>();
    private final Map<String, String> proceduralToReturn= new HashMap<>();
    private final Map<String, Set<String>> callToProcedurals = new HashMap<>();
    private final Map<String, Set<String>> returnToProcedurals = new HashMap<>();

    public enum SymbolType {
        CALL,
        INTERNAL,
        RETURN,
        PROCEDURAL
    }

    public void addCallSymbol(String c) {
        callSymbols.add(c);
        callToProcedurals.put(c, new HashSet<>());
    }

    public void addReturnSymbol(String r) {
        returnSymbols.add(r);
        returnToProcedurals.put(r, new HashSet<>());
    }

    public void addInternalSymbol(String symbol) {
        internalSymbols.add(symbol);
    }
    public void addProceduralSymbol(String p, String c, String r) {
        proceduralSymbols.add(p);

        proceduralToCall.put(p,c);
        proceduralToReturn.put(p,r);

        callToProcedurals.get(c).add(p);
        returnToProcedurals.get(r).add(p);
    }

    public Set<String> getCallSymbols() {
        return callSymbols;
    }

    public Set<String> getReturnSymbols() {
        return returnSymbols;
    }

    public Set<String> getInternalSymbols() {
        return internalSymbols;
    }
    public Set<String> getProceduralSymbols() {
        return proceduralSymbols;
    }

    public String getCallFromProcedural(String p) { return proceduralToCall.get(p); }
    public String getReturnFromProcedural(String p) { return proceduralToReturn.get(p); }
    public Set<String> getProceduralsFromCall(String c) { return callToProcedurals.get(c); }
    public Set<String> getProceduralsFromReturn(String r) { return returnToProcedurals.get(r); }


    public SymbolType kindOfSymbol(String symbol) {
        if (callSymbols.contains(symbol))
            return SymbolType.CALL;
        if (returnSymbols.contains(symbol))
            return SymbolType.RETURN;
        if (internalSymbols.contains(symbol))
            return SymbolType.INTERNAL;
        if (proceduralSymbols.contains(symbol))
            return SymbolType.PROCEDURAL;
        return null;
    }
}
