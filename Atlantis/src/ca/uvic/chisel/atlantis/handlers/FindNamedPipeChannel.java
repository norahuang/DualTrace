package ca.uvic.chisel.atlantis.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.impl.PartSashContainerImpl;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import ca.uvic.chisel.atlantis.views.DualTraceChannelView;
import ca.uvic.chisel.bfv.BigFileApplication;

public class FindNamedPipeChannel extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		MPart container = (MPart) editorPart.getSite().getService(MPart.class);
		MElementContainer m = container.getParent();
		if (!(m instanceof PartSashContainerImpl)) {
			Throwable throwable = new Throwable("This is not a dual-trace");
			BigFileApplication.showErrorDialog("This is not a dual-trace!", "Open a dual-trace First", throwable);
			return null;
		}

		MPart editorPart1 = (MPart) m.getChildren().get(0);
		MPart editorPart2 = (MPart) m.getChildren().get(1);
		if (editorPart1.getObject() instanceof CompatibilityEditor
				&& editorPart2.getObject() instanceof CompatibilityEditor) {
			IEditorPart editor1 = ((CompatibilityEditor) editorPart1.getObject()).getEditor();
			IEditorPart editor2 = ((CompatibilityEditor) editorPart2.getObject()).getEditor();
			DualTraceChannelView dualTraceChannelView = (DualTraceChannelView) HandlerUtil
					.getActiveWorkbenchWindow(event).getActivePage().findView(DualTraceChannelView.ID);
			if (dualTraceChannelView != null) {
				dualTraceChannelView.getNamedPipeChannels(true, editor1, editor2);
				//dualTraceChannelView.updateView();
			}

		}

		return null;
	}

}
