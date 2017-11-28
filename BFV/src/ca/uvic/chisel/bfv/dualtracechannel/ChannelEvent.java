package ca.uvic.chisel.bfv.dualtracechannel;

public class ChannelEvent implements Comparable<ChannelEvent> {
		private String functionName;
		private CommunicationStage stage;
		private FullFunctionMatch fullFunctionMatch;
		private Channel channel;

		public enum CommunicationStage {
		    OPENING,
		    DATATRANS,
		    CLOSING;
			
			
		}
		
		
		@SuppressWarnings("unused") // Default constructor is for JAXB's use only--do not use elsewhere!
		private ChannelEvent() {}
		
		public ChannelEvent(String functionName, CommunicationStage stage, FullFunctionMatch fullFunctionMatch, Channel channel) {
			super();		
		    this.functionName = functionName;
			this.stage = stage;
			this.fullFunctionMatch = fullFunctionMatch;
			this.channel = channel;
		}
		
		@SuppressWarnings("restriction")
		@Override
		public String toString() {				
			return stage + ":"+ functionName + "() at line " + this.fullFunctionMatch.getEventStart().getLineElement().getLine();
		}

		
	    public Channel getChannel() {
			return channel;
		}

		public void setChannel(Channel channel) {
			this.channel = channel;
		}

		public String getFunctionName() {
			return functionName;
		}

		public void setFunctionName(String functionName) {
			this.functionName = functionName;
		}

		
		public FullFunctionMatch getFullFunctionMatch() {
			return fullFunctionMatch;
		}

		public void setFullFunctionMatch(FullFunctionMatch fullFunctionMatch) {
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
		public int compareTo(ChannelEvent other) {
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
