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
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.sourceforge.jeval.EvaluationException;
import sase.engine.ConfigFlags;
import sase.engine.EngineController;
import sase.engine.Profiling;
import sase.query.*;
import sase.stream.ABCEvent;
import sase.stream.ParseStockStreamConfig;
import sase.stream.StockStreamConfig;
import sase.stream.StreamController;

/**
 * The interface
 * @author haopeng
 *
 */

public class CommandLineUI {


	/**
	 * The main entry to run the engine under command line
	 *
	 * @param args the inputs
	 * 0: the nfa file location
	 * 1: the stream config file
	 * 2: print the results or not (1 for print, 0 for not print)
	 * 3: use sharing techniques or not, ("sharingengine" for use, nothing for not use)
	 *
	 * 0: NFA文件位置
	 * 1: 流配置文件
	 * 2: 是否输出结果，结果地址
	 * 3: 是否使用并发事件处理技术("concurrent"使用,"0"为不使用)
	 * 4: 生成事件流：abcevent，stockevent，abcconcurrentoderevent，abcconcurrentunoderevent
	 */
	public static void main(String args[]) throws CloneNotSupportedException, EvaluationException, FileNotFoundException,IOException{
		String nfaFileLocation = "example\\test\\test.query";
		String streamConfigFile = "example\\test\\test.stream";

		ConfigFlags.printResults = true;

		String engineType = null;
		if(args.length > 0){
			nfaFileLocation = args[0];
		}

		if(args.length > 1){
			streamConfigFile = args[1];
		}

		if(args.length > 2){
			//输出文件到外部
			// 外部地址：args[2]
			if(args[2]!=null){
			//args[2]：文件输出位置
			// args[1]：事件流的地址
			// 本程序的输出名字是：args[1]+结果
			}
		}
		//3: 是否使用并发事件处理技术("concurrent"使用,"0"为不使用)传统SASE处理
		if (args.length > 3 && Objects.equals(args[3], "concurrent")) {
			ConfigFlags.processConcurrentEventStream = true;
		} else {
			ConfigFlags.processConcurrentEventStream = false;
		}

		//解析.query文件中的配置流信息，根据配置信息，流生成器生成流
//		ParseStockStreamConfig.parseStockEventConfig(streamConfigFile);
		//声明StreamController变量,但未初始化
		StreamController myStreamController = null;
		//声明EngineController变量,但未初始化和设置NFA等
		EngineController myEngineController = new EngineController();
		if(engineType != null){
			myEngineController = new EngineController(engineType);
		}
		//构建NFA
		myEngineController.setNfa(nfaFileLocation);
//		for(int i = 0; i < 1; i ++){
		//repreat multiple times for a constant performance
		int i=0;
		myEngineController.initializeEngine();
		//运行垃圾收集器
		System.gc();
		System.out.println("\nRepeat No." + (i+1) +" is started...");
		//myStreamn Controller = new StreamController(StockStreamConfig.streamSize,"StockEvent");
		//新建流控制器："ABCEvent"
		if (args.length > 3 ) {
			myStreamController = new StreamController(StockStreamConfig.streamSize,args[4]);
		}else{
			myStreamController = new StreamController(StockStreamConfig.streamSize,"abcevent");
		}

//		myStreamController.generateStockEventsAsConfigType();
		myEngineController.setInput(myStreamController.getMyStream());
//		myStreamController.printStream();
		myEngineController.runEngine();
		System.out.println("\nProfiling results for repeat No." + (i+1) +" are as follows:");
		Profiling.printProfiling();



	}

}

//}
