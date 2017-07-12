package ca.uvic.chisel.bfv.utils;

import javax.management.RuntimeErrorException;

public class TimeAccumulator {

	long startTime = -1;
	long lastCheckin = -1;
	long lastCheckout = -1;
	long sum = 0;
	
	boolean in = false;
	
	public void checkIn(){
		if(in){
			throw new RuntimeErrorException(null, "Timer afoul, in called when already in.");
		}
		in = true;
		this.lastCheckin = System.currentTimeMillis();
		if(-1 == this.startTime){
			this.startTime = System.currentTimeMillis();
		}
	}

	public void checkOut(){
		// I would have an error here too, but I want to be able to call close safely and with impunity.
		// As long as it doesn't break timing, I shall call out freely.
		if(!in){
			throw new RuntimeErrorException(null, "Timer afoul, out called when already out.");
		}
		in = false;
		
		long checkout = System.currentTimeMillis();
		this.sum += checkout - this.lastCheckin;
	}
	
	public long getSeconds(){
		return this.sum/1000;
	}
	
}
