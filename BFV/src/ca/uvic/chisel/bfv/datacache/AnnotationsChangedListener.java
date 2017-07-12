package ca.uvic.chisel.bfv.datacache;


public interface AnnotationsChangedListener {

	public static enum EventType {
		CommentsChanged, TagsChanged, MessagesChanged
	}
	
	void handleAnnotationChanged(EventType type);
	
}
