package ca.uvic.chisel.bfv.dualtracechannelmatch;

import ca.uvic.chisel.bfv.dualtracechannel.CommunicationStage;
import ca.uvic.chisel.bfv.dualtracechannel.FunctionType;

public class ChannelOpenCloseEvent implements Comparable<ChannelOpenCloseEvent> {
		private String functionName;
		private CommunicationStage stage;
		private FullFunctionMatchOfTrace fullFunctionMatch;
		private MatchChannel channel;
		private String traceName;

		
		@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
		private ChannelOpenCloseEvent() {}
		
		public ChannelOpenCloseEvent(String functionName, CommunicationStage stage, FullFunctionMatchOfTrace fullFunctionMatch, MatchChannel channel, String traceName) {
			super();		
		    this.functionName = functionName;
			this.stage = stage;
			this.fullFunctionMatch = fullFunctionMatch;
			this.channel = channel;
			this.traceName = traceName;
		}
		
		@SuppressWarnings("restriction")
		@Override
		public String toString() {		
			String datatran = "";
			if(stage == CommunicationStage.DATATRANS)
			{
			    if(fullFunctionMatch.getType()==FunctionType.send){
			    	datatran = ",send";
			    }	
			    else{
			    	datatran = ",receive";
			    }
			}
			return stage + datatran + ":"+ functionName + "() in "+ this.traceName +" at line " + this.fullFunctionMatch.getEventStart().getLineElement().getLine();
		}

		
	    public MatchChannel getChannel() {
			return channel;
		}

		public void setChannel(MatchChannel channel) {
			this.channel = channel;
		}

		public String getFunctionName() {
			return functionName;
		}

		public void setFunctionName(String functionName) {
			this.functionName = functionName;
		}

		
		public FullFunctionMatchOfTrace getFullFunctionMatch() {
			return fullFunctionMatch;
		}

		public void setFullFunctionMatch(FullFunctionMatchOfTrace fullFunctionMatch) {
			this.fullFunctionMatch = fullFunctionMatch;
		}

		public CommunicationStage getStage() {
			return stage;
		}

		public void setStage(CommunicationStage stage) {
			this.stage = stage;
		}


		/**
		 * Compares the start and end location of this tag occurrence to another occurrence. Does not check the occurrences' associated tags.
		 * @param other tag occurrence to compare this occurrence to
		 * @return -1 if this occurrence's location is before the other's, 1 if it is after the other's, or 0 if they have the same location
		 */
		@Override
		public int compareTo(ChannelOpenCloseEvent other) {
			if (other == null) {
				return -1;
			} 
			
			if (this.fullFunctionMatch.getEventStart().getLineElement().getLine() < other.fullFunctionMatch.getEventStart().getLineElement().getLine()) {
				return -1;
			} else if (this.fullFunctionMatch.getEventStart().getLineElement().getLine() > other.fullFunctionMatch.getEventStart().getLineElement().getLine()) {
				return 1;
			} // else, both start on the same line, so check the start char next			
            return 0;
		}


}
