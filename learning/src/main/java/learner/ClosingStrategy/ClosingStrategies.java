//package learner.ClosingStrategy;
//
//public class ClosingStrategies {
//    /**
//     * Closing strategy that selects the first row from each equivalence class as representative.
//     */
//    public static final ClosingStrategy<@Nullable Object, @Nullable Object> CLOSE_FIRST =
//            new ClosingStrategy<>() {
//
//                @Override
//                public <RI, RD> List<Row<RI>> selectClosingRows(List<List<Row<RI>>> unclosedClasses,
//                                                                ObservationTable<RI, RD> table,
//                                                                MembershipOracle<RI, RD> oracle) {
//                    List<Row<RI>> result = new ArrayList<>(unclosedClasses.size());
//                    for (List<Row<RI>> clazz : unclosedClasses) {
//                        result.add(clazz.get(0));
//                    }
//                    return result;
//                }
//
//                @Override
//                public String toString() {
//                    return "CloseFirst";
//                }
//            };
//}
