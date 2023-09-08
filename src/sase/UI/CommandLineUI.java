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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.sourceforge.jeval.EvaluationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import sase.engine.ConfigFlags;
import sase.engine.EngineController;
import sase.engine.Profiling;
import sase.query.*;
import sase.stream.ABCEvent;
import sase.stream.ParseStockStreamConfig;
import sase.stream.StockStreamConfig;
import sase.stream.StreamController;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


/**
 * The interface
 * @author haopeng
 *
 */
@EnableAspectJAutoProxy
public class CommandLineUI {
	//true：开启流生成器
	//false：用文件中的 流
	public static boolean streamControllerSwitch = false;
	//true：正常输出到控制台
	//false：输出到指定文件
	public static boolean outPut = false;

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
	 * 2: 是否打印结果(1为打印,0为不打印)
	 * 3: 是否使用共享技术("sharingengine"使用,空为不使用)
	 */
	public static void main(String args[]) throws CloneNotSupportedException, EvaluationException, FileNotFoundException,IOException{
		String nfaFileLocation = "example\\test\\test.query";
		String streamConfigFile = "example\\test\\test.stream";
		//恢复控制台输出删除trycatch
		try {
			// 创建一个新的输出流，将标准输出重定向到文件
			PrintStream fileOutput = new PrintStream(new FileOutputStream("C:\\Users\\Dee\\Desktop\\第一篇论文\\新events-10w-50%.txt"));
			// 将标准输出设置为新的输出流
			System.setOut(fileOutput);


		String engineType = null;
		if(args.length > 0){
			nfaFileLocation = args[0];
		}
		
		if(args.length > 1){
			streamConfigFile = args[1];
		}
		
		if(args.length > 2){
			if(Integer.parseInt(args[2])== 1){
				ConfigFlags.printResults = true;
			}else{
				ConfigFlags.printResults = false;
			}
		}
		
		if(args.length > 3){
			engineType = args[3];
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

		myStreamController = new StreamController(StockStreamConfig.streamSize,"ABCEvent");
//		myStreamController.generateStockEventsAsConfigType();
		myEngineController.setInput(myStreamController.getMyStream());
//		myStreamController.printStream();
		myEngineController.runEngine();
		System.out.println("\nProfiling results for repeat No." + (i+1) +" are as follows:");
		Profiling.printProfiling();



			// 关闭输出流
			fileOutput.close();
			// 恢复标准输出
			System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		}
	}
//}
