package sase.query;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * MultiQuery的辅助类，用于构建公共中间查询
 */
public class PublicQueryUtils {

    private NFA nfa1;
    private NFA nfa2;
    private NFA q; //nfa1和nfa2的公共中间查询
    private List<String> publicEventTypeSequence; //保存公共SEQ的事件类型
    //private List<String> publicEventTagSequence; //保存公共SEQ的事件变量
    private List<Integer> kleeneClosureOrNegationLabel; //记录闭包和否定位置

    private List<NFA> nfaList; //存储原始查询和公共中间查询
    private List<List<NFA>> edgeList; //存储查询的对应关系，父节点在前，子节点在后

    public PublicQueryUtils() {
        nfaList = new ArrayList<>();
        edgeList = new ArrayList<>();
    }

    /**
     * 将多查询两两进行公共中间查询的构造
     *
     * @param nfas
     * @return
     */
    public void getAllQueries(List<NFA> nfas) {
        int nfaCount = nfas.size();
        //base case
        if (nfaCount == 1)
            return;
        for (int i = 0; i < nfaCount; i++) {
            nfaList.add(nfas.get(i));
        }
        List<NFA> temp = new ArrayList<>(); // 保存用于下一步计算的nfa
        List<NFA> edge = null; // 用户记录父节点到
        //计算两两公共中间查询
        for (int i = 0; i < nfaCount - 1; i++) {
            for (int j = i + 1; j < nfaCount; j++) {
                NFA nfa = getPublicQueryOfTwoQuery(nfas.get(i), nfas.get(j));
                try {
                    // 如果nfa不存在temp中，则添加
                    if (!existenceCheck(temp, nfa)) {
                        // 添加nfa到nfaList
                        temp.add(nfa);
                        this.nfaList.add(nfa);
                        // 添加两条边
                        edge = new ArrayList<>();
                        edge.add(nfa);
                        edge.add(nfas.get(i));
                        this.edgeList.add(edge);
                        edge = new ArrayList<>();
                        edge.add(nfa);
                        edge.add(nfas.get(j));
                        this.edgeList.add(edge);

                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }

        getAllQueries(temp);
    }

    /**
     * 构建两个查询的公共中间查询
     *
     * @return
     */
    public NFA getPublicQueryOfTwoQuery(NFA q1, NFA q2) {
        this.nfa1 = q1;
        this.nfa2 = q2;

        q = new NFA();
        getPublicSEQ(nfa1, nfa2);
        getPublicPredicates(nfa1, nfa2);
        getPublicTimeWindow(nfa1, nfa2);

        return q;
    }

    /**
     * 构建q1和q2的公共SEQ
     *
     * @param q1
     * @param q2
     */
    public void getPublicSEQ(NFA q1, NFA q2) {
        String res = "";
        publicEventTypeSequence = new ArrayList<>();
        //publicEventTagSequence = new ArrayList<>();
        kleeneClosureOrNegationLabel = new ArrayList<>();

        int q1StatesLength = q1.getStates().length;
        int q2StatesLength = q2.getStates().length;
        //动态计算最长公共子序列
        int[][] dp = new int[q1StatesLength + 1][q2StatesLength + 1];
        //记录最长公共子序列
        int[][] trace = new int[q1StatesLength + 1][q2StatesLength + 1];
        //标记节点是否是闭包、否定，只要有一方是闭包，则为1，两方都是否定，则为2
        int[][] label = new int[q1StatesLength + 1][q2StatesLength + 1];

        //先考虑否定的情况，否定只在skip-till-any-match策略下支持
        //没有否定，使用最长公共子序列思想进行计算
        for (int i = 1; i < q1StatesLength + 1; i++) {
            for (int j = 1; j < q2StatesLength + 1; j++) {
                if (q1.getStates(i - 1).getEventType().equals(q2.getStates(j - 1).getEventType())) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    trace[i][j] = 1;
                    if (q1.getStates(i - 1).isKleeneClosure || q2.getStates(j - 1).isKleeneClosure)
                        label[i][j] = 1;
                    if (q1.getStates(i - 1).isNegation && q2.getStates(j - 1).isNegation)
                        label[i][j] = 2;
                } else if (dp[i][j - 1] >= dp[i - 1][j]) {
                    dp[i][j] = dp[i][j - 1];
                    trace[i][j] = 2;
                } else {
                    dp[i][j] = dp[i - 1][j];
                    trace[i][j] = 3;
                }
            }
        }

        getPublicSequence(trace, label, q1StatesLength, q2StatesLength);
        res = "PATTERN SEQ(";
        char tag = 'a';
        for (int i = 0; i < publicEventTypeSequence.size(); i++) {
            if (i == 0) {
                if (kleeneClosureOrNegationLabel.get(i) == 0)
                    res += publicEventTypeSequence.get(i) + " " + tag++;
                else if (kleeneClosureOrNegationLabel.get(i) == 1)
                    res += publicEventTypeSequence.get(i) + "+ " + tag++ + "[]";
                else
                    res += "!{" + publicEventTypeSequence.get(i) + " " + tag++ + "}";
            } else {
                if (kleeneClosureOrNegationLabel.get(i) == 0)
                    res += ", " + publicEventTypeSequence.get(i) + " " + tag++;
                else if (kleeneClosureOrNegationLabel.get(i) == 1)
                    res += ", " + publicEventTypeSequence.get(i) + "+ " + tag++ + "[]";
                else
                    res += ", !{" + publicEventTypeSequence.get(i) + " " + tag++ + "}";
            }
        }
        res += ")";

        q.morePartitionAttribute = new ArrayList<String>();
        q.hasMorePartitionAttribute = false;
        q.parseFastQueryLine(res); //parse the public SEQ
    }

    /**
     * 根据状态c数组获取最长公共子序列
     *
     * @param trace
     * @param i
     * @param j
     */
    public void getPublicSequence(int[][] trace, int[][] label, int i, int j) {
        if (i == 0 || j == 0)
            return;
        if (trace[i][j] == 1) {
            getPublicSequence(trace, label, i - 1, j - 1);
            publicEventTypeSequence.add(nfa1.getStates(i - 1).getEventType());
            //publicEventTagSequence.add(nfa1.getStates(i - 1).getTag());
            //记录闭包
            if (label[i][j] == 1)
                kleeneClosureOrNegationLabel.add(1);
            else if (label[i][j] == 2)
                kleeneClosureOrNegationLabel.add(2);
            else
                kleeneClosureOrNegationLabel.add(0);
        } else if (trace[i][j] == 2) {
            getPublicSequence(trace, label, i, j - 1);
        } else {
            getPublicSequence(trace, label, i - 1, j);
        }
    }

    /**
     * 获取公共谓词--修正
     */
    public void getPublicPredicates(NFA q1, NFA q2) {

        String str = "";

        //首先获取策略
        this.q.setSelectionStrategy(q1.getSelectionStrategy());
        //之后获取分区属性
        this.q.setPartitionAttribute(q1.getPartitionAttribute());

        char tag = 'a';
        String type1 = "";
        String type2 = "";
        Set<String> resultSet = new HashSet<>();
        for (int i = 0; i < publicEventTypeSequence.size(); i++) {
            type1 = publicEventTypeSequence.get(i);
            for (int j = 0; j < q1.getStates().length; j++) {
                //如果q1的状态事件与公共seq的第i个事件相同
                if (q1.getStates(j).getEventType().equals(type1)) {
                    for (int k = 0; k < q1.getStates(j).getEdges().length; k++) {
                        for (int l = 0; l < q1.getStates(j).getEdges(k).getPredicates().length; l++) {
                            str = "AND ";
                            String predicateDescription = q1.getStates(j).getEdges(k).getPredicates()[l].predicateDescription;
                            //如果是出现a[1].price > a[i-1].price
                            if (predicateDescription.contains("$previous")) {
                                int index = predicateDescription.indexOf("$");
                                str += tag + "[i]." + predicateDescription.substring(0, index) + tag + "[i-1]" +
                                        predicateDescription.substring(index + 9);
                            }
                            //如果是出现类似于a.price < b.price
                            else if (predicateDescription.contains("$")) {
                                //找到$后面的数字
                                int index = predicateDescription.indexOf("$") + 1;
                                int temp = Integer.parseInt(String.valueOf(predicateDescription.charAt(index)));
                                type2 = q1.getStates(temp - 1).getEventType();
                                //如果谓词涉及到的其他类型也在公共seq中，则将其加入
                                if (publicEventTypeSequence.contains(type2)) {
                                    int type2Index = publicEventTypeSequence.indexOf(type2);
                                    //如果a的类型是闭包，则要写为a[1].price
                                    if (q1.getStates(j).getStateType().equals("kleeneClosure"))
                                        str += tag + "[1]." + predicateDescription;
                                    else
                                        str += tag + "." + predicateDescription;
                                    str = str.replace("$", "");
                                    str = str.replace(String.valueOf(predicateDescription.charAt(index)), Character.toString((char) (tag + type2Index)));
                                }
                            }
                            //如果是普通的a.price > 100
                            else {
                                //如果当前状态是闭包
                                if (q1.getStates(j).getStateType().equals("kleeneClosure"))
                                    str += tag + "[1]." + predicateDescription;
                                else
                                    str += tag + "." + predicateDescription;
                            }
                            resultSet.add(str);
                        }
                    }
                }
            }
            for (int j = 0; j < q2.getStates().length; j++) {
                //如果q1的状态事件与公共seq的第i个事件相同
                if (q2.getStates(j).getEventType().equals(type1)) {
                    for (int k = 0; k < q2.getStates(j).getEdges().length; k++) {
                        for (int l = 0; l < q2.getStates(j).getEdges(k).getPredicates().length; l++) {
                            str = "AND ";
                            String predicateDescription = q2.getStates(j).getEdges(k).getPredicates()[l].predicateDescription;
                            //如果是出现a[1].price > a[i-1].price
                            if (predicateDescription.contains("$previous")) {
                                int index = predicateDescription.indexOf("$");
                                str += tag + "[i]." + predicateDescription.substring(0, index) + tag + "[i-1]" +
                                        predicateDescription.substring(index + 9);
                            }
                            //如果是出现类似于a.price < b.price
                            else if (predicateDescription.contains("$")) {
                                //找到$后面的数字
                                int index = predicateDescription.indexOf("$") + 1;
                                int temp = Integer.parseInt(String.valueOf(predicateDescription.charAt(index)));
                                type2 = q1.getStates(temp - 1).getEventType();
                                //如果谓词涉及到的其他变量也在公共seq中，则将其加入
                                if (publicEventTypeSequence.contains(type2)) {
                                    int type2Index = publicEventTypeSequence.indexOf(type2);
                                    //如果a的类型是闭包，则要写为a[1].price
                                    if (q2.getStates(j).getStateType().equals("kleeneClosure"))
                                        str += tag + "[1]." + predicateDescription;
                                    else
                                        str += tag + "." + predicateDescription;
                                    str = str.replace("$", "");
                                    str = str.replace(String.valueOf(predicateDescription.charAt(index)), Character.toString((char) (tag + type2Index)));
                                }
                            }
                            //如果是普通的a.price > 100
                            else {
                                //如果当前状态是闭包
                                if (q2.getStates(j).getStateType().equals("kleeneClosure"))
                                    str += tag + "[1]." + predicateDescription;
                                else
                                    str += tag + "." + predicateDescription;
                            }
                            resultSet.add(str);
                        }
                    }
                }
            }
            tag++;

        }
        if (resultSet.size() != 0) {
            processResultSet(resultSet);
        }

    }

    /**
     * 处理初步获取的谓词
     *
     * @param resultSet
     */
    public void processResultSet(Set<String> resultSet) {
        char tag = 'a';
        int cnt = 0; // 处理的个数
        List<String> list = null;
        while (cnt <= resultSet.size()) {
            list = new ArrayList<>();
            for (String str : resultSet) {
                //如果不是kleeneClosure类型不仅是对第一个元素做约束
                if (!str.contains("[i-1]")) {
                    if (str.charAt(0) == tag) {
                        list.add(str);
                        cnt++;
                    }
                } else {
                    cnt++;
                }
            }
            predicateAnalysis(list);
            tag++;
        }
    }

    /**
     * 对同一类型下的predicate进行分析
     *
     * @param list
     */
    public void predicateAnalysis(List<String> list) {
        if(list.size() == 0)
            return;
        //标签，0表示p1包含<，1表示p1包含>，2表示p1包含=，3表示p1包含<=，4表示p1包含>=，5表示p1包含!=
        int label1 = -1;
        //0表示包含-，1表示包含+，2表示包含*，3表示包含/，4表示包含%
        int label2 = -1;
        for (int i = 0; i < list.size() - 1; i++) {
            String token1 = list.get(i).substring(4);

            if (token1.contains("<="))
                label1 = 0;
            else if(token1.contains(">="))
                label1 = 1;
            else if(token1.contains("<"))
                label1 = 2;
            else if(token1.contains(">"))
                label1 = 3;
            else if(token1.contains("!="))
                label1 = 4;
            else if(token1.contains("="))
                label1 = 5;

            if(token1.contains("-"))
                label2 = 0;
            else if(token1.contains("+"))
                label2 = 1;
            else if(token1.contains("*"))
                label2 = 2;
            else if(token1.contains("/"))
                label2 = 3;
            else if(token1.contains("%"))
                label2 = 4;

            for (int j = i + 1; j < list.size(); j++) {
                String token2 = list.get(j).substring(4);
                // label3,4含义同上
                int label3 = -1;
                int label4 = -1;

                if (token2.contains("<="))
                    label3 = 0;
                else if(token2.contains(">="))
                    label3 = 1;
                else if(token2.contains("<"))
                    label3 = 2;
                else if(token2.contains(">"))
                    label3 = 3;
                else if(token2.contains("!="))
                    label3 = 4;
                else if(token2.contains("="))
                    label3 = 5;

                if(token2.contains("-"))
                    label4 = 0;
                else if(token2.contains("+"))
                    label4 = 1;
                else if(token2.contains("*"))
                    label4 = 2;
                else if(token2.contains("/"))
                    label4 = 3;
                else if(token2.contains("%"))
                    label4 = 4;

                // 如果都是<
                if(label1 == 2 && label3 == 2) {
                    String name1 = token1.substring(token1.indexOf("."), token1.indexOf("<") - 1);
                    String name2 = token2.substring(token2.indexOf("."), token2.indexOf("<") - 1);
                    if(name1.equals(name2)) {
                        String value1 = token1.substring(token1.indexOf("<") + 2);
                        String value2 = token2.substring(token2.indexOf("<") + 2);
                        // 如果右侧都是数值，则取大的数值作为list[i]的右侧数值，当前list[j]从list中去掉
                        if(isNumeric(value1) && isNumeric(value2)) {
                            int max = Math.max(Integer.parseInt(value1), Integer.parseInt(value2));
                            list.set(i, token1.substring(0, token1.indexOf("<") + 2) + String.valueOf(max));
                            list.remove(j);
                        }
                    }
                }
                // 如果都是>
                else if(label1 == 3 && label3 == 3) {
                    String name1 = token1.substring(token1.indexOf("."), token1.indexOf(">") - 1);
                    String name2 = token2.substring(token2.indexOf("."), token2.indexOf(">") - 1);
                    if(name1.equals(name2)) {
                        String value1 = token1.substring(token1.indexOf(">") + 2);
                        String value2 = token2.substring(token2.indexOf(">") + 2);
                        // 如果右侧都是数值，则取较小的数值作为list[i]的右侧数值，当前list[j]从list中去掉
                        if(isNumeric(value1) && isNumeric(value2)) {
                            int min = Math.min(Integer.parseInt(value1), Integer.parseInt(value2));
                            list.set(i, token1.substring(0, token1.indexOf(">") + 2) + String.valueOf(min));
                            list.remove(j);
                        }
                    }
                }
                // 如果都是<=
                else if(label1 == 0 && label3 == 0) {
                    String name1 = token1.substring(token1.indexOf("."), token1.indexOf("<") - 1);
                    String name2 = token2.substring(token2.indexOf("."), token2.indexOf("<") - 1);
                    if(name1.equals(name2)) {
                        String value1 = token1.substring(token1.indexOf("=") + 2);
                        String value2 = token2.substring(token2.indexOf("=") + 2);
                        // 如果右侧都是数值，则取校的数值作为list[i]的右侧数值，当前list[j]从list中去掉
                        if(isNumeric(value1) && isNumeric(value2)) {
                            int max = Math.max(Integer.parseInt(value1), Integer.parseInt(value2));
                            list.set(i, token1.substring(0, token1.indexOf("=") + 2) + String.valueOf(max));
                            list.remove(j);
                        }
                    }
                }
                // 如果都是>=
                else if(label1 == 1 && label3 == 1) {
                    String name1 = token1.substring(token1.indexOf("."), token1.indexOf(">") - 1);
                    String name2 = token2.substring(token2.indexOf("."), token2.indexOf(">") - 1);
                    if(name1.equals(name2)) {
                        String value1 = token1.substring(token1.indexOf("=") + 2);
                        String value2 = token2.substring(token2.indexOf("=") + 2);
                        // 如果右侧都是数值，则取校的数值作为list[i]的右侧数值，当前list[j]从list中去掉
                        if(isNumeric(value1) && isNumeric(value2)) {
                            int min = Math.min(Integer.parseInt(value1), Integer.parseInt(value2));
                            list.set(i, token1.substring(0, token1.indexOf("=") + 2) + String.valueOf(min));
                            list.remove(j);
                        }
                    }
                }
                // 如果都是=，则都去掉
                else if(label1 == 5 && label3 == 5) {
                    list.remove(j);
                    list.remove(i);
                    i--;
                    j--;
                }
                // 如果是 < 和 > ，且无交集则需要将该类型的所有包含>或者<的条件都去掉，有交集则保留
                else if(label1 == 2 && label3 == 3 || label1 == 3 && label3 == 2) {
                    String name1 = "";
                    String name2 = "";
                    if(label1 == 2 && label3 == 3) {
                        name1 = token1.substring(token1.indexOf("."), token1.indexOf("<") - 1);
                        name2 = token2.substring(token2.indexOf("."), token2.indexOf(">") - 1);
                    }
                    else {
                        name1 = token1.substring(token1.indexOf("."), token1.indexOf(">") - 1);
                        name2 = token2.substring(token2.indexOf("."), token2.indexOf("<") - 1);
                    }

                }
            }
        }
    }

    /**
     * 判断str是否是数值
     * @param str
     * @return
     */
    public boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


    /**
     * 检查n是否在nfas中
     *
     * @param nfas
     * @param n
     * @return
     */
    public boolean existenceCheck(List<NFA> nfas, NFA n) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        int nfaCount = nfas.size();
        for (int i = 0; i < nfaCount; i++) {
            if (nfas.get(i).getHashValue().equals(n.getHashValue()))
                return true;
        }
        return false;
    }

    /**
     * 获取公共时间窗口
     *
     * @param q1
     * @param q2
     */
    public void getPublicTimeWindow(NFA q1, NFA q2) {
        this.q.setTimeWindow(Math.max(q1.timeWindow, q2.timeWindow));
    }

    public NFA getQ() {
        return this.q;
    }

    public List<NFA> getNfaList() {
        return nfaList;
    }

    public List<List<NFA>> getEdgeList() {
        return edgeList;
    }
}
