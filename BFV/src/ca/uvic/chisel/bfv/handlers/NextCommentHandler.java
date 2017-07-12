package ca.uvic.chisel.bfv.handlers;

import ca.uvic.chisel.bfv.views.CommentsView;

import org.eclipse.core.commands.*;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for navigating to the next comment in the group that is currently selected in the Comments View. 
 * Invoked when the Next Comment command is executed.
 * @author Laura Chan
 */
public class NextCommentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CommentsView commentsView = (CommentsView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(CommentsView.ID);
		if (commentsView != null) {
			commentsView.nextCommentInSelectedGroup();
		}
		return null;
	}

}
