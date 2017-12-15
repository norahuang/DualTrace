package ca.uvic.chisel.bfv.dualtracechannelmatch;

public class ChannelDataTransEvent implements Comparable<ChannelDataTransEvent> {
		private FullFunctionMatchOfTrace send;
		private FullFunctionMatchOfTrace recv;
		private String messsage;
		private MatchChannel channel;
		


		public ChannelDataTransEvent(FullFunctionMatchOfTrace send, FullFunctionMatchOfTrace recv, String message, MatchChannel channel) {
			super();
			this.send = send;
			this.recv = recv;
			this.messsage = message;
			this.channel = channel;
		}
		
		public String toString(){
			return "DATATRANS:"+messsage;
		}
		
		@Override
		public int compareTo(ChannelDataTransEvent other) {
			if (other == null) {
				return -1;
			} 
			
			if (this.send.getTraceName() == other.send.getTraceName()){
				if (this.send.getEventStart().getLineElement().getLine() < other.send.getEventStart().getLineElement().getLine()) {
					return -1;
				} else if (this.send.getEventStart().getLineElement().getLine() > other.send.getEventStart().getLineElement().getLine()) {
					return 1;
				} // else, both start on the same line, so check the start char next			
	            return 0;
			} else if(this.send.getTraceName() == other.recv.getTraceName()){
				if (this.send.getEventStart().getLineElement().getLine() < other.recv.getEventStart().getLineElement().getLine()) {
					return -1;
				} else if (this.send.getEventStart().getLineElement().getLine() > other.recv.getEventStart().getLineElement().getLine()) {
					return 1;
				} // else, both start on the same line, so check the start char next			
	            return 0;
			} else
			{
				return -1;
			}
		}
		
		public FullFunctionMatchOfTrace getSend() {
			return send;
		}
		public void setSend(FullFunctionMatchOfTrace send) {
			this.send = send;
		}
		public FullFunctionMatchOfTrace getRecv() {
			return recv;
		}
		public void setRecv(FullFunctionMatchOfTrace recv) {
			this.recv = recv;
		}
		
		public String getMesssage() {
			return messsage;
		}

		public void setMesssage(String messsage) {
			this.messsage = messsage;
		}

		public MatchChannel getChannel() {
			return channel;
		}

		public void setChannel(MatchChannel channel) {
			this.channel = channel;
		}

}
