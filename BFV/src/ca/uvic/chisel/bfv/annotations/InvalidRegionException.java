package ca.uvic.chisel.bfv.annotations;

/**
 * Exception thrown when the user tries to create a region with invalid bounds.
 * @author Laura Chan
 */
public class InvalidRegionException extends Exception {
	private static final long serialVersionUID = -3645035720448131262L; // keeps the compiler happy

	public InvalidRegionException() {
	}

	public InvalidRegionException(String message) {
		super(message);
	}

	public InvalidRegionException(Throwable cause) {
		super(cause);
	}

	public InvalidRegionException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidRegionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
