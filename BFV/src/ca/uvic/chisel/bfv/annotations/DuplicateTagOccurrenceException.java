package ca.uvic.chisel.bfv.annotations;

/**
 * Exception thrown when the user tries to add an occurrence of a tag at a location where there is already an occurrence of that tag.
 * @author Laura Chan
 */
public class DuplicateTagOccurrenceException extends Exception {
	private static final long serialVersionUID = 1080020647213638710L; // keeps the compiler happy

	public DuplicateTagOccurrenceException() {}

	public DuplicateTagOccurrenceException(String message) {
		super(message);
	}

	public DuplicateTagOccurrenceException(Throwable cause) {
		super(cause);
	}

	public DuplicateTagOccurrenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateTagOccurrenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
