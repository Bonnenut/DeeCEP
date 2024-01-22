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

import java.util.Random;

/**
 * This class wraps the stream, specifies how to generate or import stream.
 *
 * @author haopeng
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
    public StreamController() {
        eventID = 0;
        randomGenerator = new Random(11);
    }

    /**
     * Constructor, specified size and event type
     *
     * @param size
     * @param eventType
     */
    public StreamController(int size, String eventType) {
        this.size = size;
        myStream = new Stream(size);
        if (eventType.equalsIgnoreCase("abcevent")) {
            this.generateABCEvents();
        }
        if (eventType.equalsIgnoreCase("stockevent")) {
            this.generateStockEvents();
        }
        if (eventType.equalsIgnoreCase("abcconcurrentoderevent")) {
            this.generateABCConcurrentOrderEvents();
        }
        if (eventType.equalsIgnoreCase("abcconcurrentunoderevent")) {
            this.generateABCConcurrentUnorderEvents();
        }
    }

    /**
     * Generates a series of stock events
     */
    public void generateStockEventsAsConfig() {
        Random r = new Random(StockStreamConfig.randomSeed);
        StockEvent events[] = new StockEvent[this.size];
        int id;
        int timestamp = 0;
        int symbol;
        int volume;
        int price = r.nextInt(100);
        int random = 0;
        String eventType = "stock";


        for (int i = 0; i < size; i++) {
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
    public void generateStockEventsAsConfigType() {
        if (StockStreamConfig.increaseProbability > 100) {
            Random r = new Random(StockStreamConfig.randomSeed);
            StockEvent events[] = new StockEvent[this.size];
            int id;
            int timestamp = 0;
            int symbol;
            int volume;
            int price = r.nextInt(100);
            String eventType = "stock";


            for (int i = 0; i < size; i++) {
                id = i;
                timestamp = id;

                symbol = r.nextInt(StockStreamConfig.numOfSymbol) + 1;
                price = r.nextInt(StockStreamConfig.maxPrice) + 1;
                volume = r.nextInt(StockStreamConfig.maxVolume) + 1;
                eventType = "stock" + symbol;
                events[i] = new StockEvent(id, timestamp, symbol, price, volume, eventType);
            }
            myStream.setEvents(events);

        } else {
            this.generateStockEventsWithIncreaseProbability();
        }
    }

    /**
     * Generates a series of stock events
     */
    public void generateStockEventsWithIncreaseProbability() {

        Random r = new Random(StockStreamConfig.randomSeed);
        StockEvent events[] = new StockEvent[this.size];
        int id;
        int timestamp = 0;
        int symbol;
        int volume;
        int price[] = new int[StockStreamConfig.numOfSymbol];
        for (int i = 0; i < StockStreamConfig.numOfSymbol; i++) {
            //initializes the prices of each stock
            price[i] = r.nextInt(1000);
        }

        int random = 0;
        String eventType = "stock";


        for (int i = 0; i < size; i++) {
            id = i;
            timestamp = id;
            symbol = r.nextInt(StockStreamConfig.numOfSymbol) + 1;
            random = r.nextInt(100) + 1;
            if (random <= StockStreamConfig.increaseProbability) {
                //increase
                price[symbol - 1] += (r.nextInt(3) + 1);
            } else if (random > (100 + StockStreamConfig.increaseProbability) / 2) {
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
    public void generateStockEvents() {
        Random r = new Random(11);
        StockEvent events[] = new StockEvent[this.size];
        int id;
        int timestamp = 0;
        int symbol;
        int volume;
        int price = r.nextInt(100);
        int random = 0;
        String eventType = "stock";

        for (int i = 0; i < size; i++) {
            id = i;
            timestamp = id;
            symbol = r.nextInt(2); //0 or 1
            random = r.nextInt(100);
            if (random < 55) {
                price += r.nextInt(5);
            } else if (random >= 55 && random < 77) {
                price -= r.nextInt(5);
            }
            volume = r.nextInt(1000);
            events[i] = new StockEvent(id, timestamp, symbol, price, volume);
        }
        myStream.setEvents(events);
    }


    /**
     * 生成无乱序的并发事件流
     */
	public void generateABCConcurrentOrderEvents() {
		ABCEvent[] events = new ABCEvent[this.size];
		// this is for correctness test
		Random r = new Random(11);
		// 并发时间戳变量
		int concurrentRate = 20; // 时间戳并发率为20%
		int concurrentCount = (int) (size * (concurrentRate / 100.0));
		int normalCount = size - concurrentCount;
		int avgNormalBetweenConcurrents = normalCount / (concurrentCount + 1);
		int normalCounter = 0;
		int concurrentCounter = 0;
		int timestamp = 0;

		// 事件生成变量
		int id;
        String eventType = null;
        int price = r.nextInt(100);
        int volume;
        int symbol;

        for (int i = 0; i < size; i++) {
            id = i;
            int random = r.nextInt(5);

            // 根据随机数决定事件类型
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
                case 4:
                    eventType = "E";
                    break;
            }

            // 随机决定价格变动
            random = r.nextInt(100);
            if (random < 55) {
                price += r.nextInt(5);
            } else if (random >= 55 && random < 77) {
                price -= r.nextInt(5);
            }

            volume = r.nextInt(1000);
            symbol = r.nextInt(2); // 0 或者 1

            // 生成时间戳
            if (concurrentCounter > 0) {
                // 使用之前的时间戳（并发事件）
                concurrentCounter--;
            } else {
                // 在avgNormalBetweenConcurrents个普通时间戳后增加时间戳
                if (normalCounter >= avgNormalBetweenConcurrents) {
                    concurrentCounter = 1; // 准备下一个并发时间戳
                    normalCounter = 0;
                } else {
                    timestamp++;
                    normalCounter++;
                }
            }
            events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);
        }
        myStream.setEvents(events);
    }

    /**
     * 生成乱序的并发事件流
     */
    public void generateABCConcurrentUnorderEvents() {
        Random r = new Random(11);
        ABCEvent[] events = new ABCEvent[this.size];
        int id;
        int timestamp = 0;
        int random = 0;
        String eventType = "";
        int price = r.nextInt(100);
        int volume;
        int symbol;
        int consecutiveCounter = 0;
        int concurrencyRatio = 3;
        int consecutiveConcurrency = 5;

        for (int i = 0; i < size; i++) {
            id = i;
            random = r.nextInt(4);

            // 引入乱序逻辑
            if (i > 0 && i % 3 == 0) {
                timestamp = events[i - 1].getTimestamp(); // 与前一个事件共享相同的时间戳
            } else {
                // 50%的概率使时间戳减小
                if (r.nextBoolean()) {
                    timestamp = Math.max(0, timestamp - r.nextInt(3));
                } else {
                    timestamp = i + 1; // 递增的时间戳
                }
            }

            // 控制局部并发比例
            if (i > 0 && i % concurrencyRatio == 0) {
                // 控制 x 个并发事件之间没有其他事件
                if (consecutiveCounter < consecutiveConcurrency) {
                    timestamp = events[i - 1].getTimestamp(); // 与前一个事件共享相同的时间戳
                    consecutiveCounter++;
                } else {
                    consecutiveCounter = 0;
                }
            }

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
            symbol = r.nextInt(2);

            events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);
        }

        myStream.setEvents(events);
    }


    /**
     * Generates another batch of stock events
     *
     * @param number the size of the stream
     */
    public void generateNextStockEvents(int number) {

        StockEvent events[] = new StockEvent[number];
        int id;
        int timestamp = 0;
        int symbol;
        int volume;
        int price = this.randomGenerator.nextInt(100);
        int random = 0;
        String eventType = "stock";

        for (int i = 0; i < number; i++) {
            id = this.eventID;
            timestamp = id;
            symbol = this.randomGenerator.nextInt(2); //0 or 1
            random = this.randomGenerator.nextInt(100);
            if (random < 55) {
                price += this.randomGenerator.nextInt(5);
            } else if (random >= 55 && random < 77) {
                price -= this.randomGenerator.nextInt(5);
            }
            volume = this.randomGenerator.nextInt(1000);

            events[i] = new StockEvent(id, timestamp, symbol, price, volume);
            this.eventID++;


        }
        this.myStream = new Stream(number);
        myStream.setEvents(events);
    }


    /**
     * Generates ABCEvents for the stream
     */
    public void generateABCEvents() {
        // this is for correctness test
        Random r = new Random(11);
        ABCEvent events[] = new ABCEvent[this.size];
        int id;
        int timestamp = 0;
        int random = 0;
        String eventType = "";
        int price = r.nextInt(100);
        int volume;
        int symbol;
        for (int i = 0; i < size; i++) {
            id = i;
            random = r.nextInt(4);
            timestamp = i;
            switch (random) {
                case 0:
                    eventType = "A";
                    break;
                case 1:
                    eventType = "B";
                    //price += 1;
                    break;
                case 2:
                    eventType = "C";
                    //price += 2;
                    break;
                case 3:
                    eventType = "D";
                    //price += 3;
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

            events[i] = new ABCEvent(id, timestamp, symbol, eventType, price, volume);

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
    public void printStream() {
        for (int i = 0; i < myStream.getSize(); i++) {
            System.out.println("The " + i + " event out of " + size + " events is: " + myStream.getEvents()[i].toString());
        }
    }


}
