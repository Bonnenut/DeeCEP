package sase.query;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * the definition of query plan graph
 * @author Xie Peiliang
 * @date 2020/8/8
 */
public class QueryPlanGraph {
    // the number of vertexes
    private int vLen;
    // the number of edges
    private int eLen;
    // the list of vertexes
    private VertexNode[] vertexNodes;

    /**
     * the definition of graph vertex
     */
    public class VertexNode {
        NFA nfa;
        // the first edge
        EdgeNode firstEdge;
    }

    /**
     * the definition of graph edge
     */
    public class EdgeNode {
        // the index of vertex in edge
        int vexIndex;
        EdgeNode nextNode;
    }

    /**
     * construct
     * @param nfaList
     * @param edgeList
     */
    public QueryPlanGraph(List<NFA> nfaList, List<List<NFA>> edgeList) {
        this.vLen = nfaList.size();
        this.eLen = edgeList.size();
        buildQueryPlanGraph(nfaList, edgeList);
    }

    /**
     * 将node节点链接到目标节点的最后
     * @param target
     * @param node
     */
    public void linkLast(EdgeNode target, EdgeNode node) {
        while(target.nextNode != null) {
            target = target.nextNode;
        }
        target.nextNode = node;
    }

    /**
     * 返回查询nfa所在顶点容器中的位置
     * @param nfa
     * @return
     */
    public int getPosition(NFA nfa) {
        for(int i = 0; i < vertexNodes.length; i++) {
            try {
                if(nfa.getHashValue().equals(vertexNodes[i].nfa.getHashValue())) {
                    return i;
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 创建基于邻接表的图
     */
    public void buildQueryPlanGraph(List<NFA> nfaList, List<List<NFA>> edgeList) {
        // 初始化顶点数组
        vertexNodes = new VertexNode[this.vLen];
        for(int i = 0; i < this.vLen; i++) {
            vertexNodes[i] = new VertexNode();
            vertexNodes[i].nfa = nfaList.get(i);
            vertexNodes[i].firstEdge = null;
        }

        // 初始化边
        for(int i = 0; i < this.eLen; i++) {
            NFA nfa1 = edgeList.get(i).get(0); // 起始查询
            NFA nfa2 = edgeList.get(i).get(1); // 终点查询
            int start = getPosition(nfa1); // 起始查询位置
            int end = getPosition(nfa2); // 终点查询位置

            EdgeNode edgeNode1 = new EdgeNode(); // 新建一个边界点
            edgeNode1.vexIndex = end; // 指向终点位置

            /**
             * 如果该顶点的第一条连接边为空则指向新建的边节点edgeNode1
             * 否则将该边节点放在最后
             */
            if(vertexNodes[start].firstEdge == null) {
                vertexNodes[start].firstEdge = edgeNode1;
            } else {
                linkLast(vertexNodes[start].firstEdge, edgeNode1);
            }
        }
    }

    @Override
    public String toString() {
        String str = "the query plan graph is: \n";
        for(int i = 0; i < this.vLen; i++) {
            str += String.valueOf(i) + ": ";
            EdgeNode edgeNode = vertexNodes[i].firstEdge;
            while(edgeNode != null) {
                str += String.valueOf(edgeNode.vexIndex);
                edgeNode = edgeNode.nextNode;
            }
            str += "\n";
        }
        return str;
    }
}
