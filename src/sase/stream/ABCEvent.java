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


/**
 * This class represents a kind of event.
 * @author haopeng
 *
 */
public class ABCEvent implements Event{
	/**
	 * Event id
	 */
	int id;
	
	/**
	 * Event timestamp
	 */
	int timestamp;

	/**
	 * Symbol, an attribute
	 */
	int symbol;
	
	/**
	 * event type
	 */
	String eventType;
	String stateType;
	/**
	 * Price, an attribute
	 */
	int price;

	/**
	 * Volume, an attribute
	 */
	int volume;
	
	/**
	 * Constructor
	 */
	public  ABCEvent(int i, int ts,int s, String et, int p,int v){
		id = i;
		timestamp = ts;
		symbol = s;
		eventType = et;
		price = p;
		volume = v;
	}


	/**


	/**
	 * @return the cloned event
	 */
	@Override
	public Object clone(){
		ABCEvent o = null;
		try {
			o = (ABCEvent)super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * @return the id
	 */
	@Override
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	@Override
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the timestamp
	 */
	@Override
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the eventType
	 */
	@Override
    public String getEventType() {
		return eventType;
	}


	/**
	 * @return the eventType
	 */
	@Override
	public String getStateType() {
		return stateType;
	}



	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * @return the volume
	 */
	public int getVolume() {
		return volume;
	}

	/**
	 * @param volume the volume to set
	 */
	public void setVolume(int volume) {
		this.volume = volume;
	}

	/**
	 * @return the symbol
	 */
	public int getSymbol() {
		return symbol;
	}

	/**
	 * @param symbol the symbol to set
	 */
	public void setSymbol(int symbol) {
		this.symbol = symbol;
	}

	@Override
	public String toString(){
		return "ID="+ id + "    Timestamp=" + timestamp + "    Symbol=" + symbol
			+ "    eventType=" + eventType + "    Price=" + price + "    Volume=" + volume;
	}

	/* (non-Javadoc)
	 * @see edu.umass.cs.sase.mvc.model.Event#getAttributeByName(java.lang.String)
	 */
	@Override
	public int getAttributeByName(String attributeName) {

		if(attributeName.equalsIgnoreCase("volume")) {
			return volume;
		}
		if(attributeName.equalsIgnoreCase("symbol")) {
			return symbol;
		}
		if(attributeName.equalsIgnoreCase("price")) {
			return price;
		}
		if(attributeName.equalsIgnoreCase("id")) {
			return this.id;
		}
		if(attributeName.equalsIgnoreCase("timestamp")) {
			return this.timestamp;
		}

		return -1;
	}

	/* (non-Javadoc)
	 * @see edu.umass.cs.sase.mvc.model.Event#getAttributeByNameString(java.lang.String)
	 */
	@Override
	public String getAttributeByNameString(String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see edu.umass.cs.sase.mvc.model.Event#getAttributeValueType(java.lang.String)
	 */
	@Override
	public int getAttributeValueType(String attributeName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.umass.cs.sase.mvc.model.Event#getAttributeByNameDouble(java.lang.String)
	 */
	@Override
	public double getAttributeByNameDouble(String attributeName) {
		// TODO Auto-generated method stub
		return 0;
	}



	}




	
	
