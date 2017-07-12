package ca.uvic.chisel.bfv.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.internal.navigator.AdaptabilityUtility;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class BigFileOpenActionProvider extends CommonActionProvider {

		protected OpenBigFileAction openFileAction;

		protected ICommonViewerWorkbenchSite viewSite = null;

		protected boolean contribute = false;

		/**
		 * This is kind of a hack, this contains a list of ids which require file conversion to open.
		 * This conversion includes looking into the .tmp directory, and should only be used when opening
		 * a file with the BigFileEditor or subclasses.  If this could be replaced by an instanceof check that would
		 * be awesome.
		 */
		protected Collection<String> editorIdsToConvert = initConversionEditors();

		@Override
		public void init(ICommonActionExtensionSite aConfig) {

			if (aConfig.getViewSite() instanceof ICommonViewerWorkbenchSite) {
				viewSite = (ICommonViewerWorkbenchSite) aConfig.getViewSite();
				openFileAction = new OpenBigFileAction(viewSite.getPage(), editorIdsToConvert);
				contribute = true;
			}
		}

		protected Collection<String> initConversionEditors() {
			ArrayList<String> ids = new ArrayList<String>();
			ids.add(BigFileEditor.ID);
			return ids;
		}

		@Override
		public void fillContextMenu(IMenuManager aMenu) {
			if (!contribute || getContext().getSelection().isEmpty()) {
				return;
			}

			IStructuredSelection selection = (IStructuredSelection) getContext()
					.getSelection();

			openFileAction.selectionChanged(selection);
			if (openFileAction.isEnabled()) {
				aMenu.insertAfter(ICommonMenuConstants.GROUP_OPEN, openFileAction);
			}
			addOpenWithMenu(aMenu);
		}

		@Override
		public void fillActionBars(IActionBars theActionBars) {
			if (!contribute) {
				return;
			}
			IStructuredSelection selection = (IStructuredSelection) getContext()
					.getSelection();
			if (selection.size() == 1
					&& selection.getFirstElement() instanceof IFile) {
				openFileAction.selectionChanged(selection);
				theActionBars.setGlobalActionHandler(ICommonActionConstants.OPEN,
						openFileAction);
			}

		}

		protected void addOpenWithMenu(IMenuManager aMenu) {
			IStructuredSelection ss = (IStructuredSelection) getContext()
					.getSelection();

			if (ss == null || ss.size() != 1) {
				return;
			}

			Object o = ss.getFirstElement();

			// first try IResource
			IAdaptable openable = (IAdaptable) AdaptabilityUtility.getAdapter(o,
					IResource.class);
			// otherwise try ResourceMapping
			if (openable == null) {
				openable = (IAdaptable) AdaptabilityUtility.getAdapter(o,
						ResourceMapping.class);
			} else if (((IResource) openable).getType() != IResource.FILE) {
				openable = null;
			}

			if (openable != null) {
				// Create a menu flyout.
				IMenuManager submenu = new MenuManager(
						WorkbenchNavigatorMessages.OpenActionProvider_OpenWithMenu_label,
						ICommonMenuConstants.GROUP_OPEN_WITH);
				submenu.add(new GroupMarker(ICommonMenuConstants.GROUP_TOP));
				submenu.add(new OpenBigFileWithMenu(viewSite.getPage(), openable, editorIdsToConvert)); 
				submenu.add(new GroupMarker(ICommonMenuConstants.GROUP_ADDITIONS));

				// Add the submenu.
				if (submenu.getItems().length > 2 && submenu.isEnabled()) {
					aMenu.appendToGroup(ICommonMenuConstants.GROUP_OPEN_WITH,
							submenu);
				}
			}
		}

	}
