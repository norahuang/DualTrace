package ca.uvic.chisel.atlantis.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.impl.PartSashContainerImpl;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import ca.uvic.chisel.bfv.BigFileApplication;

public class FindChannelTypeDialog extends ChannelTypeDialog{
	
	public FindChannelTypeDialog(Shell parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void okPressed() {
		Map<String,String> selectedChannels = new HashMap<String,String>();
		for (Object selection : treeViewer.getCheckedElements()) {
			if(((String)selection).equals("ALL")){
				for (Object item : treeViewer.getExpandedElements()){
					selectedChannels.put((String)item, (String)selection);
				}
				break;
			}
			selectedChannels.put((String)selection, (String)selection);
		}
		findChannels(new ArrayList<String>(selectedChannels.values()));
		super.okPressed();
	}

	private void findChannels(List<String> selectedChannels) {
		IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		MPart container = (MPart) editorPart.getSite().getService(MPart.class);
		MElementContainer m = container.getParent();
		if (!(m instanceof PartSashContainerImpl)) {
			Throwable throwable = new Throwable("This is not a dual-trace");
			BigFileApplication.showErrorDialog("This is not a dual-trace!", "Open a dual-trace First", throwable);
			return;
		}

		MPart editorPart1 = (MPart) m.getChildren().get(0);
		MPart editorPart2 = (MPart) m.getChildren().get(1);
		if (editorPart1.getObject() instanceof CompatibilityEditor
				&& editorPart2.getObject() instanceof CompatibilityEditor) {
			IEditorPart editor1 = ((CompatibilityEditor) editorPart1.getObject()).getEditor();
			IEditorPart editor2 = ((CompatibilityEditor) editorPart2.getObject()).getEditor();
			DualTraceChannelView dualTraceChannelView = (DualTraceChannelView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().findView(DualTraceChannelView.ID);
			if (dualTraceChannelView != null) {
				dualTraceChannelView.getChannels(selectedChannels, editor1, editor2);
			}

		}
	}


}
