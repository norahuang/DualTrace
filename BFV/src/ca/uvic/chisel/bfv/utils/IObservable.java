package ca.uvic.chisel.bfv.utils;

import java.util.Observable;

public class IObservable extends Observable {
	
	public static enum EventType {
		DocumentUpdated, SearchCompletion
	}
	
	
	public IObservable() {
		super();
	}
	
	@Override
	public void setChanged() {
		super.setChanged();
	}
}
