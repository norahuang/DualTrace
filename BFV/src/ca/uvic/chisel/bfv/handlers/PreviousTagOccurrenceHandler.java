package ca.uvic.chisel.bfv.handlers;

import ca.uvic.chisel.bfv.views.TagsView;

import org.eclipse.core.commands.*;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for navigating to the previous occurrence of the tag that is currently selected in the Tags View. 
 * Invoked when the Previous Tag Occurrence command is executed.
 * @author Laura Chan
 */
public class PreviousTagOccurrenceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TagsView tagsView = (TagsView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(TagsView.ID);
		if (tagsView != null) {
			tagsView.previousOccurrenceOfSelectedTag();
		}
		return null;
	}
}
