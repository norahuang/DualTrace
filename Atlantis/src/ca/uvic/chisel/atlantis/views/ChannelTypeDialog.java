package ca.uvic.chisel.atlantis.views;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.impl.PartSashContainerImpl;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import com.google.gson.Gson;

import ca.uvic.chisel.atlantis.functionparsing.ChannelType;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.bfv.BigFileApplication;

/**
 * Dialog for adding a tag to the file.
 * 
 * @author Nora Huang
 */
public class ChannelTypeDialog extends TitleAreaDialog {

	public static final String ID = "ca.uvic.chisel.bfv.views.ChannelTypeDialog";
	protected CheckboxTreeViewer treeViewer;

	/**
	 * Constructs a new dialog for adding or editing a comment.
	 * 
	 * @param parentShell
	 *            parent shell widget for this dialog
	 * @param commentToEdit
	 *            comment to edit if this should be an edit dialog, or null if a
	 *            new comment is to be added
	 */
	public ChannelTypeDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Select Channel Types:");
	}

	private class ChannelTypeContentProvider implements ITreeContentProvider {
		private ArrayList<String> ChannelTypes;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				ChannelTypes = new ArrayList<String>(); // clear out the old
														// data
				List<?> input = (List<?>) newInput;
				for (Object o : input) { // add in the new data
					ChannelTypes.add((String) o);
				}
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ChannelTypes.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	/**
	 * Creates the dialog area, which contains a combobox for specifying the
	 * comment's group and a text field for entering/editing the comment's text
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		treeViewer = new CheckboxTreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		treeViewer.getTree().setHeaderVisible(false);
		treeViewer.getTree().setLinesVisible(false);
		treeViewer.setContentProvider(new ChannelTypeContentProvider());
		treeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				int s1 = e1.toString().length();
				int s2 = e2.toString().length();
				return Integer.compare(s1, s2);
			};
		});

		List<String> channelTypes = getChannelTypeSetting();
		treeViewer.setInput(channelTypes);

		GridData tableData = new GridData();
		tableData.grabExcessHorizontalSpace = true;
		tableData.grabExcessVerticalSpace = true;
		tableData.horizontalSpan = 2;
		tableData.horizontalAlignment = GridData.FILL;
		tableData.verticalAlignment = GridData.FILL;
		treeViewer.getTree().setLayoutData(tableData);

		return parent;
	}

	private List<String> getChannelTypeSetting() {
		AtlantisTraceEditor activeTraceDisplayer = (AtlantisTraceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		File f = activeTraceDisplayer.getEmptyFile();
		File jsonFile = AtlantisFileUtils.getChannelTypeSettingFile(f);
		Gson gson = new Gson();
		List<String> channelTypeNameList = new ArrayList<String>();
		
		try ( final FileReader fileReader = new FileReader(jsonFile.getAbsolutePath()) ) {
			ChannelType[] channelTypeList = gson.fromJson(fileReader, ChannelType[].class);
			for(ChannelType ct: channelTypeList){
				channelTypeNameList.add(ct.getChannelTypeName());
				System.out.println(ct.getChannelTypeName());
			}
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(channelTypeNameList.size()>1){
			channelTypeNameList.add("ALL");
		}
		return channelTypeNameList;

	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(new GridLayout(1, true));

		GridData okGridData = new GridData();
		okGridData.horizontalAlignment = SWT.FILL;
		okGridData.grabExcessHorizontalSpace = true;

		final Button ok = new Button(parent, SWT.PUSH); // done manually so we
														// can implement a
														// custom selection
														// listener without
														// clashing with the
														// default one
		ok.setLayoutData(okGridData);
		ok.setText(IDialogConstants.OK_LABEL);
		ok.setData(new Integer(IDialogConstants.OK_ID)); // sets the button's
															// ID; needed since
															// we're not using
															// createButton()
		ok.setEnabled(false);

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object[] selection = treeViewer.getCheckedElements();
				if (selection.length > 0) {
					ok.setEnabled(true);
				} else {
					ok.setEnabled(false);
				}
			}
		});

		ok.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				okPressed();
			}
		});
		// Set the OK button to be the default button
		Shell shell = parent.getShell();
		if (shell != null) {
			shell.setDefaultButton(ok);
		}

		Button cancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		GridData cancelGridData = new GridData();
		cancelGridData.horizontalAlignment = SWT.FILL;
		cancelGridData.grabExcessHorizontalSpace = true;
		cancel.setLayoutData(cancelGridData);
	}


}
