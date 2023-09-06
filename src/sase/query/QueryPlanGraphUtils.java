package sase.query;

/**
 * This class is used to wrap the QueryPlanGraph class and MultiQuery class
 * @author Xie Peiliang
 * @date 2020/8/11
 */
public class QueryPlanGraphUtils {

    /**
     * build the query plan graph by multi query file
     * @param multiQueryFile
     * @return
     */
    public static QueryPlanGraph buildQueryPlanGraph(String multiQueryFile) {
        MultiQuery multiQuery = new MultiQuery(multiQueryFile);
        PublicQueryUtils publicQueryUtils = new PublicQueryUtils();
        publicQueryUtils.getAllQueries(multiQuery.nfas);
        QueryPlanGraph queryPlanGraph = new QueryPlanGraph(publicQueryUtils.getNfaList(), publicQueryUtils.getEdgeList());
        return queryPlanGraph;
    }

}
