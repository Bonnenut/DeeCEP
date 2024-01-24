/*
 * Copyright (c) 2011, Regents of the University of Massachusetts Amherst
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:

 *   * Redistributions of source code must retain the above copyright notice, this list of conditions
 * 		and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * 		and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the University of Massachusetts Amherst nor the names of its contributors
 * 		may be used to endorse or promote products derived from this software without specific prior written
 * 		permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sase.query;

import java.util.*;

import sase.engine.Run;
import sase.stream.Event;
import net.sourceforge.jeval.EvaluationException;

import static com.sun.xml.internal.bind.v2.schemagen.Util.equal;
import static sase.UI.CommandLineUI.tw;

/**
 * This class represents a state from NFA.
 *
 * @author haopeng
 */
public class State {
    //	 int NFAsamecount =0;
    int arraylist;

    /**
     * The line in the nfa file for this state
     */
    String nfaLine;

    /**
     * The type of the state, normal, kleene closure or negation
     */
    String stateType;

    /**
     * The event type for this state
     */
    String eventType;

    /**
     * The order of this state
     */
    int order;

    /**
     * Denoting whether this state is the first state
     */
    boolean isStart;
    boolean isConcurrentStart;


    /**
     * Denoting whether this state is the last state
     */
    boolean isEnding;
    boolean isConcurrentEnding;

    /**
     * Denoting whether this state is a kleene closure state
     */
    boolean isKleeneClosure;
    boolean isConcurrent;//Dee
    boolean isNegation;
    boolean isBeforeNegation;
    boolean isAfterNegation;

    /**
     * The edges from this state
     */
    Edge[] edges;

    /**
     * Representation for this state in the fast query format, usually 'a' for the first state, 'b' for the second state, etc.
     */
    String tag;

    /**
     * Constructs a state based on a line in the nfa file, and the order
     *
     * @param nfaLine the line in the nfa file
     * @param order   the order of this state
     */
    public State(String nfaLine, int order) {
        this.nfaLine = nfaLine;
        this.order = order;
        isStart = false;
        isEnding = false;
        isKleeneClosure = false;
        eventType = "test";
        parseNfaLine(nfaLine);
        if (this.stateType.equalsIgnoreCase("kleeneclosure")) {
            this.isKleeneClosure = true;
        }
        if (this.stateType.equalsIgnoreCase("concurrent")) {
            this.isConcurrent = true;
        }

    }

    public State(int order, String tag, String eventType, String stateType) {
        this.order = order;
        this.tag = tag;
        this.eventType = eventType;
        this.stateType = stateType;
        if (this.stateType.equalsIgnoreCase("normal")) {
            this.isKleeneClosure = false;
            this.isConcurrent = false;
            this.isNegation = false;
            //创建一个边对象数组，其大小为1
            this.edges = new Edge[1];
            //判断边的属性（0：begin；1：take；）；使用新的Edge对象初始化第一个元素（位于索引0处），并将值0作为参数传递给Edge构造函数。
            this.edges[0] = new Edge(0);
        }
        //Dee
        else if (this.stateType.equalsIgnoreCase("concurrent")) {
            //NFAsamecount++;
            this.isConcurrent = true;
            this.isKleeneClosure = false;
            this.isNegation = false;
            this.edges = new Edge[1];
            this.edges[0] = new Edge(3);
            //判断边的属性（0：begin；1：take；-1:same）
        } else if (this.stateType.equalsIgnoreCase("kleeneClosure")) {
            this.isKleeneClosure = true;
            this.isNegation = false;
            this.isConcurrent = false;
            this.edges = new Edge[3];
            for (int i = 0; i < 3; i++) {
                this.edges[i] = new Edge(i);
            }
        } else if (this.stateType.equalsIgnoreCase("negation")) {
            this.isKleeneClosure = false;
            this.isNegation = true;
            this.isConcurrent = false;
            this.edges = new Edge[1];
            this.edges[0] = new Edge(0);
        }
    }

    public State() {

    }

    /**
     * Adds a predicate to this state based on the given description.
     *
     * @param pDescription
     */
    public void addPredicate(String pDescription) {
        //System.out.println("A new predicate is added to this state: " + this.tag);
        //System.out.println(pDescription);
        // a predicate should be composed of 3 parts: left operator right
        StringTokenizer st = new StringTokenizer(pDescription);
        int size = st.countTokens();
        String left = st.nextToken();

        int edgeNumber = this.parseEdgeNumber(left);
        //System.out.println("left=" + left);

        String right = null;
        while (st.hasMoreTokens()) {
            right = st.nextToken();
        }

        //System.out.println("right=" + right);
        String newLeft = this.replaceLeftStateNumber(left);
        String newRight = this.replaceRightStateNumber(right);


        String p = pDescription.replace(left, newLeft);
        p = p.replace(right, newRight);
        this.edges[edgeNumber].addPredicate(p);
        //System.out.println("predicate after parsing is:" + p);


    }

    /**
     * Used to replace the state number of the left operand
     *
     * @param original
     * @return the replaced string
     */
    public String replaceLeftStateNumber(String original) {
        int dotPosition = original.indexOf('.');
        return original.substring(dotPosition + 1, original.length());
    }

    /**
     * Used to replace the state number of the right operand
     *
     * @param original
     * @return the replaced string
     */
    public String replaceRightStateNumber(String original) {
        if (original.contains("(")) {
            String innerPart = this.parseRightStateNumber(original.substring(original.indexOf('(') + 1, original.indexOf(')')));
            String outterPart = original.substring(0, original.indexOf('('));
            return outterPart + "(" + innerPart + ")";
        } else {
            return this.parseRightStateNumber(original);
        }

    }

    /**
     * Parses the state number of the right operand
     *
     * @param original
     * @return the parsed state number
     */
    public String parseRightStateNumber(String original) {
        if (!original.contains(".")) {
            return original;
        } else {
            if (original.contains("[..i-1]")) {
                char initial = original.charAt(0);
                String stateNamePart = original.substring(0, original.indexOf(']') + 1);
                return original.replace(stateNamePart, "$" + (initial - 'a' + 1));
            }
            String stateName = original.substring(0, original.indexOf('.'));
            if (stateName.length() == 1) {
                if (stateName.equalsIgnoreCase(this.tag)) {
                    return original.substring(original.indexOf('.'));
                } else {
                    return original.replace(stateName, "$" + (stateName.charAt(0) - 'a' + 1));
                }
            } else {
                return "$previous" + original.substring(original.indexOf('.'));
            }
        }

    }

    /**
     * Judges the edge type, "take" or "begin"
     *
     * @param predicateLeft
     * @return 1 for "take", 0 for "begin"
     */
    public int parseEdgeNumber(String predicateLeft) {
        if (predicateLeft.contains("[i]")) {
            //"take" edge
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Parses a line in the nfa file, e.g.: State=1 & type = normal & eventtype = c | edgetype = begin & price < 100
     *
     * @param nfaLine the line in the nfa file
     */
    void parseNfaLine(String nfaLine) {
        this.nfaLine = nfaLine;
        StringTokenizer st = new StringTokenizer(nfaLine, "|"); //parse the state and edges
        int count = 0;
        int size = st.countTokens();//count the size of edges
        edges = new Edge[size - 1];// parse the edge
        while (st.hasMoreTokens()) {
            if (count == 0) {// parse the part describing the state
                parseState(st.nextToken().trim());
                count++;
            } else {
                edges[count - 1] = new Edge(st.nextToken().trim());
                count++;
            }
        }
    }

    /**
     * Parses the description for state, e.g.: state = 1, type = normal/kleeneclosure/negation
     *
     * @param stateLine the description for this state
     */
    public void parseState(String stateLine) {
        StringTokenizer st = new StringTokenizer(stateLine, "&");
        while (st.hasMoreTokens()) {
            parseEquation(st.nextToken().trim());
        }
    }

    /**
     * Parses the formulas in the query, e.g.: price > 100
     *
     * @param equation the formula string
     */
    public void parseEquation(String equation) {// parse the state/state type, eventtype
        StringTokenizer st = new StringTokenizer(equation, "=");
        String left = st.nextToken().trim();
        String right = st.nextToken().trim();
        if (left.equalsIgnoreCase("state")) {
            this.order = Integer.parseInt(right);
        } else if (left.equalsIgnoreCase("type")) {
            this.stateType = right;
        } else if (left.equalsIgnoreCase("eventtype")) {
            this.eventType = right;
        }
    }

    /**
     * Self description
     */
    @Override
    public String toString() {
        String temp = "";
        if (isStart) {
            temp += " I am a starting state";
        }
        if (isEnding) {
            temp += " I am a ending state";
        }
        if (isKleeneClosure) {
            temp += " I am a kleene closure state";
        }
        if (isConcurrentStart) {
            temp += " 我是并发事件的开始事件";
        }
        if (isConcurrentEnding) {
            temp += " 我是并发事件的结束事件";
        }
        temp += " My state type is: " + this.stateType;
        temp += "\n my description file = " + this.nfaLine;
        return "This is the " + order + " state, requiring events of " + eventType
                + " event type, " + temp;
    }

    /**
     * Dee
     * combinations：存储找到的所有符合条件的排列组合
     * List<Event> 与 Q 匹配版本
     *
     * @param eventGroups 事件列表
     * @param Q           匹配条件
     * @return combinations 找到的所有排列组合
     */
//    public List<List<Event>> swipeBufferFindCombinations(List<Event> eventGroups, String Q) {
    public List<List<Event>> swipeBufferFindCombinations(List<Event> eventGroups, String Q, Run r) {
        if (r != null) {
            //检查匹配中没有之前匹配过的运行
            for (int i : r.getEventIds()) {
                for (int j = 0; j < eventGroups.size(); j++) {
                    if (eventGroups.get(j).getId() == i) {
                        eventGroups.remove(j);
                    }
                }

            }
        }
        Q = Q.replace("^", "");
        // combinations：存储找到的所有符合条件的排列组合
        List<List<Event>> combinations = new ArrayList<>();
        // 调用辅助方法开始搜索排列组合
        swipeBufferFindCombinationsHelper(eventGroups, Q, 0, new ArrayList<>(), combinations);
        // 返回找到的所有排列组合
        return combinations;
    }

    /**
     * 递归2：辅助方法，用于递归搜索匹配的排列组合
     *
     * @param eventGroups        事件列表
     * @param Q                  匹配条件
     * @param index              当前匹配的 Q 中的位置
     * @param currentCombination 当前已匹配的组合
     * @param combinations       存储找到的所有符合条件的排列组合
     */
    private static void swipeBufferFindCombinationsHelper(List<Event> eventGroups, String Q, int index, List<Event> currentCombination, List<List<Event>> combinations) {
        // 如果已经匹配完Q中的所有事件类型，将当前组合添加到结果集合中
        if (index == Q.length()) {
            // 在添加到结果集合之前，对当前组合按照时间戳排序
            currentCombination.sort(Comparator.comparingInt(Event::getTimestamp));
            combinations.add(new ArrayList<>(currentCombination));
            return;
        }

        // 获取Q中当前位置指定的事件类型
        String eventType = String.valueOf(Q.charAt(index));

        // 遍历事件列表
        for (Event event : eventGroups) {
            // 检查事件类型是否与Q中指定的类型匹配
            if (equal(event.getEventType(), eventType)) {
                if (isValidTimeConstraint(currentCombination, event, tw)) {
                    // 将当前事件添加到组合中
                    currentCombination.add(event);
                    // 递归调用方法，继续搜索下一个事件类型
                    swipeBufferFindCombinationsHelper(eventGroups, Q, index + 1, currentCombination, combinations);
                    // 回溯：移除刚才添加的事件，尝试其他可能的事件
                    currentCombination.remove(currentCombination.size() - 1);
                }
            }
        }
    }
//    private static void swipeBufferFindCombinationsHelper(List<Event> eventGroups, String Q, int index, List<Event> currentCombination, List<List<Event>> combinations) {
//        if (index == Q.length()) {
//            currentCombination.sort(Comparator.comparingInt(Event::getTimestamp));
//            combinations.add(new ArrayList<>(currentCombination));
//            return;
//        }
//
//        String eventType = String.valueOf(Q.charAt(index));
//        // 创建一个集合来存储已经添加到组合中的事件类型
//        Set<String> usedEventTypes = new HashSet<>();
//        for (Event event : currentCombination) {
//            usedEventTypes.add(event.getEventType());
//        }
//
//        for (Event event : eventGroups) {
//            if (equal(event.getEventType(), eventType) && !usedEventTypes.contains(eventType)) {
//                currentCombination.add(event);
//                swipeBufferFindCombinationsHelper(eventGroups, Q, index + 1, currentCombination, combinations);
//                currentCombination.remove(currentCombination.size() - 1);
//                // 添加到 usedEventTypes 集合中，避免重复添加
//                usedEventTypes.add(eventType);
//            }
//        }
//    }
private static boolean isValidTimeConstraint(List<Event> currentCombination, Event newEvent, int tw) {
    if (!currentCombination.isEmpty()) {
        Event lastEvent = currentCombination.get(currentCombination.size() - 1);
        int timeDifference = Math.abs(newEvent.getTimestamp() - lastEvent.getTimestamp());
        return timeDifference <= tw;
    }
    return true; // If currentCombination is empty, time constraint is valid
}
    /**
     * Dee
     * combinations：存储找到的所有符合条件的排列组合
     * Map<String, List<Event>>与Q匹配版本
     *
     * @param eventGroups
     * @return
     */
    // 寻找匹配的排列组合的方法
    // 递归1：公共接口
    public List<List<Event>> findCombinations(Map<String, List<Event>> eventGroups, String Q) {
        Q = Q.replace("^", "");
        // combinations：存储找到的所有符合条件的排列组合
        List<List<Event>> combinations = new ArrayList<>();
        // 调用辅助方法开始搜索排列组合
        findCombinationsHelper(eventGroups, Q, 0, new ArrayList<>(), combinations);
        // 返回找到的所有排列组合
        return combinations;
    }

    // 递归2：辅助方法，用于递归搜索匹配的排列组合
    private static void findCombinationsHelper(Map<String, List<Event>> eventGroups, String Q, int index, List<Event> currentCombination, List<List<Event>> combinations) {
        // 如果已经匹配完 Q 中的所有事件类型，将当前组合添加到结果集合中
        if (index == Q.length()) {
            //combinations：存储找到的所有符合条件的排列组合
            combinations.add(new ArrayList<>(currentCombination));
            return;
        }

        // 获取当前 Q 中指定位置的事件类型
        char eventType = Q.charAt(index);
        // 检查事件组中是否包含当前事件类型
        //并发事件组中是否包含Q[0]
        if (eventGroups.containsKey(String.valueOf(eventType))) {
            // 获取当前事件类型下的所有事件
            List<Event> events = eventGroups.get(String.valueOf(eventType));

            // 遍历events
            for (Event event : events) {
                // 剪枝：如果当前事件已经在组合中，跳过
                if (!currentCombination.contains(event)) {
                    // 将当前事件添加到组合中
                    currentCombination.add(event);
                    // 递归调用，继续搜索下一个事件类型
                    findCombinationsHelper(eventGroups, Q, index + 1, currentCombination, combinations);
                    // 回溯：将添加的事件移除，尝试下一个事件
                    currentCombination.remove(currentCombination.size() - 1);
                }
            }
        }
    }

    /**
     * 函数注释：判断是否可以启动一个事件。
     *
     * @param eventGroups：一个时间戳下的所有事件，以事件类型为 key的hashmap
     * @return 如果事件类型匹配并且第一个边的谓词评估为 true，则返回 true；否则返回 false。
     * @throws EvaluationException 在谓词评估过程中发生异常时抛出 EvaluationException 异常。
     * @author Dee
     */
    public List<List<Event>> swipeBufferCanStartWithEventByDeeCEP(List<Event> eventGroups) throws EvaluationException {
        // 条件注释：检查事件状态是否为 "normal"，且事件类型是否与定义的 eventType 匹配

        //如果canStart事件的长度=1，也就是说是normal事件
        //那么开始判断事件e的事件类型等不等于canStart的事件类型
        //不等于返回false
        //else：也就是说canStart事件是concurrent事件
        //那么查看e的事件类型是否在被包含在查询中，包含则继续进行，不包含则立刻返回false
        List<List<Event>> combinations = null;
        if (this.getEventType().length() == 1) {
//            // 如果canStart事件的长度=1，也就是说是normal事件
//            if (!e.getEventType().equalsIgnoreCase(this.eventType)) {
//                // 如果事件类型与定义的 eventType 不匹配，返回 false
//                return false;
//            }
        } else {//canStart事件是concurrent事件，比如是A^B^
            Run r = null;
            //递归+剪枝与Q集合对比
            combinations = swipeBufferFindCombinations(eventGroups, this.eventType, r);
            //combinations为空，说明没找到匹配
        }

        return combinations; // 返回 true，表示事件符合启动条件
    }

    /**
     * 函数注释：判断是否可以启动一个事件。
     *
     * @param eventGroups：一个时间戳下的所有事件，以事件类型为 key的hashmap
     * @return 如果事件类型匹配并且第一个边的谓词评估为 true，则返回 true；否则返回 false。
     * @throws EvaluationException 在谓词评估过程中发生异常时抛出 EvaluationException 异常。
     * @author Dee
     */
    public List<List<Event>> canStartWithEventByDeeCEP(Map<String, List<Event>> eventGroups) throws EvaluationException {
        // 条件注释：检查事件状态是否为 "normal"，且事件类型是否与定义的 eventType 匹配

        //如果canStart事件的长度=1，也就是说是normal事件
        //那么开始判断事件e的事件类型等不等于canStart的事件类型
        //不等于返回false
        //else：也就是说canStart事件是concurrent事件
        //那么查看e的事件类型是否在被包含在查询中，包含则继续进行，不包含则立刻返回false
        List<List<Event>> combinations = null;
        if (this.getEventType().length() == 1) {
//            // 如果canStart事件的长度=1，也就是说是normal事件
//            if (!e.getEventType().equalsIgnoreCase(this.eventType)) {
//                // 如果事件类型与定义的 eventType 不匹配，返回 false
//                return false;
//            }
        } else {//canStart事件是concurrent事件，比如是A^B^
            //递归+剪枝与Q集合对比
            combinations = findCombinations(eventGroups, this.eventType);
            //combinations为空，说明没找到匹配
        }

        return combinations; // 返回 true，表示事件符合启动条件
    }


    /**
     * 函数注释：判断是否可以启动一个事件。
     *
     * @param e 事件对象，包含事件类型等信息。
     * @return 如果事件类型匹配并且第一个边的谓词评估为 true，则返回 true；否则返回 false。
     * @throws EvaluationException 在谓词评估过程中发生异常时抛出 EvaluationException 异常。
     */
    public boolean canStartWithEvent(Event e) throws EvaluationException {

        if (!e.getEventType().equalsIgnoreCase(this.eventType)) {
            // 如果事件类型与定义的 eventType 不匹配，返回 false
            return false;
        }

        // 条件注释：使用第一个边的谓词评估事件
        if (this.edges[0].evaluatePredicate(e, e)) {
            // 如果第一个边的谓词评估为 true，执行以下逻辑
            return true; // 返回 true，表示事件符合启动条件
        }

// 如果前面的条件都不满足，返回 false，表示事件不符合启动条件
        return false;

    }


    /**
     * @return the eventType
     */
    public String getEventType() {
        return eventType;
    }


    /**
     * @param eventType the eventType to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }


    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * @return the isStart
     */
    public boolean isStart() {
        return isStart;
    }

    /**
     * @return the isConcurrentStart
     */
    public boolean isConcurrentStart() {
        return isConcurrentStart;
    }


    /**
     * @param isConcurrentStart the isStart to set
     */
    public void setConcurrentStart(boolean isConcurrentStart) {
        this.isConcurrentStart = isConcurrentStart;
    }

    /**
     * @param isStart the isStart to set
     */
    public void setStart(boolean isStart) {
        this.isStart = isStart;
    }

    /**
     * @return the isEnding
     */
    public boolean isEnding() {
        return isEnding;
    }

    /**
     * @return the isConcurrentEnding
     */
    public boolean isConcurrentEnding() {
        return isConcurrentEnding;
    }

    /**
     * @param isEnding the isEnding to set
     */
    public void setEnding(boolean isEnding) {
        this.isEnding = isEnding;
    }


    /**
     * @param isConcurrentEnding the isEnding to set
     */
    public void setConcurrentEnding(boolean isConcurrentEnding) {
        this.isConcurrentEnding = isConcurrentEnding;
    }

    /**
     * @return the isKleeneClosure
     */
    public boolean isKleeneClosure() {
        return isKleeneClosure;
    }


    /**
     * @param isKleeneClosure the isKleeneClosure to set
     */
    public void setKleeneClosure(boolean isKleeneClosure) {
        this.isKleeneClosure = isKleeneClosure;
    }

    /**
     * @return the nfaLine
     */
    public String getNfaLine() {
        return nfaLine;
    }

    /**
     * @param nfaLine the nfaLine to set
     */
    public void setNfaLine(String nfaLine) {
        this.nfaLine = nfaLine;
    }

    /**
     * @return the stateType
     */
    public String getStateType() {
        return stateType;
    }

    /**
     * @param stateType the stateType to set
     */
    public void setStateType(String stateType) {
        this.stateType = stateType;
    }

    /**
     * @return the edges
     */
    public Edge[] getEdges() {
        return edges;
    }

    public Edge getEdges(int order) {
        return edges[order];
    }

    /**
     * @param edges the edges to set
     */
    public void setEdges(Edge[] edges) {
        this.edges = edges;
    }

    /**
     * @return the isNegation
     */
    public boolean isNegation() {
        return isNegation;
    }

    /**
     * @param isNegation the isNegation to set
     */
    public void setNegation(boolean isNegation) {
        this.isNegation = isNegation;
    }

    /**
     * @return the isBeforeNegation
     */
    public boolean isBeforeNegation() {
        return isBeforeNegation;
    }

    /**
     * @param isBeforeNegation the isBeforeNegation to set
     */
    public void setBeforeNegation(boolean isBeforeNegation) {
        this.isBeforeNegation = isBeforeNegation;
    }

    /**
     * @return the isAfterNegation
     */
    public boolean isAfterNegation() {
        return isAfterNegation;
    }

    /**
     * @param isAfterNegation the isAfterNegation to set
     */
    public void setAfterNegation(boolean isAfterNegation) {
        this.isAfterNegation = isAfterNegation;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }


}
