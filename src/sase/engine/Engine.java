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
package sase.engine;

import java.util.stream.Collectors;
import java.util.*;

import net.sourceforge.jeval.EvaluationException;


import sase.query.Edge;
import sase.query.NFA;
import sase.query.State;
import sase.stream.Event;
import sase.stream.Stream;

//import static java.util.stream.Nodes.collect;
import static sase.engine.Profiling.numberOfRuns;


/**
 * This is the processing engine.
 *
 * @author haopeng
 */
public class Engine {

    /**
     * The input stream
     */
    Stream input;
    /**
     * The event buffer
     */
    EventBuffer buffer;
    /**
     * The event sameBuffer
     *
     * @author Dee
     */
    EventBuffer sameBuffer;
    int recordTimeStamp = 0;
    int stampCount = 1;
    int[] cloner;
    boolean isSame;
    ArrayList<Run> SameStampMatch;//用来临时存同时刻之间产生的部分匹配
    int nowstamp = 0;
    /**
     * The nfa representing the query
     */
    NFA nfa;
    /**
     * The run pool, which is used to reuse the run data structure.
     */
    RunPool engineRunController;
    /**
     * The active runs in memory
     */
    public ArrayList<Run> activeRuns;
    public ArrayList<Run> cloneactiveRuns;//克隆数组


    HashMap<Integer, ArrayList<Run>> activeRunsByPartition;
    /**
     * The runs which can be removed from the active runs.
     */
    ArrayList<Run> toDeleteRuns;
    /**
     * The matches
     */
    MatchController matches;

    /**
     * The buffered events for the negation components
     */
    ArrayList<Event> negationEvents;

    HashMap<Integer, ArrayList<Event>> negationEventsByPartition;

    /**
     * The default constructor.
     */

    public Engine() {
        buffer = new EventBuffer();
        sameBuffer = new EventBuffer();//Dee
        engineRunController = new RunPool();
        this.activeRuns = new ArrayList<Run>();
        this.toDeleteRuns = new ArrayList<Run>();
        this.matches = new MatchController();
        Profiling.resetProfiling();

    }

    /**
     * This method initializes the engine.
     */

    public void initialize() {
        input = null;
        buffer = new EventBuffer();
        sameBuffer = new EventBuffer();//Dee
        engineRunController = new RunPool();
        this.activeRuns = new ArrayList<Run>();
        this.activeRunsByPartition = new HashMap<Integer, ArrayList<Run>>();
        this.toDeleteRuns = new ArrayList<Run>();
        this.matches = new MatchController();
        Profiling.resetProfiling();

    }

    /**
     * This method is used to warm up the engine.
     *
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */
    public void warmUp() throws CloneNotSupportedException, EvaluationException {
        this.runEngine();
        buffer = new EventBuffer();
        sameBuffer = new EventBuffer();
        engineRunController = new RunPool();
        this.activeRuns = new ArrayList<Run>();
        this.toDeleteRuns = new ArrayList<Run>();
        this.matches = new MatchController();
        Profiling.resetProfiling();


    }

    /**
     * This is the main run logic method
     */

    public void runEngine() throws CloneNotSupportedException, EvaluationException {
        ConfigFlags.timeWindow = this.nfa.getTimeWindow();
        ConfigFlags.timeStamp = this.nfa.getTimeStamp();
        ConfigFlags.sequenceLength = this.nfa.getSize();
        ConfigFlags.selectionStrategy = this.nfa.getSelectionStrategy();
        ConfigFlags.hasPartitionAttribute = this.nfa.isHasPartitionAttribute();
//		ConfigFlags.processConcurrentEventStream = this.nfa.isHasPartitionAttribute();//Dee
        ConfigFlags.hasNegation = this.nfa.isHasNegation();
        if (ConfigFlags.hasNegation) {
            this.runNegationEngine();
        } else if (ConfigFlags.selectionStrategy.equalsIgnoreCase("skip-till-any-match")) {
            this.runSkipTillAnyEngine();
        } else if (ConfigFlags.selectionStrategy.equalsIgnoreCase("skip-till-next-match")) {
            this.runSkipTillNextEngine();
        } else if (ConfigFlags.selectionStrategy.equalsIgnoreCase("partition-contiguity")) {
            this.runPartitionContiguityEngine();
        }


    }

    /**
     * The main method for skip-till-any-match
     *
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */
    public void runSkipTillAnyEngine() throws CloneNotSupportedException, EvaluationException {
        if (!ConfigFlags.processConcurrentEventStream) {
            Event e = null;
            long currentTime = 0;
            while ((e = this.input.popEvent()) != null) {// evaluate event one by one
                currentTime = System.nanoTime();
                this.evaluateRunsForSkipTillAny(e);// evaluate existing runs
                if (this.toDeleteRuns.size() > 0) {
                    this.cleanRuns();
                }
                this.createNewRun(e);// create new run starting with this event if possible


                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;
                //记录当前时间戳
            }
        }

        //分区 没用
        if (ConfigFlags.hasPartitionAttribute) {
            ConfigFlags.partitionAttribute = this.nfa.getPartitionAttribute();

            int isSameCount = 0;//同时刻事件个数
            this.activeRunsByPartition = new HashMap<Integer, ArrayList<Run>>();
            Event e = null;
            long currentTime = 0;
            while ((e = this.input.popEvent()) != null) {// evaluate event one by one
//				//入口
//				//标记同时刻事件(isSame) + 获得同时刻事件个数
                //this.sameEventprocessing(e);
                currentTime = System.nanoTime();
                //[1]部分匹配
                this.evaluateRunsByPartitionForSkipTillAny(e);// evaluate existing runs
                //[2]清理匹配
                if (this.toDeleteRuns.size() > 0) {
                    this.cleanRunsByPartition();
                }
                //[3]创建新运行分区
                this.createNewRunByPartition(e);// create new run starting with this event if possible


                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;

            }
        }
    }

    /**
     * 这个方法用于按时间戳对事件进行分组。
     *
     * @param batchEvents 要分组的事件列表
     *                    外层 Map（键：时间戳）：
     *                    键：Integer，表示事件的时间戳。
     *                    值：另一个 Map。
     *                    <p>
     *                    内层 Map（键：事件类型）：
     *                    键：String，表示事件的类型。
     *                    值：List<ABCEvent>，表示具有相同时间戳和事件类型的一组事件。
     * @author Dee
     */
    public Map<Integer, Map<String, List<Event>>> groupEventsByTimestamp(List<Event> batchEvents) {
        return batchEvents.stream()
                .collect(Collectors.groupingBy(
                        Event::getTimestamp,
                        Collectors.groupingBy(Event::getEventType)
                ));
    }


    /**
     * 同时刻事件处理程序
     *
     * @param e
     */
    public void sameEventprocessing(Event e) {
        //第一个事件会因记录时间戳在后，比对在先，而不被记录。所以StampCount从1开始加，一旦识别到同时刻就=2.
        if (recordTimeStamp == e.getTimestamp()) {
            //当前事件的时间戳与前一个事件的时间戳相同
            // 计数+1
            stampCount++;
            //标记当前事件为isSame
            this.isSame = true;

        } else {
            //当前事件时间戳与前一个事件不同，stampcount复原
            stampCount = 1;
            //记录当前事件时间戳
            recordTimeStamp = e.getTimestamp();
            //取消标记当前事件为isSame
            this.isSame = false;
        }
    }

    int lastid = 0;
    int lasttimestamp = 0;

    /**
     * 这是 skip-till-next-match 的主要方法。
     * 通过这个方法，系统会逐个评估事件，进行部分匹配，清理不再需要的运行，以及创建新的运行。
     *
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */
    public void runSkipTillNextEngine() throws CloneNotSupportedException, EvaluationException {
        // arg[3]：运行传统sase
        if (!ConfigFlags.processConcurrentEventStream) {
            Event e = null;
            long currentTime = 0;
            // 逐个评估事件
            while ((e = this.input.popEvent()) != null) {
                currentTime = System.nanoTime();

                // [1] 部分匹配
                this.evaluateRunsForSkipTillNext(e);
                // [2]如果有需要删除的运行，执行清理操作
                if (this.toDeleteRuns.size() > 0) {
                    this.cleanRuns();
                }
                // [3]创建新的运行，以当前事件为起点
                this.createNewRun(e);
                // 更新总运行时间和事件数量
                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;
            }
        }
        /**
         * 处理含有并发事件的事件流
         * DeeCEP主要循环入口
         * pop事件进来，存到batchEvents中
         * 存够batchSize了，开始分组
         * 按时间戳对事件进行分组：groupEventsByTimestamp
         *	给分好组的事件包，排队送入NFA，还没写
         */
        if (ConfigFlags.processConcurrentEventStream) {
            Event e = null;
            //每次从流中取事件的数量，以后改成能在环境中输入
            int batchSize = 30;
            // 计数器，用于追踪每个批次处理的事件数量
            int eventsProcessed = 0;
            // batchEvents用于存储当前批次的事件
            List<Event> batchEvents = new ArrayList<>();
            Map<Integer, Map<String, List<Event>>> tsGroups = null; // 在这里声明 tsGroups
            while ((e = this.input.popEvent()) != null) {// evaluate event one by one
                // 将事件添加到当前批次
                batchEvents.add(e);
                eventsProcessed++; // 先自增
                if (eventsProcessed == batchSize) {
                    //按时间戳对事件进行分组
                    tsGroups = groupEventsByTimestamp(batchEvents);
                    //给分好组的事件包，排队送入NFA
                    eventGoNFA(tsGroups);
                    eventsProcessed = 0; // 重置计数器
                    batchEvents.clear(); // 清空当前批次的事件列表
                }
            }
        }
        //分区 没用
        // 如果存在分区属性
        if (ConfigFlags.hasPartitionAttribute) {
            ConfigFlags.partitionAttribute = this.nfa.getPartitionAttribute();
            this.activeRunsByPartition = new HashMap<Integer, ArrayList<Run>>();
            Event e = null;
            long currentTime = 0;
            // 逐个评估事件
            while ((e = this.input.popEvent()) != null) {
                currentTime = System.nanoTime();
                // 评估现有运行状况
                this.evaluateRunsByPartitionForSkipTillNext(e);
                // 如果有需要删除的运行，执行清理操作
                if (this.toDeleteRuns.size() > 0) {
                    this.cleanRunsByPartition();
                }
                // 创建新的运行，以当前事件为起点
                this.createNewRunByPartition(e);
                // 更新总运行时间和事件数量
                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;
            }
        }
    }

    /**
     * @author Dee
     * [1]部分匹配
     * [2]清理缓存
     * [3]创建新运行
     */
    public void eventGoNFA(Map<Integer, Map<String, List<Event>>> tsGroups) throws EvaluationException, CloneNotSupportedException {
//		(Map<时间戳, Map<事件类型, List<ABCEvent>>>)
//		Map.Entry<Integer, Map<String, List<Event>>> timestampEntry:
//		定义了一个新的变量 timestampEntry，它表示哈希表中的一个条目。在每次循环迭代中，timestampEntry 将被设置为当前迭代的条目。
        Map.Entry<String, List<Event>> eventTypeEntry;
        // 遍历时间戳组
        for (Map.Entry<Integer, Map<String, List<Event>>> timestampEntry : tsGroups.entrySet()) {
            // 获取当前时间戳
            int timestamp = timestampEntry.getKey();
            // 获取当前时间戳对应的事件类型分组映射
            Map<String, List<Event>> eventGroups = timestampEntry.getValue();

            // 判断本时间戳组 是否有多个事件
            if (eventGroups.size() > 1) {// [3] concurrent事件 创建新运行
                // [3]创建新的运行，以当前事件为起点
                //eventGroups是一个哈希map，key = 事件类型，value = ABCEvent
                //eventGroups是一个时间戳下的事件包
                createNewRunByDeeCEP(eventGroups);
                // [1] 部分匹配
                evaluateRunsForSkipTillNextByDeeCEP(eventGroups);
            } else {// [3] normal事件 创建新运行
                // 此时只有一个键值对
                for (Map.Entry<String, List<Event>> innerEntry : eventGroups.entrySet()) {
                    // 获取事件类型和事件列表
                    String eventType = innerEntry.getKey();
                    List<Event> events = innerEntry.getValue();
                    //events 只包含一个事件，直接取出该事件
                    Event e = events.get(0);
                    // [3]创建新的运行，以当前事件为起点
                    this.createNewRun(e);

                    // [1] 部分匹配
                    this.evaluateRunsForSkipTillNextByDeeCEP(eventGroups);
                }
            }

        }


        // [2]如果有需要删除的运行，执行清理操作
        if (this.toDeleteRuns.size() > 0) {
            this.cleanRuns();
        }
    }

    /**
     * This method is called when the query uses the partition-contiguity selection strategy
     *
     * @throws CloneNotSupportedException
     */

    public void runPartitionContiguityEngine() throws EvaluationException, CloneNotSupportedException {
        // ConfigFlags类用于设置引擎的参数
        ConfigFlags.partitionAttribute = this.nfa.getPartitionAttribute();
        ConfigFlags.hasPartitionAttribute = true;
        //新建哈希map主动分区运行：activeRunsByPartition
        this.activeRunsByPartition = new HashMap<Integer, ArrayList<Run>>();
        ConfigFlags.timeWindow = this.nfa.getTimeWindow();
        ConfigFlags.timeStamp = this.nfa.getTimeStamp();//dee

        ConfigFlags.sequenceLength = this.nfa.getSize();
        ConfigFlags.selectionStrategy = this.nfa.getSelectionStrategy();

        Event e = null;
        long currentTime = 0;
        while ((e = this.input.popEvent()) != null) {
            currentTime = System.nanoTime();

            this.evaluateRunsForPartitionContiguity(e);
            if (this.toDeleteRuns.size() > 0) {
                this.cleanRunsByPartition();
            }
            //所有B事件都能进入，筛选出value = 450的B事件，事件进入activeRunsPartition哈希表内
            this.createNewRunByPartition(e);
            Profiling.totalRunTime += (System.nanoTime() - currentTime);
            Profiling.numberOfEvents += 1;
        }
    }

    /**
     * The main method when there is a negation component in the query
     *
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */
    public void runNegationEngine() throws CloneNotSupportedException, EvaluationException {
        if (!ConfigFlags.hasPartitionAttribute) {
            Event e = null;
            long currentTime = 0;
            this.negationEvents = new ArrayList<Event>();
            while ((e = this.input.popEvent()) != null) {// evaluate event one by one
                currentTime = System.nanoTime();
                if (this.checkNegation(e)) {
                    this.negationEvents.add(e);
                } else {
                    this.evaluateRunsForNegation(e);// evaluate existing runs
                    if (this.toDeleteRuns.size() > 0) {
                        this.cleanRuns();
                    }
                    this.createNewRun(e);// create new run starting with this event if possible
                }

                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;

            }
        }

        if (ConfigFlags.hasPartitionAttribute) {
            ConfigFlags.partitionAttribute = this.nfa.getPartitionAttribute();
            this.activeRunsByPartition = new HashMap<Integer, ArrayList<Run>>();
            this.negationEventsByPartition = new HashMap<Integer, ArrayList<Event>>();

            Event e = null;
            long currentTime = 0;
            while ((e = this.input.popEvent()) != null) {// evaluate event one by one
                currentTime = System.nanoTime();
                if (this.checkNegation(e)) {
                    this.indexNegationByPartition(e);
                } else {
                    this.evaluateRunsByPartitionForNegation(e);// evaluate existing runs
                    if (this.toDeleteRuns.size() > 0) {
                        this.cleanRunsByPartition();
                    }
                    this.createNewRunByPartition(e);// create new run starting with this event if possible
                }

                Profiling.totalRunTime += (System.nanoTime() - currentTime);
                Profiling.numberOfEvents += 1;

            }
        }
    }

    /**
     * This method will iterate all existing runs for the current event, for skip-till-any-match.
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */

    public void evaluateRunsForSkipTillAny(Event e) throws CloneNotSupportedException, EvaluationException {
        int size = this.activeRuns.size();
        for (int i = 0; i < size; i++) {
            Run r = this.activeRuns.get(i);
            if (r.isFull()) {
                continue;
            }
            this.evaluateEventForSkipTillAny(e, r);
        }
    }

    /**
     * This method will iterate all existing runs for the current event, for .
     * 遍历当前事件的所有现有运行
     *
     * @param eventGroups 当前正在评估的事件包。
     * @throws CloneNotSupportedException 如果对某个对象不支持克隆操作。
     * @throws EvaluationException        如果在评估过程中发生错误。
     */
//    int activerunslasttimestamp = 0;
    //传进来的：eventGroups
    // 是一个哈希map，key = 事件类型，value = ABCEvent
    // eventGroups是一个时间戳下的事件包
    public void evaluateRunsForSkipTillNextByDeeCEP(Map<String, List<Event>> eventGroups) throws CloneNotSupportedException, EvaluationException {
        // 初始化一个变量以存储 activeRuns 列表的大小。
        int size = this.activeRuns.size();
        // 遍历每个活动运行。
        for (int i = 0; i < size; i++) {
            // 从 activeRuns 列表获取当前运行。
            Run r = this.activeRuns.get(i);
            // 检查当前运行是否已满，如果是，则跳过当前迭代，继续下一个活动运行。
            if (r.isFull()) {
                continue;
            }
            // 获取当前运行的最后一个事件的时间戳。
            lasttimestamp = r.getLastNEventTimeStamp();
            //谓词检查，一个时间戳下面的 事件包 列表中的部分匹配
            this.evaluateEventForSkipTillNextByDeeCEPByDeeCEP(eventGroups, r);
        }
    }


    /**
     * This method will iterate all existing runs for the current event, for .
     * 该方法用于遍历当前事件的所有现有运行，用于skip-till-next-match的逻辑。
     *
     * @param e 当前正在评估的事件。
     * @throws CloneNotSupportedException 如果对某个对象不支持克隆操作。
     * @throws EvaluationException        如果在评估过程中发生错误。
     */
    int activerunslasttimestamp = 0;

    public void evaluateRunsForSkipTillNext(Event e) throws CloneNotSupportedException, EvaluationException {
        // 初始化一个变量以存储 activeRuns 列表的大小。
        int size = this.activeRuns.size();
        // 遍历每个活动运行。
        for (int i = 0; i < size; i++) {
            // 从 activeRuns 列表获取当前运行。
            Run r = this.activeRuns.get(i);
            // 检查当前运行是否已满，如果是，则跳过当前迭代，继续下一个活动运行。
            if (r.isFull()) {
                continue;
            }
            // 获取当前运行的最后一个事件的时间戳。
            lasttimestamp = r.getLastNEventTimeStamp();
            this.evaluateEventForSkipTillNext(e, r);
        }
    }


    /**
     * This method will iterate all existing runs for the current event, for queries with a negation component.
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     * @throws EvaluationException
     */
    public void evaluateRunsForNegation(Event e) throws CloneNotSupportedException, EvaluationException {
        int size = this.activeRuns.size();
        for (int i = 0; i < size; i++) {
            Run r = this.activeRuns.get(i);
            if (r.isFull()) {
                continue;
            }
            this.evaluateEventForNegation(e, r);
        }
    }


    /**
     * This method will iterate runs in the same partition for the current event, for skip-till-any-match
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     */    //[1]部分匹配
    //
    //
    //
    public void evaluateRunsByPartitionForSkipTillAny(Event e) throws CloneNotSupportedException {
        //1)过滤同symbol事件
        int key = e.getAttributeByName(ConfigFlags.partitionAttribute);
        if (this.activeRunsByPartition.containsKey(key)) {
            //2)新建partitionedRuns数组
            //存：现存所有部分匹配
            ArrayList<Run> partitionedRuns = this.activeRunsByPartition.get(key);
            //3)遍历现有的部分匹配，检查是否有isFull的；
            //→有：
            //→没有：1. 检查此事件能让状态机进入下一个状态【0：begin；1：take；2：proceed】
            int size = partitionedRuns.size();
            for (int i = 0; i < size; i++) {
                Run r = partitionedRuns.get(i);
                if (r.isFull()) {
                    continue;
                }
                if (!isSame) {//不是同时刻事件才能与已存在的部分匹配进行匹配。
                    this.evaluateEventOptimizedForSkipTillAny(e, r);
                }

            }
        }
    }

    /**
     * This method will iterate runs in the same partition for the current event, for skip-till-next-match
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     */
    public void evaluateRunsByPartitionForSkipTillNext(Event e) throws CloneNotSupportedException {
        int key = e.getAttributeByName(ConfigFlags.partitionAttribute);
        if (this.activeRunsByPartition.containsKey(key)) {
            //数组每次都更新，所以每次都加一遍
            ArrayList<Run> partitionedRuns = this.activeRunsByPartition.get(key);
            int size = partitionedRuns.size();
            for (int i = 0; i < size; i++) {
                Run r = partitionedRuns.get(i);
                if (r.isFull()) {
                    continue;
                }
                this.evaluateEventOptimizedForSkipTillNext(e, r);//
            }
        }
    }

    /**
     * This method will iterate runs in the same partition for the current event, for queries with a negation component.
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     */
    public void evaluateRunsByPartitionForNegation(Event e) throws CloneNotSupportedException {
        int key = e.getAttributeByName(ConfigFlags.partitionAttribute);
        if (this.activeRunsByPartition.containsKey(key)) {
            ArrayList<Run> partitionedRuns = this.activeRunsByPartition.get(key);
            int size = partitionedRuns.size();
            for (int i = 0; i < size; i++) {
                Run r = partitionedRuns.get(i);
                if (r.isFull()) {
                    continue;
                }
                this.evaluateEventOptimizedForNegation(e, r);//
            }
        }
    }

    /**
     * If the selection strategy is partition-contiguity, this method is called and it will iterate runs in the same partition for the current event
     *
     * @param e The current event which is being evaluated.
     * @throws CloneNotSupportedException
     */
    public void evaluateRunsForPartitionContiguity(Event e) throws CloneNotSupportedException {
        //筛选symbol与第一个事件相同的的所有事件
        //筛选查询中的第二个事件满足：
        //令size等于：哈希表主动分区运行中：
        //symbol=0：size = 哈希表【主动分区运行】中key=0的事件的size
        //symbol=1：size = 哈希表【主动分区运行】中key=1的事件的size
        //【主动分区运行】中有对应值的进入for循环：
        //for循环：
        //找到第二个事件类型与查询符合的事件；
        //
        //
        //匹配到第二个事件也不会isFull，还要后续检查
        //evaluateEventForPartitonContiguityOptimized(e, r)
        int key = e.getAttributeByName(ConfigFlags.partitionAttribute);
        if (this.activeRunsByPartition.containsKey(key)) {
            ArrayList<Run> partitionedRuns = this.activeRunsByPartition.get(key);
            int size = partitionedRuns.size();
            for (int i = 0; i < size; i++) {
                Run r = partitionedRuns.get(i);
                if (r.isFull()) {
                    continue;
                }
                this.evaluateEventForPartitonContiguityOptimized(e, r);//
            }
        }
    }


    /**
     * This method evaluates the event for a given run, for skip-till-any-match
     *
     * @param e The current event which is being evaluated.
     * @param r The run against which the evaluation goes
     * @throws CloneNotSupportedException
     *///r对e求值的运行值;
    // 0：begin；1：take；-1:same
    public void evaluateEventOptimizedForSkipTillAny(Event e, Run r) throws CloneNotSupportedException {
        int checkResult = this.checkPredicateOptimized(e, r);
        switch (checkResult) {
            case 1:
                //检查时间窗口
                boolean timeWindow = this.checkTimeWindow(e, r);
                if (timeWindow) {
                    //自我复制
                    Run newRun = this.cloneRun(r);
                    this.addRunByPartition(newRun);
                    this.addEventToRun(r, e);
                } else {
                    //超时删除
                    this.toDeleteRuns.add(r);
                }
                break;
            case 2:
                Run newRun = this.cloneRun(r);
                this.addRunByPartition(newRun);

                r.proceed();
                this.addEventToRun(r, e);
        }
    }

    /**
     * This method evaluates the event for a given run, for skip-till-next-match.
     *
     * @param e The current event which is being evaluated.
     * @param r The run against which the evaluation goes
     */
    public void evaluateEventOptimizedForSkipTillNext(Event e, Run r) {
        int checkResult = this.checkPredicateOptimized(e, r);
        switch (checkResult) {
            case 1://take
                boolean timeWindow = this.checkTimeWindow(e, r);
                if (timeWindow) {
                    this.addEventToRun(r, e);
                } else {
                    this.toDeleteRuns.add(r);
                }
                break;
            case 2:
                r.proceed();
                this.addEventToRun(r, e);
        }
    }

    /**
     * This method evaluates the event for a given run, for queries with a negation component.
     *
     * @param e The current event which is being evaluated.
     * @param r The run against which the evaluation goes
     */
    public void evaluateEventOptimizedForNegation(Event e, Run r) throws CloneNotSupportedException {
        int checkResult = this.checkPredicateOptimized(e, r);
        switch (checkResult) {
            case 1:
                boolean timeWindow = this.checkTimeWindow(e, r);
                if (timeWindow) {
                    Run newRun = this.cloneRun(r);
                    this.addRunByPartition(newRun);

                    this.addEventToRunForNegation(r, e);
                } else {
                    this.toDeleteRuns.add(r);
                }
                break;
            case 2:
                Run newRun = this.cloneRun(r);
                this.addRunByPartition(newRun);

                r.proceed();
                this.addEventToRunForNegation(r, e);
        }
    }

    /**
     * If the selection strategy is partition-contiguity, this method is called, and it evaluates the event for a given run
     *
     * @param e The current event which is being evaluated
     * @param r The run against which the evaluation goes
     * @throws CloneNotSupportedException
     */
    public void evaluateEventForPartitonContiguityOptimized(Event e, Run r) throws CloneNotSupportedException {
        //当前事件 ！= 第二个查询的事件类型 checkResult=0
        //当前事件 =   第二个查询的事件类型 checkResult=0
        int checkResult = this.checkPredicateOptimized(e, r);
        switch (checkResult) {
            case 0:
                this.toDeleteRuns.add(r);
                break;
            case 1:
                //检查时间窗口是否符合要求
                boolean timeWindow = this.checkTimeWindow(e, r);
                if (timeWindow) {
                    this.addEventToRun(r, e);
                } else {
                    this.toDeleteRuns.add(r);
                }
                break;
            case 2:
                r.proceed();
                this.addEventToRun(r, e);
        }


    }

    /**
     * This method evaluates an event against a run, for skip-till-any-match
     *
     * @param e The event which is being evaluated.
     * @param r The run which the event is being evaluated against.
     * @throws CloneNotSupportedException
     */
    public void evaluateEventForSkipTillAny(Event e, Run r) throws CloneNotSupportedException {
        boolean checkResult = true;


        checkResult = this.checkPredicate(e, r);// check predicate
        if (checkResult) { // the predicate if ok.
            checkResult = this.checkTimeWindow(e, r); // the time window is ok
            if (checkResult) {// predicate and time window are ok
                this.buffer.bufferEvent(e);// add the event to buffer
                int oldState = 0;
                int newState = 0;


                Run newRun = this.cloneRun(r); // clone this run


                oldState = newRun.getCurrentState();
                newRun.addEvent(e);                    // add the event to this run
                newState = newRun.getCurrentState();
                if (oldState != newState) {
                    this.activeRuns.add(newRun);
                } else {//kleene closure
                    if (newRun.isFull) {
                        //check match and output match
                        if (newRun.checkMatch()) {
                            this.outputMatch(new Match(newRun, this.nfa, this.buffer));
                            Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());

                        }
                    } else {
                        //check proceed
                        if (this.checkProceed(newRun)) {
                            Run newerRun = this.cloneRun(newRun);
                            this.activeRuns.add(newRun);
                            newerRun.proceed();
                            if (newerRun.isComplete()) {
                                this.outputMatch(new Match(r, this.nfa, this.buffer));
                                Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());

                            } else {
                                this.activeRuns.add(newerRun);
                            }
                        }
                    }
                }


            } else {
                this.toDeleteRuns.add(r);
            }

        }
    }

    /**
     * @author Dee
     * //谓词检查
     * eventGroups是一个哈希map，key = 事件类型，value = ABCEvent
     * eventGroups是一个时间戳下的事件包
     */
    public void evaluateEventForSkipTillNextByDeeCEPByDeeCEP(Map<String, List<Event>> eventGroups, Run r) throws CloneNotSupportedException {
        // 标识谓词检查结果，默认为 true。
        boolean checkResult = true;
        // 当前在NFA的几个状态
        int currentState = r.getCurrentState();
        //当前应该找的事件
        State s = this.nfa.getStates(currentState); // 获取当前状态。

        // 如果是并发边+事件包，则进行递归+回溯
        if (s.getStateType().equalsIgnoreCase("concurrent")) {
            checkResult = this.checkPredicateByDeeCEP(eventGroups, r);
        } else {// 如果是单边+事件包，false
            checkResult = false;
        }

        // 当前时间戳与当前部分匹配的最后时间戳不同
        // 注释掉 if 语句，作为正常的 SASE 逻辑
        // 正常情况下，该 if 语句用于处理并发逻辑，因此暂时注释掉。
        // if (e.getTimestamp() != lasttimestamp) {
        if (checkResult) { // 如果谓词检查通过。
            System.out.println("checkResult is true，满足当前谓词");
            // 检查时间窗口是否通过，事件包来的时候没有时间戳，但是事件包的时间戳都一样，拿第一个事件的时间戳就行
            Event e = eventGroups.values().iterator().next().get(0);
            checkResult = this.checkTimeWindow(e, r);
             System.out.println("the time window is ok");
             //12.28
            if (checkResult) { // 如果谓词和时间窗口都通过。
//                this.buffer.bufferEvent(e); // 将事件添加到缓冲区。
                int oldState = 0;
                int newState = 0;
                oldState = r.getCurrentState();

//                r.addEvent(e); // 添加事件到运行。

                // 更新当前所在状态机的状态。
                newState = r.getCurrentState();

                if (oldState == newState) { // Kleene 闭包
                    if (r.isFull()) {
                        // 检查匹配（全部通过）并输出匹配结果。
                        if (r.checkMatch()) {
                            this.outputMatch(new Match(r, this.nfa, this.buffer));
                            Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                            this.toDeleteRuns.add(r);
                        }
                    } else {
                        // 检查运行是否继续。
                        if (this.checkProceed(r)) {
                            Run newRun = this.cloneRun(r);

                            this.activeRuns.add(newRun);

                            this.addRunByPartition(newRun);
                            r.proceed();

                            if (r.isComplete()) {
                                this.outputMatch(new Match(r, this.nfa, this.buffer));
                                Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                                this.toDeleteRuns.add(r);
                            }
                        }
                    }
                }
            } else {
                this.toDeleteRuns.add(r);
            }
        }
        // }
    }

    /**
     * This method evaluates an event against a run, for skip-till-next-match
     * 该方法用于对一个运行进行事件评估，用于跳过到下一个匹配的逻辑。
     *
     * @param e 被评估的事件。
     * @param r 被评估事件的运行。
     * @throws CloneNotSupportedException 如果对某个对象不支持克隆操作。
     */
// 如果 checkResult = true，当前事件 e 将加入当前运行 r。
    public void evaluateEventForSkipTillNext(Event e, Run r) throws CloneNotSupportedException {
        boolean checkResult = true; // 标识谓词检查结果，默认为 true。
        int currentState = r.getCurrentState(); // 获取当前运行的状态。
        State s = this.nfa.getStates(currentState); // 获取当前状态。

        // 如果是并发边，则进行位运算
        // if(s.getStateType().equalsIgnoreCase("concurrent")){
        //     checkResult = this.checkConcurrentPredicate(e, r);
        // }else{//如果是普通边进行普通谓词检查
        checkResult = this.checkPredicate(e, r); // 检查普通谓词
        // }

        // 当前时间戳与当前部分匹配的最后时间戳不同
        // 注释掉 if 语句，作为正常的 SASE 逻辑
        // 正常情况下，该 if 语句用于处理并发逻辑，因此暂时注释掉。
        // if (e.getTimestamp() != lasttimestamp) {
        if (checkResult) { // 如果谓词检查通过。
            // System.out.println("checkResult is true，满足当前谓词");
            checkResult = this.checkTimeWindow(e, r); // 检查时间窗口是否通过。
            // System.out.println("the time window is ok");
            if (checkResult) { // 如果谓词和时间窗口都通过。
                this.buffer.bufferEvent(e); // 将事件添加到缓冲区。
                int oldState = 0;
                int newState = 0;
                oldState = r.getCurrentState();

                r.addEvent(e); // 添加事件到运行。

                // 更新当前所在状态机的状态。
                newState = r.getCurrentState();

                if (oldState == newState) { // Kleene 闭包
                    if (r.isFull()) {
                        // 检查匹配（全部通过）并输出匹配结果。
                        if (r.checkMatch()) {
                            this.outputMatch(new Match(r, this.nfa, this.buffer));
                            Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                            this.toDeleteRuns.add(r);
                        }
                    } else {
                        // 检查运行是否继续。
                        if (this.checkProceed(r)) {
                            Run newRun = this.cloneRun(r);

                            this.activeRuns.add(newRun);

                            this.addRunByPartition(newRun);
                            r.proceed();

                            if (r.isComplete()) {
                                this.outputMatch(new Match(r, this.nfa, this.buffer));
                                Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                                this.toDeleteRuns.add(r);
                            }
                        }
                    }
                }
            } else {
                this.toDeleteRuns.add(r);
            }
        }
        // }
    }


    /**
     * This method evaluates an event against a run, for queries with a negation component.
     *
     * @param e The event which is being evaluated.
     * @param r The run which the event is being evaluated against.
     * @throws CloneNotSupportedException
     */
    public void evaluateEventForNegation(Event e, Run r) throws CloneNotSupportedException {
        boolean checkResult = true;


        checkResult = this.checkPredicate(e, r);// check predicate
        if (checkResult) { // the predicate if ok.
            checkResult = this.checkTimeWindow(e, r); // the time window is ok
            if (checkResult) {// predicate and time window are ok
                this.buffer.bufferEvent(e);// add the event to buffer
                int oldState = 0;
                int newState = 0;


                Run newRun = this.cloneRun(r); // clone this run


                oldState = newRun.getCurrentState();
                newRun.addEvent(e);                    // add the event to this run
                newState = newRun.getCurrentState();
                if (oldState != newState) {
                    this.activeRuns.add(newRun);
                    State tempState = this.nfa.getStates(newState);
                    if (tempState.isBeforeNegation()) {
                        r.setBeforeNegationTimestamp(e.getTimestamp());
                    } else if (tempState.isAfterNegation()) {
                        r.setAfterNegationTimestamp(e.getTimestamp());
                    }
                } else {//kleene closure
                    if (newRun.isFull) {
                        //check match and output match
                        if (newRun.checkMatch()) {
                            this.outputMatchForNegation(new Match(newRun, this.nfa, this.buffer), newRun);
                            Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());

                        }
                    } else {
                        //check proceed
                        if (this.checkProceed(newRun)) {
                            Run newerRun = this.cloneRun(newRun);
                            this.activeRuns.add(newRun);
                            newerRun.proceed();
                            if (newerRun.isComplete()) {
                                this.outputMatchForNegation(new Match(r, this.nfa, this.buffer), r);
                                Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());

                            } else {
                                this.activeRuns.add(newerRun);
                            }
                        }
                    }
                }


            } else {
                this.toDeleteRuns.add(r);
            }

        }
    }

    /**
     * This methods add a new run to a partition.
     *
     * @param newRun The run to be added
     */

    public void addRunByPartition(Run newRun) {
        //在哈希表内 查找与当前事件相同symbol的映射
        // 有→在此key下运行
        // 没有→新建newPartition数组存入新key下的哈希表。然后再运行。
        if (this.activeRunsByPartition.containsKey(newRun.getPartitonId())) {
            this.activeRunsByPartition.get(newRun.getPartitonId()).add(newRun);//？
        } else {
            ArrayList<Run> newPartition = new ArrayList<Run>();
            newPartition.add(newRun);
            this.activeRunsByPartition.put(newRun.getPartitonId(), newPartition);
        }
    }

    /**
     * This method evaluates an event against a run.
     *
     * @param e The event which is being evaluated.
     * @param r The run which the event is being evaluated against.
     * @throws CloneNotSupportedException
     */


    public void evaluateEventForPartitonContiguity(Event e, Run r) throws CloneNotSupportedException {
        boolean checkResult = true;


        checkResult = this.checkPredicate(e, r);// check predicate
        if (checkResult) { // the predicate if ok.
            checkResult = this.checkTimeWindow(e, r); // the time window is ok
            if (checkResult) {// predicate and time window are ok
                this.buffer.bufferEvent(e);// add the event to buffer
                int oldState = 0;
                int newState = 0;
                oldState = r.getCurrentState();
                r.addEvent(e);
                newState = r.getCurrentState();
                if (oldState == newState)//kleene closure
                {
                    if (r.isFull) {
                        //check match and output match
                        if (r.checkMatch()) {
                            this.outputMatch(new Match(r, this.nfa, this.buffer));
                            Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                            this.toDeleteRuns.add(r);
                        }
                    } else {
                        //check proceed
                        if (this.checkProceed(r)) {
                            Run newRun = this.cloneRun(r);
                            this.activeRuns.add(newRun);
                            this.addRunByPartition(newRun);

                            r.proceed();
                            if (r.isComplete()) {
                                this.outputMatch(new Match(r, this.nfa, this.buffer));
                                Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                                this.toDeleteRuns.add(r);

                            }
                        }
                    }
                }


            } else {
                this.toDeleteRuns.add(r);
            }

        } else {
            this.toDeleteRuns.add(r);
        }
    }


    /**
     * This method adds an event to a run
     *
     * @param r The event to be added
     * @param e The run to which the event is added
     */
    public void addEventToRun(Run r, Event e) {
        this.buffer.bufferEvent(e);// add the event to buffer
        int oldState = 0;
        int newState = 0;
        oldState = r.getCurrentState();
        r.addEvent(e);//为运行增加事件；state[2,2,0]
        newState = r.getCurrentState();
        if (oldState == newState)//kleene closure
        {
            if (r.isFull) {
                //check match and output match
                if (r.checkMatch()) {
                    this.outputMatch(new Match(r, this.nfa, this.buffer));
                    Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                    this.toDeleteRuns.add(r);
                }
            }
        }

    }

    /**
     * This method adds an event to a run, for queries with a negation component.
     *
     * @param r The event to be added
     * @param e The run to which the event is added
     */
    public void addEventToRunForNegation(Run r, Event e) {
        this.buffer.bufferEvent(e);// add the event to buffer
        int oldState = 0;
        int newState = 0;
        oldState = r.getCurrentState();
        r.addEvent(e);
        newState = r.getCurrentState();
        State tempState = this.nfa.getStates(newState);

        if (tempState.isBeforeNegation()) {
            r.setBeforeNegationTimestamp(e.getTimestamp());
        } else if (tempState.isAfterNegation()) {
            r.setAfterNegationTimestamp(e.getTimestamp());
        }
        if (oldState == newState)//kleene closure
        {
            if (r.isFull) {
                //check match and output match
                if (r.checkMatch()) {
                    this.outputMatchByPartitionForNegation(new Match(r, this.nfa, this.buffer), r);
                    Profiling.totalRunLifeTime += (System.nanoTime() - r.getLifeTimeBegin());
                    this.toDeleteRuns.add(r);
                }
            }
        }

    }

    /**
     * 创建一个包含输入事件的新运行。
     *
     * @param e 当前事件。
     * @throws EvaluationException
     */
    public void createNewRun(Event e) throws EvaluationException {
        // 检查NFA的初始状态是否能够以给定事件开始
        if (this.nfa.getStates()[0].canStartWithEvent(e)) {
            // 将事件缓存到运行中
            this.buffer.bufferEvent(e);
            // 从engineRunController获取当前运行
            Run newRun = this.engineRunController.getRun();
            // 使用NFA初始化新运行
            newRun.initializeRun(this.nfa);
            // 将事件添加到新运行中
            newRun.addEvent(e);
            // 更新运行的数量
            numberOfRuns++;
            // 将新运行添加到活动运行列表中
            this.activeRuns.add(newRun);
        }
    }

    /**
     * 创建一个包含输入事件的新运行。
     * eventGroups是一个哈希map，key = 事件类型，value = ABCEvent
     *
     * @throws EvaluationException
     */
    public void createNewRunByDeeCEP(Map<String, List<Event>> eventGroups) throws EvaluationException {
        boolean checkcombinations = false;
        //canStartWithEventByDeeCEP 方法，而这个方法属于 State 类的实例，这个实例是通过 this.nfa.getStates()[0] 获取的。
        List<List<Event>> combinations = this.nfa.getStates()[0].canStartWithEventByDeeCEP(eventGroups);
        checkcombinations = combinations != null && !combinations.isEmpty();
        // 检查NFA的初始状态是否能够以给定事件开始
        if (checkcombinations) {
// combinations：
// 0：B1C2
// 0：B3C2
// eventList：0：B1C2
            // 从engineRunController获取当前运行
            Run newRun;

            for (List<Event> eventList : combinations) {
                // 在每次迭代中创建一个新的运行
                newRun = this.engineRunController.getRun();
                newRun.initializeRun(this.nfa);
                // 更新运行的数量
                numberOfRuns++;

                for (Event e : eventList) {
                    this.buffer.bufferEvent((Event) e);
                    //[0,0,0]->[-2,0,0]
                    // 将事件添加到新运行中
                    newRun.addEvent((Event) e);
                }

                // 将新运行添加到活动运行列表中
                this.activeRuns.add(newRun);
            }

        }
    }


    /**
     * Creates a new run containing the input event and adds the new run to the corresponding partition
     *
     * @param e The current event
     * @throws EvaluationException
     * @throws CloneNotSupportedException
     */
    public void createNewRunByPartition(Event e) throws EvaluationException, CloneNotSupportedException {
//createNewRunByPartition(Event e)：将运行添加到新的分区
        //筛选能够满足第一个查询的事件
        if (this.nfa.getStates()[0].canStartWithEvent(e)) {
            //[1]存入缓存区
            this.buffer.bufferEvent(e);
            //[2]产生一个newRun(初始化前为空)在运行池Runpool中
            Run newRun = this.engineRunController.getRun();
            //2.1初始化此空newRun：
            //nfa、size、state、eventIds、currentStat、alive、isFull、isComplete、count;
            newRun.initializeRun(this.nfa);
            //2.3//1）获取事件类型：“normal”；
            //2）eventId:存放此NFA中的已匹配事件(失败事件被清除）；
            //3）state[0,0]→state[2,0]
            //4）第一个事件：count:0→1；currentState：0
            //第二个事件：count:1→2；currentState：1；
            //5）判断是否为查询的最后一个事件：
            //1. 是最后一个事件：setFull = true
            //2. 不是最后一个事件：
            //6）记录第一件事件的时间；
            //7）partitonId = symbol；
            newRun.addEvent(e);
            //this.numberOfRuns.update(1);
            numberOfRuns++;
            //存入activeRuns数组（数组存放所有部分匹配。复制来的存放吗？）;
            this.activeRuns.add(newRun);
            //在哈希表内 查找与当前事件相同symbol的映射
            //
            //1. 有→在此key下运行
            //2. 没有→新建newPartition数组存入新key下的哈希表。然后再运行。
            this.addRunByPartition(newRun);
        }
    }

    /**
     * Checks the predicate for e against r
     *
     * @param e The current event
     * @param r The run against which e is evaluated
     * @return The check result, 0 for false, 1 for take or begin, 2 for proceed
     */
    public int checkPredicateOptimized(Event e, Run r) {//0 for false, 1 for take or begin, 2 for proceed
        int currentState = r.getCurrentState();
        State s = this.nfa.getStates(currentState);
        if (!s.getEventType().equalsIgnoreCase(e.getEventType())) {// event type check;
            return 0;
        }

        if (!s.isKleeneClosure()) {
            Edge beginEdge = s.getEdges(0);
            boolean result;
            //result = firstEdge.evaluatePredicate(e, r, buffer);
            result = beginEdge.evaluatePredicate(e, r, buffer);//
            if (result) {
                return 1;
            }
        } else {
            if (r.isKleeneClosureInitialized()) {
                boolean result;
                result = this.checkProceedOptimized(e, r);//proceedEdge.evaluatePredicate(e, r, buffer);
                if (result) {
                    return 2;
                } else {


                    Edge takeEdge = s.getEdges(1);
                    result = takeEdge.evaluatePredicate(e, r, buffer);
                    if (result) {
                        return 1;
                    }
                }
            } else {
                Edge beginEdge = s.getEdges(0);
                boolean result;

                result = beginEdge.evaluatePredicate(e, r, buffer);//
                if (result) {
                    return 1;
                }
            }
        }


        return 0;


    }

    /**
     * Checks whether the run needs to proceed if we add e to r
     *
     * @param e The current event
     * @param r The run against which e is evaluated
     * @return The checking result, TRUE for OK to proceed
     */
    public boolean checkProceedOptimized(Event e, Run r) {
        int currentState = r.getCurrentState();
        State s = this.nfa.getStates(currentState + 1);
        if (!s.getEventType().equalsIgnoreCase(e.getEventType())) {// event type check;
            return false;
        }
        Edge beginEdge = s.getEdges(0);
        boolean result;
        result = beginEdge.evaluatePredicate(e, r, buffer);
        return result;
    }

    /**
     * Checks whether the event satisfies the predicates of a run
     * 检查事件是否满足运行的谓词
     * <p>
     * eventGroups是一个哈希map，key = 事件类型，value = ABCEvent
     * eventGroups是一个时间戳下的事件包
     *
     * @param r the current run 当前运行
     * @return the check result 检查结果
     */
    public boolean checkPredicateByDeeCEP(Map<String, List<Event>> eventGroups, Run r) {
        // 获取当前部分匹配的状态
        int currentState = r.getCurrentState();
        // 获取当前部分匹配要找什么
        State s = this.nfa.getStates(currentState);
        State stateInstance = new State();  // 创建 State 类的对象

        // 检查事件类型是否匹配
        List<List<Event>> combinations = stateInstance.findCombinations(eventGroups, s.getEventType());
        if (combinations == null||combinations.isEmpty()) {
            // 如果事件类型不匹配，返回false
            return false;
        }else{
            return true;
        }
        //注销了边上的谓词计算
        //注销了闭包
    }


    /**
     * Checks whether the event satisfies the predicates of a run
     * 检查事件是否满足运行的谓词
     *
     * @param e the current event 当前事件
     * @param r the current run 当前运行
     * @return the check result 检查结果
     */
    public boolean checkPredicate(Event e, Run r) {
        // 获取当前运行的状态
        int currentState = r.getCurrentState();
        // 获取当前状态
        State s = this.nfa.getStates(currentState);

        // 检查事件类型是否匹配
        if (!s.getEventType().equalsIgnoreCase(e.getEventType())) {
            // 如果事件类型不匹配，返回false
            return false;
        }

        // 检查状态是否不是Kleene闭包状态
        if (!s.isKleeneClosure()) {
            // 获取状态的第一条边,beginEdge用于闭包
            Edge beginEdge = s.getEdges(0);
            // 定义用于存储谓词计算结果的变量
            boolean result;
            // 评估第一条边上的谓词，将结果存储在result中
            result = beginEdge.evaluatePredicate(e, r, buffer);//
            // 如果谓词计算结果为真，返回true
            if (result) {
                return true;
            }
        } else {//DeeDee
            if (r.isConcurrentInitialized()) {
                //从这里开始写
                //如果States是concurrent事件
                //获取边上的谓词，比如B^C^
            }
            // 如果状态是Kleene闭包状态
            // 检查是否初始化了Kleene闭包
            if (r.isKleeneClosureInitialized()) {
                Edge takeEdge = s.getEdges(1);
                boolean result;
                // 评估第二条边上的谓词，将结果存储在result中
                result = takeEdge.evaluatePredicate(e, r, buffer);
                // 如果谓词计算结果为真，返回true
                if (result) {
                    return true;
                }
            } else {
                Edge beginEdge = s.getEdges(0);
                // 定义用于存储谓词计算结果的变量
                boolean result;
                // 评估第一条边上的谓词，将结果存储在result中
                result = beginEdge.evaluatePredicate(e, r, buffer);//
                // 如果谓词计算结果为真，返回true
                if (result) {
                    return true;
                }
            }
        }
        // 如果未满足任何条件，返回false
        return false;
    }

    /**
     * Checks whether the event satisfies the partition of a run, only used under partition-contiguity selection strategy
     *
     * @param e the current event
     * @param r the current run
     * @return the check result, boolean format
     */
    public boolean checkPartition(Event e, Run r) {

        if (r.getPartitonId() == e.getAttributeByName(this.nfa.getPartitionAttribute())) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether a kleene closure state can proceed to the next state
     *
     * @param r the current run
     * @return the check result, boolean format
     */


    public boolean checkProceed(Run r) {// cannot use previous, only use position?
        int currentState = r.getCurrentState();

        Event previousEvent = this.buffer.getEvent(r.getPreviousEventId());
        State s = this.nfa.getStates(currentState);


        Edge proceedEdge = s.getEdges(2);
        boolean result;
        result = proceedEdge.evaluatePredicate(previousEvent, r, buffer);//
        if (result) {
            return true;
        }

        return false;

    }

    public boolean checkNegation(Event e) throws EvaluationException {
        if (this.nfa.getNegationState().canStartWithEvent(e)) {
            return true;
        }
        return false;
    }

    public void indexNegationByPartition(Event e) {
        int id = e.getAttributeByName(this.nfa.getPartitionAttribute());
        if (this.negationEventsByPartition.containsKey(id)) {
            this.negationEventsByPartition.get(id).add(e);
        } else {
            ArrayList<Event> newPartition = new ArrayList<Event>();
            newPartition.add(e);
            this.negationEventsByPartition.put(id, newPartition);
        }

    }

    public boolean searchNegation(int beforeTimestamp, int afterTimestamp, ArrayList<Event> list) {
        // basic idea is to use binary search on the timestamp
        int size = list.size();
        int lower = 0;
        int upper = size - 1;
        Event tempE;
        while (lower <= upper) {
            tempE = list.get((lower + upper) / 2);
            if (tempE.getTimestamp() >= beforeTimestamp && tempE.getTimestamp() <= afterTimestamp) {
                return true;
            }
            if (tempE.getTimestamp() < beforeTimestamp) {
                lower = (lower + upper) / 2;
            } else {
                upper = (lower + upper) / 2;
            }

        }
        return false;
    }

    public boolean searchNegationByPartition(int beforeTimestamp, int afterTimestamp, int partitionId) {
        if (this.negationEventsByPartition.containsKey(partitionId)) {
            ArrayList<Event> tempList = this.negationEventsByPartition.get(partitionId);
            return this.searchNegation(beforeTimestamp, afterTimestamp, tempList);

        }
        return false;
    }

    /**
     * Clones a run
     *
     * @param r the run to be cloned
     * @return the new run cloned from the input run.
     * @throws CloneNotSupportedException
     */

    public Run cloneRun(Run r) throws CloneNotSupportedException {
        Run newRun = this.engineRunController.getRun();
        newRun = (Run) r.clone();
        numberOfRuns++;
        return newRun;
    }

    /**
     * Checks whether the event satisfies the time window constraint of a run
     *
     * @param e the current event
     * @param r the current run
     * @return the check result
     */

    public boolean checkTimeWindow(Event e, Run r) {
        if ((e.getTimestamp() - r.getStartTimeStamp()) <= this.nfa.getTimeWindow()) {
            return true;
        }
        return false;
    }


    /**
     * Outputs a match, and profiles.
     *
     * @param m the match to be output
     */


    public void outputMatch(Match m) {

        Profiling.numberOfMatches++;
        //this.matches.addMatch(m);
        if (ConfigFlags.printResults) {
            System.out.println("----------Here is the No." + Profiling.numberOfMatches + " match----------");
            if (Profiling.numberOfMatches == 878) {
                System.out.println("debug");
            }
            System.out.println(m);
        }


    }

    /**
     * Outputs a match, and profiles, for queries with a negation componengt, without a partition attribute.
     *
     * @param m the match to be output
     */
    public void outputMatchForNegation(Match m, Run r) {
        if (this.searchNegation(r.getBeforeNegationTimestamp(), r.getAfterNegationTimestamp(), this.negationEvents)) {
            Profiling.negatedMatches++;
            System.out.println("~~~~~~~~~~~~~~~~Here is a negated match~~~~~~~~~~~~~~~");
            System.out.println(m);
        } else {

            Profiling.numberOfMatches++;
            //this.matches.addMatch(m);
            if (ConfigFlags.printResults) {
                System.out.println("----------Here is the No." + Profiling.numberOfMatches + " match----------");
                if (Profiling.numberOfMatches == 878) {
                    System.out.println("debug");
                }
                System.out.println(m);
            }

        }

    }

    /**
     * Outputs a match, and profiles, for queries with a negation componengt, with a partition attribute.
     *
     * @param m the match to be output
     */
    public void outputMatchByPartitionForNegation(Match m, Run r) {
        if (this.searchNegationByPartition(r.getBeforeNegationTimestamp(), r.getAfterNegationTimestamp(), r.getPartitonId())) {
            Profiling.negatedMatches++;
            System.out.println("~~~~~~~~~~~~~~~~Here is a negated match~~~~~~~~~~~~~~~");
            System.out.println(m);
        } else {

            Profiling.numberOfMatches++;
            //this.matches.addMatch(m);
            if (ConfigFlags.printResults) {
                System.out.println("----------Here is the No." + Profiling.numberOfMatches + " match----------");
                if (Profiling.numberOfMatches == 878) {
                    System.out.println("debug");
                }
                System.out.println(m);
            }

        }

    }

    /**
     * Deletes runs violating the time window
     *
     * @param currentTime current time
     * @param timeWindow  time window of the query
     * @param delayTime   specified delay period, any run which has been past the time window by this value would be deleted.
     */

    public void deleteRunsOverTimeWindow(int currentTime, int timeWindow, int delayTime) {
        int size = this.activeRuns.size();
        Run tempRun = null;
        for (int i = 0; i < size; i++) {
            tempRun = this.activeRuns.get(i);
            if (!tempRun.isFull && tempRun.getStartTimeStamp() + timeWindow + delayTime < currentTime) {
                this.toDeleteRuns.add(tempRun);
                Profiling.numberOfRunsOverTimeWindow++;

            }
        }
    }

    /**
     * Cleans useless runs
     */
    public void cleanRuns() {

        int size = this.toDeleteRuns.size();
        Run tempRun = null;
        for (int i = 0; i < size; i++) {
            tempRun = toDeleteRuns.get(0);
            Profiling.totalRunLifeTime += (System.nanoTime() - tempRun.getLifeTimeBegin());
            tempRun.resetRun();
            this.activeRuns.remove(tempRun);
            this.toDeleteRuns.remove(0);
            Profiling.numberOfRunsCutted++;
        }


    }

    /**
     * Cleans useless runs by partition.
     */
    public void cleanRunsByPartition() {

        int size = this.toDeleteRuns.size();
        Run tempRun = null;
        ArrayList<Run> partitionedRuns = null;
        for (int i = 0; i < size; i++) {
            tempRun = toDeleteRuns.get(0);
            Profiling.totalRunLifeTime += (System.nanoTime() - tempRun.getLifeTimeBegin());
            tempRun.resetRun();
            this.activeRuns.remove(tempRun);
            this.toDeleteRuns.remove(0);
            Profiling.numberOfRunsCutted++;
            partitionedRuns = this.activeRunsByPartition.get(tempRun.getPartitonId());
            partitionedRuns.remove(tempRun);
            if (partitionedRuns.size() == 0) {
                this.activeRunsByPartition.remove(partitionedRuns);
            }

        }


    }


    /**
     * @return the input
     */
    public Stream getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(Stream input) {
        this.input = input;
    }

    /**
     * @return the buffer
     */
    public EventBuffer getBuffer() {
        return buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(EventBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * @return the nfa
     */
    public NFA getNfa() {
        return nfa;
    }

    /**
     * @param nfa the nfa to set
     */
    public void setNfa(NFA nfa) {
        this.nfa = nfa;
    }

    /**
     * @return the engineRunController
     */
    public RunPool getEngineRunController() {
        return engineRunController;
    }

    /**
     * @param engineRunController the engineRunController to set
     */
    public void setEngineRunController(RunPool engineRunController) {
        this.engineRunController = engineRunController;
    }


    /**
     * @return the activeRuns
     */
    public ArrayList<Run> getActiveRuns() {
        return activeRuns;
    }

    /**
     * @param activeRuns the activeRuns to set
     */
    public void setActiveRuns(ArrayList<Run> activeRuns) {
        this.activeRuns = activeRuns;
    }

    /**
     * @return the matches
     */
    public MatchController getMatches() {
        return matches;
    }

    /**
     * @param matches the matches to set
     */
    public void setMatches(MatchController matches) {
        this.matches = matches;
    }


    /**
     * @return the toDeleteRuns
     */
    public ArrayList<Run> getToDeleteRuns() {
        return toDeleteRuns;
    }

    /**
     * @param toDeleteRuns the toDeleteRuns to set
     */
    public void setToDeleteRuns(ArrayList<Run> toDeleteRuns) {
        this.toDeleteRuns = toDeleteRuns;
    }

    /**
     * @return the activeRunsByPartiton
     */
    public HashMap<Integer, ArrayList<Run>> getActiveRunsByPartiton() {
        return activeRunsByPartition;
    }

    /**
     * @param activeRunsByPartiton the activeRunsByPartiton to set
     */
    public void setActiveRunsByPartiton(
            HashMap<Integer, ArrayList<Run>> activeRunsByPartiton) {
        this.activeRunsByPartition = activeRunsByPartiton;
    }

    /**
     * @return the activeRunsByPartition
     */
    public HashMap<Integer, ArrayList<Run>> getActiveRunsByPartition() {
        return activeRunsByPartition;
    }

    /**
     * @param activeRunsByPartition the activeRunsByPartition to set
     */
    public void setActiveRunsByPartition(
            HashMap<Integer, ArrayList<Run>> activeRunsByPartition) {
        this.activeRunsByPartition = activeRunsByPartition;
    }

    /**
     * @return the negationEvents
     */
    public ArrayList<Event> getNegationEvents() {
        return negationEvents;
    }

    /**
     * @param negationEvents the negationEvents to set
     */
    public void setNegationEvents(ArrayList<Event> negationEvents) {
        this.negationEvents = negationEvents;
    }

    /**
     * @return the negationEventsByPartition
     */
    public HashMap<Integer, ArrayList<Event>> getNegationEventsByPartition() {
        return negationEventsByPartition;
    }

    /**
     * @param negationEventsByPartition the negationEventsByPartition to set
     */
    public void setNegationEventsByPartition(
            HashMap<Integer, ArrayList<Event>> negationEventsByPartition) {
        this.negationEventsByPartition = negationEventsByPartition;
    }


}
