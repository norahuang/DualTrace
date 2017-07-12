package ca.uvic.chisel.atlantis.views;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ViewPart;

import ca.uvic.chisel.atlantis.database.InstructionId;
import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;
import ca.uvic.chisel.atlantis.functionparsing.FunctionTreeContentProvider;
import ca.uvic.chisel.atlantis.functionparsing.FunctionTreeContentProvider.FunctionOrModule;
import ca.uvic.chisel.atlantis.functionparsing.FunctionTreeLabelProvider;
import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.atlantis.tracedisplayer.AtlantisTraceEditor;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.annotations.InvalidCommentLocationException;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.views.CommentsView;

public class FunctionsView extends ViewPart implements IPartListener2 {

	public static final String ID = "ca.uvic.chisel.atlantis.views.FunctionsView";

	private TreeViewer treeViewer;
	private Tree treeControl;

	private Action findFunctionStartsAction;
	private Action findFunctionCallersAction;
	private Action recompositeFunctionAction;
	private Action addMessageTypeAction;

	private AtlantisFileModelDataLayer fileModel = null;

	private AtlantisTraceEditor activeTraceDisplayer = null;

	public FunctionsView() {

	}

	private boolean reacquireFileModel() {
		try {
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			activeTraceDisplayer = (AtlantisTraceEditor) activeWorkbenchWindow.getActivePage().getActiveEditor();
			fileModel = (AtlantisFileModelDataLayer) RegistryUtils
					.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());

		} catch (Exception e) {
			// TODO we need better error handling
			return false;
		}
		return true;
	}

	@Override
	public void createPartControl(Composite parent) {
		FillLayout layout = new FillLayout();
		parent.setLayout(layout);

		createControls(parent);
		createContextMenu();

		this.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
	}

	@Override
	public void setFocus() {
		// viewer.getControl().setFocus();
	}

	private void createContextMenu() {

		createActions();

		// Create menu manager.
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});

		// Create menu.
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void fillContextMenu(IMenuManager mgr) {
		FunctionTreeContentProvider.FunctionOrModule fom = (FunctionTreeContentProvider.FunctionOrModule) treeViewer
				.getTree().getSelection()[0].getData();

		if (fom.isFunction()) {
			mgr.add(findFunctionCallersAction);
			mgr.add(findFunctionStartsAction);
			mgr.add(recompositeFunctionAction);
			mgr.add(addMessageTypeAction);
		}
	}

	public void createActions() {
		findFunctionCallersAction = new Action("Find All Callers of Function in Trace Viewer") {
			@Override
			public void run() {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				AtlantisSearchView search = (AtlantisSearchView) page.findView(AtlantisSearchView.ID);

				FunctionTreeContentProvider.FunctionOrModule fom = (FunctionTreeContentProvider.FunctionOrModule) treeViewer
						.getTree().getSelection()[0].getData();

				if (!fom.isFunction()) {
					throw new RuntimeException(
							"Find Start Instruction action called on module, should not be possible");
				}

				Instruction startInstruction = fom.getFunction().getFirst();

				search.searchFunctions(startInstruction.getIdGlobalUnique(), true);

			}
		};

		findFunctionStartsAction = new Action("Find Start Instructions in Trace Viewer") {
			@Override
			public void run() {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				AtlantisSearchView search = (AtlantisSearchView) page.findView(AtlantisSearchView.ID);

				FunctionTreeContentProvider.FunctionOrModule fom = (FunctionTreeContentProvider.FunctionOrModule) treeViewer
						.getTree().getSelection()[0].getData();

				if (!fom.isFunction()) {
					throw new RuntimeException(
							"Find Start Instruction action called on module, should not be possible");
				}

				Instruction startInstruction = fom.getFunction().getFirst();

				search.searchFunctions(startInstruction.getIdGlobalUnique(), false);

			}
		};

		recompositeFunctionAction = new Action("Perform Static Code Recomposition") {
			@Override
			public void run() {
				try {
					RecompositionView compView = (RecompositionView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage().showView(RecompositionView.ID);
					if (compView != null) {

						FunctionTreeContentProvider.FunctionOrModule fom = (FunctionTreeContentProvider.FunctionOrModule) treeViewer
								.getTree().getSelection()[0].getData();

						if (!fom.isFunction()) {
							throw new RuntimeException("Recomposite action called on module, should not be possible");
						}

						compView.recomposite(fom.getFunction());
					}
				} catch (PartInitException e1) {
					// ok if it fails
				}
			}
		};

		addMessageTypeAction = new Action("Add To MessageType") {
			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				MessageTypesView messageTypesView = (MessageTypesView) page.findView(MessageTypesView.ID);
				System.out.println("add message type");

				if (messageTypesView != null) {

					FunctionTreeContentProvider.FunctionOrModule fom = (FunctionTreeContentProvider.FunctionOrModule) treeViewer
							.getTree().getSelection()[0].getData();

					if (!fom.isFunction()) {
						throw new RuntimeException("Add To Messages Matching called on module, should not be possible");
					}
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					String functionName = fom.getFunction().getName();
					if (functionName.equals("") )
					{
						functionName = fom.toString();
					}
					AddMessageTypeDialog dialog = new AddMessageTypeDialog(shell, fom.getFunction().getFirst(), functionName);
					dialog.create();

					if (dialog.open() == Window.OK) {
						messageTypesView.updateView();
					}

				}
			}
		};
	}

	public void createControls(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		treeControl = treeViewer.getTree();
	}

	public void updateForLineChange() {

	}

	public void refresh() {
		treeViewer.refresh();
	}

	public void clearContents() {

	}

	@Override
	public void partActivated(IWorkbenchPartReference arg0) {
		// if(!reacquireFileModel()){
		// return;
		// }

		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				if (part != activeTraceDisplayer) { // don't need to redraw the
													// visualization if the
													// active Trace Displayer
													// hasn't changed
					activeTraceDisplayer = (AtlantisTraceEditor) part;
					fileModel = (AtlantisFileModelDataLayer) RegistryUtils
							.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());

					FunctionTreeContentProvider contentProvider = new FunctionTreeContentProvider(fileModel);
					treeViewer.setContentProvider(contentProvider);
					treeViewer.setLabelProvider(new FunctionTreeLabelProvider());
					treeViewer.setInput(contentProvider.genFOM(""));

					refresh();

				}
			}
		}
	}

	public void selectParentFunctionOfCurrentEditorLineSelection(int currentLine) {
		IEditorPart part = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (part instanceof IEditorPart) {
			if (part instanceof AtlantisTraceEditor) {
				activeTraceDisplayer = (AtlantisTraceEditor) part;
				fileModel = (AtlantisFileModelDataLayer) RegistryUtils
						.getFileModelDataLayerFromRegistry(activeTraceDisplayer.getCurrentBlankFile());
				Instruction inst = fileModel.getInstructionDb().getInstructionByLineNumber(currentLine);
				if (null == inst) {
					BigFileApplication.showInformationDialog("No Instruction and Function Associated",
							"There is no actual instruction associated with this trace line (" + currentLine
									+ "), and it cannot be linked to the Function View.");
					return;
				}
				InstructionId parentId = inst.getParentFunction();
				if (null == parentId.toString()) {
					BigFileApplication.showInformationDialog("No Function Associated",
							"There is no actual function associated with this trace line (" + currentLine + ": "
									+ inst.toString() + "), and it cannot be linked to the Function View.");
					return;
				}
				// Now, track down the selection that matches the parent
				// function id
				for (Object module : ((ITreeContentProvider) treeViewer.getContentProvider())
						.getElements(treeViewer.getInput())) {
					if (((FunctionOrModule) module).getModule().equals(inst.getModule())) {
						Object[] functions = ((ITreeContentProvider) treeViewer.getContentProvider())
								.getChildren(module);
						for (Object function : functions) {
							FunctionOrModule fm = (FunctionOrModule) function;
							if (fm.getFunction().getFirst().getIdGlobalUnique().equals(parentId)) {
								treeViewer.expandToLevel(function, AbstractTreeViewer.ALL_LEVELS);
								StructuredSelection structuredSelections = new StructuredSelection(function);
								treeViewer.setSelection(structuredSelections, true);
								return;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference arg0) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference arg0) {
	}
}
