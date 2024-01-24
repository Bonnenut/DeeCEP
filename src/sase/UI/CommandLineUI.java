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
package sase.UI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import net.sourceforge.jeval.EvaluationException;
import sase.engine.ConfigFlags;
import sase.engine.EngineController;
import sase.engine.Profiling;
import sase.stream.ABCEvent;
import sase.stream.StockStreamConfig;
import sase.stream.Stream;
import sase.stream.StreamController;

/**
 * The interface
 *
 * @author haopeng
 */

public class CommandLineUI {
    static public int tw=3;

    /**
     * The main entry to run the engine under command line
     *
     * @param args the inputs
     *             <p>
     *             0: 流文件地址
     *             1: 输出结果到指定地址
     */
    public static void main(String args[]) throws CloneNotSupportedException, EvaluationException, FileNotFoundException, IOException {
        String nfaFileLocation = "E:\\SASE\\example\\test\\test.query";
        String streamConfigFile = "example\\test\\test.stream";
        //是否打印结果：
        ConfigFlags.printResults = true;
        //是否使用处理乱序功能
        ConfigFlags.processUnoderConcurrentEventStream = false;
        //是否使用并发事件处理技术
        ConfigFlags.processOderConcurrentEventStream = true;
        //是否使用原始SASE
        ConfigFlags.sase = false;

//
//        if (args.length > 0) {
//            nfaFileLocation = args[0];
//        }


        StreamController myStreamController = null;
        if (args[0].contains("\\")) {
            streamConfigFile = args[0];
            List<String> lines = null;
            try {
                // 读取文件中的所有行到一个列表中
                lines = Files.readAllLines(Paths.get(streamConfigFile));
            } catch (IOException e) {
                System.out.println("读取文件时发生错误：" + e.getMessage());
                return;
            }
            // 创建一个与文件中行数相等的事件数组
            ABCEvent[] events = new ABCEvent[lines.size()];

            // 解析每一行来创建 ABCEvent 对象
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim(); // Trim 去除行首尾空白
                // 使用正则表达式 "\\s+" 来按空格分割字符串，它匹配任何数量的空格字符
                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    // 移除每个部分的标签（例如 "ID="），只保留值，然后进行解析
                    int id = Integer.parseInt(parts[0].split("=")[1]);
                    int timestamp = Integer.parseInt(parts[1].split("=")[1]);
                    int symbol = Integer.parseInt(parts[2].split("=")[1]);
                    String eventType = parts[3].split("=")[1];
                    int price = Integer.parseInt(parts[4].split("=")[1]);
                    int volume = Integer.parseInt(parts[5].split("=")[1]);

                    events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);
                } else {
                    System.out.println("文件行格式不正确: " + line);
                    // 如果一行格式不正确，则退出循环
                    return;
                }
            }

            // 创建Stream实例
            Stream myStream = new Stream(lines.size());
            // 设置事件到Stream中
            myStream.setEvents(events);
            myStreamController = new StreamController();
            // 将Stream实例设置到StreamController中
            myStreamController.setMyStream(myStream);

        } else{//没有外部文件，使用本地流生成器
            //新建流控制器："ABCEvent"
            myStreamController = new StreamController(StockStreamConfig.streamSize, args[4]);
        }


        if (args.length > 1 && args[1] != null) {
            // 使用java.io.File对象来解析args[1]，获取文件名
            File inputFile = new File(args[0]);
            String fileNameWithoutExtension = inputFile.getName();
            // 去除".txt"扩展名
            if (fileNameWithoutExtension.endsWith(".txt")) {
                fileNameWithoutExtension = fileNameWithoutExtension.substring(0, fileNameWithoutExtension.length() - 4);
            }
            // 构建输出文件的路径
            String outputPath = args[1] +"\\"+ fileNameWithoutExtension + "结果.txt";
            try {
                // 创建文件输出PrintStream对象
                PrintStream fileOut = new PrintStream(outputPath);
                // 设置标准输出到文件
                System.setOut(fileOut);
            } catch (FileNotFoundException e) {
                // 异常处理：文件未找到
                System.err.println("无法创建文件：" + e.getMessage());
                return;
            }
        }


        String engineType = null;
        //声明EngineController变量
        EngineController myEngineController = new EngineController();
        if (engineType != null) {
            myEngineController = new EngineController(engineType);
        }
        //构建NFA
        myEngineController.setNfa(nfaFileLocation);
        int i = 0;
        myEngineController.initializeEngine();
        //运行垃圾收集器
        System.gc();
        System.out.println("\nRepeat No." + (i + 1) + " is started...");


        myEngineController.setInput(myStreamController.getMyStream());
        myEngineController.runEngine();
        System.out.println("\nProfiling results for repeat No." + (i + 1) + " are as follows:");
        Profiling.printProfiling();
    }
}
