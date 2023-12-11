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
package sase.stream;

import sase.UI.CommandLineUI;

import java.io.*;
import java.util.Random;

/**
 * This class wraps the stream, specifies how to generate or import stream.
 * @author haopeng
 *
 */
public class StreamController {
	/**
	 * The stream
	 */
	Stream myStream;
	/**
	 * The size of the stream
	 */
	 int size;

	/**
	 * event id
	 */
	int eventID;

	/**
	 * A random number generator
	 */
	Random randomGenerator;

	/**
	 * Default constructor
	 */
	public StreamController(){
		eventID = 0;
		randomGenerator = new Random(11);
	}

	/**
	 * Constructor, specified size and event type
	 * @param size
	 * @param eventType
	 */
	public StreamController(int size, String eventType){
		this.size = size;
		myStream = new Stream(size);
		if(eventType.equalsIgnoreCase("abcevent")){
			this.generateABCEvents();
		}

		if(eventType.equalsIgnoreCase("stockevent")){
			this.generateStockEvents();
		}
	}
	/**
	 * Generates a series of stock events
	 */
	public void generateStockEventsAsConfig(){
		Random r = new Random(StockStreamConfig.randomSeed);
		StockEvent events[] = new StockEvent[this.size];
		int id;
		int timestamp = 0;
		int symbol;
		int volume;
		int price = r.nextInt(100);
		int random = 0;
		String eventType = "stock";



		for (int i = 0; i < size; i ++){
			id = i;
			timestamp = id;

			symbol = r.nextInt(StockStreamConfig.numOfSymbol) + 1;
			price = r.nextInt(StockStreamConfig.maxPrice) + 1;
			volume = r.nextInt(StockStreamConfig.maxVolume) + 1;

			events[i] = new StockEvent(id, timestamp, symbol, price, volume);
		}
		myStream.setEvents(events);


	}

	/**
	 * Generates a series of stock events
	 */
	public void generateStockEventsAsConfigType(){
		if(StockStreamConfig.increaseProbability > 100){
		Random r = new Random(StockStreamConfig.randomSeed);
		StockEvent events[] = new StockEvent[this.size];
		int id;
		int timestamp = 0;
		int symbol;
		int volume;
		int price = r.nextInt(100);
		String eventType = "stock";



		for (int i = 0; i < size; i ++){
			id = i;
			timestamp = id;

			symbol = r.nextInt(StockStreamConfig.numOfSymbol) + 1;
			price = r.nextInt(StockStreamConfig.maxPrice) + 1;
			volume = r.nextInt(StockStreamConfig.maxVolume) + 1;
			eventType = "stock" + symbol;
			events[i] = new StockEvent(id, timestamp, symbol, price, volume, eventType);
		}
		myStream.setEvents(events);

		}else{
			this.generateStockEventsWithIncreaseProbability();
		}
	}

	/**
	 * Generates a series of stock events
	 */
	public void generateStockEventsWithIncreaseProbability(){

		Random r = new Random(StockStreamConfig.randomSeed);
		StockEvent events[] = new StockEvent[this.size];
		int id;
		int timestamp = 0;
		int symbol;
		int volume;
		int price[] = new int[StockStreamConfig.numOfSymbol];
		for(int i = 0; i < StockStreamConfig.numOfSymbol; i ++){
			//initializes the prices of each stock
			price[i] = r.nextInt(1000);
		}

		int random = 0;
		String eventType = "stock";



		for (int i = 0; i < size; i ++){
			id = i;
			timestamp = id;
			symbol = r.nextInt(StockStreamConfig.numOfSymbol) + 1;
			random = r.nextInt(100) + 1;
			if(random <= StockStreamConfig.increaseProbability){
				//increase
				price[symbol - 1] += (r.nextInt(3) + 1);
			}else if(random > (100 + StockStreamConfig.increaseProbability) / 2){
				// decrease
				price[symbol - 1] -= (r.nextInt(3) + 1);
			}


			volume = r.nextInt(StockStreamConfig.maxVolume) + 1;
			eventType = "stock";
			events[i] = new StockEvent(id, timestamp, symbol, price[symbol - 1], volume, eventType);
		}
		myStream.setEvents(events);


	}

	/**
	 * Generates a series of stock events
	 */
	public void generateStockEvents(){
		Random r = new Random(11);
		StockEvent events[] = new StockEvent[this.size];
		int id;
		int timestamp = 0;
		int symbol;
		int volume;
		int price = r.nextInt(100);
		int random = 0;
		String eventType = "stock";

		for (int i = 0; i < size; i ++){
			id = i;
			timestamp = id;
			symbol = r.nextInt(2); //0 or 1
			random = r.nextInt(100);
			if(random < 55){
				price += r.nextInt(5);
			}else if(random >= 55 && random < 77){
				price -= r.nextInt(5);
			}
			volume = r.nextInt(1000);

			events[i] = new StockEvent(id, timestamp, symbol, price, volume);



		}
		myStream.setEvents(events);


	}

	/**
	 * Generates another batch of stock events
	 * @param number the size of the stream
	 */
	public void generateNextStockEvents(int number){

		StockEvent events[] = new StockEvent[number];
		int id;
		int timestamp = 0;
		int symbol;
		int volume;
		int price = this.randomGenerator.nextInt(100);
		int random = 0;
		String eventType = "stock";

		for (int i = 0; i < number; i ++){
			id = this.eventID;
			timestamp = id;
			symbol = this.randomGenerator.nextInt(2); //0 or 1
			random = this.randomGenerator.nextInt(100);
			if(random < 55){
				price += this.randomGenerator.nextInt(5);
			}else if(random >= 55 && random < 77){
				price -= this.randomGenerator.nextInt(5);
			}
			volume = this.randomGenerator.nextInt(1000);

			events[i] = new StockEvent(id, timestamp, symbol, price, volume);
			this.eventID ++;


		}
		this.myStream = new Stream(number);
		myStream.setEvents(events);
	}

	/**
	 * Generates samestamp ABCEvents for the stream
	 * Dee
	 */
	public void generateABCEvents() {
		//如果用这个流，设为1，用文件中的为0
		if(CommandLineUI.streamControllerSwitch == true){
		//在生成流数据的方法generateABCEvents()中,添加PrintWriter对象:
		PrintWriter pw = null;
		try {
			//写外部文件位置
			pw = new PrintWriter("C:/Users/Dee/Desktop/第一篇论文/layout/events-10w-50%.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// this is for correctness test
			Random r = new Random(11);
			this.events = new ABCEvent[size];
			int id;
			int timestamp = 0;
			int random = 0;
			String eventType = "";
			int price = r.nextInt(100);
			int volume;
			int symbol;

			int counter = 0;
			int repeatCounter = 0;
			int repeatLimit = r.nextInt(6) + 5; // random number between 5 and 10
			int interval = r.nextInt(2) + 2; // random number between 2 and 3

			for (int i = 0; i < size; i++) {
				id = i;
				random = r.nextInt(4);
				switch (random) {
					case 0:
						eventType = "A";
						break;
					case 1:
						eventType = "B";
						break;
					case 2:
						eventType = "C";
						break;
					case 3:
						eventType = "D";
						break;
				}

				random = r.nextInt(100);
				if (random < 55) {
					price += r.nextInt(5);
				} else if (random >= 55 && random < 77) {
					price -= r.nextInt(5);
				}

				volume = r.nextInt(1000);
				symbol = r.nextInt(2); // 0 or 1

				// if counter equals to interval, generate repeated timestamps
				if (counter == interval) {
					if (repeatCounter < repeatLimit) {
						repeatCounter++;
					} else {
						// if enough repeated timestamps are generated, reset counter and generate new interval and repeatLimit
						counter = 0;
						repeatCounter = 0;
						repeatLimit = r.nextInt(6) + 5; // random number between 5 and 10
						interval = r.nextInt(2) + 2; // random number between 2 and 3
					}
				} else {
					timestamp++;
					counter++;
				}
				events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);
				//在生成每个事件对象时,将事件数据写入文件:
				pw.println(id + "," + timestamp + "," + symbol + "," + eventType + "," + price + "," + volume);
			}
		//生成完数据后,关闭PrintWriter:
		pw.close();}
		else{
			try {
				//读外部文件位置
				BufferedReader reader = new BufferedReader(new FileReader("D:\\第一篇论文\\实验\\事件流\\实验图例.txt"));
				//BufferedReader reader = new BufferedReader(new FileReader(args[0]));
				String line;
				int i = 0;
				this.events = new ABCEvent[size];
	//			while ((line = reader.readLine()) != null) {
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");
					int id = Integer.parseInt(parts[0]);
					int timestamp = Integer.parseInt(parts[1]);
					int symbol = Integer.parseInt(parts[2]);
					String eventType = parts[3];
					int price = Integer.parseInt(parts[4]);
					int volume = Integer.parseInt(parts[5]);
					events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);
					i++;
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		myStream.setEvents(events);
	}

	/**
	 * @return the myStream
	 */
	public Stream getMyStream() {
		return myStream;
	}

	/**
	 * @param myStream the myStream to set
	 */
	public void setMyStream(Stream myStream) {
		this.myStream = myStream;
		this.setSize(myStream.getSize());
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Outputs the events in the stream one by one in the console
	 */
	public void printStream(){

			// 输出表头
			System.out.println("ID\tTimestamp\tSymbol\tEvent Type\tPrice\tVolume");

// 输出事件数据

			for (ABCEvent event : this.events) {
				System.out.println(event.getId() + "\t" + event.getTimestamp() + "\t" + event.getSymbol() + "\t"
						+ event.getEventType() + "\t" + event.getPrice() + "\t" + event.getVolume());
			}
		}

	private ABCEvent[] events;

}
