package ca.uvic.chisel.bfv.dualtrace;

/**
 * Exception thrown when the user tries to add an occurrence of a tag at a location where there is already an occurrence of that tag.
 * @author Laura Chan
 */
public class DuplicateMessageOccurrenceException extends Exception {
	private static final long serialVersionUID = 1080020647213638710L; // keeps the compiler happy

	public DuplicateMessageOccurrenceException() {}

	public DuplicateMessageOccurrenceException(String message) {
		super(message);
	}

	public DuplicateMessageOccurrenceException(Throwable cause) {
		super(cause);
	}

	public DuplicateMessageOccurrenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateMessageOccurrenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
