package ca.uvic.chisel.atlantis.datacache;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class AsyncResult<T> {

	private final T result;
	
	private final IStatus status;
	
	public AsyncResult(T result, IStatus status) {
		this.result = result;
		this.status = status;
	}
	
	public AsyncResult(IStatus status) {
		this.result = null;
		this.status = status;
	}

	public T getResult() {
		return result;
	}

	public IStatus getStatus() {
		return status;
	}
	
	public boolean isCancelled() {
		return status == Status.CANCEL_STATUS;
	}
	
	public static <T> AsyncResult<T> cancelled() {
		return new AsyncResult<T>(Status.CANCEL_STATUS);
	}
}
