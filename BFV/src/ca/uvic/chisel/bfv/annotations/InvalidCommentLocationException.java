package ca.uvic.chisel.bfv.annotations;

/**
 * Exception thrown when a user tries to add a comment at a location where there is already another comment from the same group 
 * @author Laura Chan
 */
public class InvalidCommentLocationException extends Exception {
	private static final long serialVersionUID = -4460084763061775787L; // keeps the compiler happy

	public InvalidCommentLocationException() {}

	public InvalidCommentLocationException(String message) {
		super(message);
	}

	public InvalidCommentLocationException(Throwable cause) {
		super(cause);
	}

	public InvalidCommentLocationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidCommentLocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
