package ca.uvic.chisel.atlantis.eventtracevisualization;

import java.util.Comparator;

import ca.uvic.chisel.atlantis.models.TraceThreadEvent;

public class ThreadComparator implements Comparator<TraceThreadEvent> {
	@Override
	public int compare(TraceThreadEvent event1, TraceThreadEvent event2) {
		
		int sequenceNumber1 = event1.getLineNumber();
		int sequenceNumber2 = event2.getLineNumber();
		
		if (sequenceNumber1 < sequenceNumber2) {
			return -1;
		} else if (sequenceNumber1 > sequenceNumber2) {
			return 1;
		}
		
		return 0;
	}
}