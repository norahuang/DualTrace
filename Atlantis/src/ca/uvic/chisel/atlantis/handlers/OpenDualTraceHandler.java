package ca.uvic.chisel.atlantis.handlers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFileChooser;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MCompositePart;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.impl.PartSashContainerImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.functionparsing.ChannelType;
import ca.uvic.chisel.atlantis.functionparsing.NamedPipeFunctions;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.bfv.BigFileApplication;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.gibraltar.GibraltarMain;

public class OpenDualTraceHandler extends AbstractHandler {
	EModelService ms;
	EPartService ps;
	WorkbenchPage page;

	@SuppressWarnings("restriction")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Show file selector based on the project view, and open the selected
		// trace
		// into a second window, with separate views.
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (editorPart == null) {
			Throwable throwable = new Throwable("No active trace");
			BigFileApplication.showErrorDialog("No active trace", "Please open one trace first", throwable);
			return null;
		}

		MPart container = (MPart) editorPart.getSite().getService(MPart.class);
		MElementContainer m = container.getParent();
		if (m instanceof PartSashContainerImpl) {
			Throwable throwable = new Throwable("The active trace is already opened as dual-trace");
			BigFileApplication.showErrorDialog("The active trace is already opened as dual-trace",
					"This trace can only open as dual-trace with a single active trace.", throwable);
			return null;
		}
		IFile file = getPathOfSelectedFile(event);

		if (null == file) {
			try (Scanner sc = new Scanner(System.in);) {

				JFileChooser f = new JFileChooser(
						"Select a binary trace, to open as a dual trace alongside current active trace");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				File runningDir;
				try {
					runningDir = new File(
							GibraltarMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					f.setCurrentDirectory(runningDir);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}

				f.showOpenDialog(null);

				System.out.println(f.getCurrentDirectory());
				System.out.println(f.getSelectedFile());
				if (null == f.getSelectedFile()) {
					return null;
				}
				file = AtlantisFileUtils.convertFileIFile(f.getSelectedFile());
			}
		}

		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
		try {
			IFileUtils fileUtil = RegistryUtils.getFileUtils();
			File f = BfvFileUtils.convertFileIFile(file);
			f = fileUtil.convertFileToBlankFile(f);
			IFile convertedFile = BfvFileUtils.convertFileIFile(f);
			convertedFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			if (!convertedFile.exists()) {
				fileUtil.createEmptyFile(convertedFile);
			}

			IEditorPart containerEditor = HandlerUtil.getActiveEditorChecked(event);
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			ms = window.getService(EModelService.class);
			ps = window.getService(EPartService.class);
			page = (WorkbenchPage) window.getActivePage();
			IEditorPart editorToInsert = page.openEditor(new FileEditorInput(convertedFile), desc.getId());
			splitEditor(0.5f, 3, editorToInsert, containerEditor, new FileEditorInput(convertedFile));
			window.getShell().layout(true, true);
			createChannelTypeSetting(f);

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void createChannelTypeSetting(File f) {
		IFile file = AtlantisFileUtils.convertFileIFile(f);
		IFile emptyFile = file.getProject()
				.getFile(new Path(BfvFileUtils.TMP_DIRECTORY_NAME + "/" + "channelTypes.json"));
		File jsonFile = AtlantisFileUtils.convertFileIFile(emptyFile);
		if (jsonFile.exists() && !jsonFile.isDirectory()) {
			return; // don't overwrite the existing file.
		}

		AtlantisFileUtils.createChannelTypeSettingFile(jsonFile);
		Gson gson = new GsonBuilder().create();

		// 1. Java object to JSON, and save into a file
		try {
			List<ChannelType> channelTypeList = new ArrayList<ChannelType>();
			channelTypeList.add(NamedPipeFunctions.getInstance().getNamedPipe());

			try (final FileWriter fileWriter = new FileWriter(jsonFile.getAbsolutePath())) {
				gson.toJson(channelTypeList, new TypeToken<List<ChannelType>>() {
				}.getType(), fileWriter);
			}

		} catch (JsonIOException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void splitEditor(float ratio, int where, IEditorPart editorToInsert, IEditorPart containerEditor,
			FileEditorInput newEditorInput) {
		MPart container = (MPart) containerEditor.getSite().getService(MPart.class);
		if (container == null) {
			return;
		}

		MPart toInsert = (MPart) editorToInsert.getSite().getService(MPart.class);
		if (toInsert == null) {
			return;
		}

		MPartStack stackContainer = getStackFor(container);
		MElementContainer<MUIElement> parent = container.getParent();
		int index = parent.getChildren().indexOf(container);
		MStackElement stackSelElement = stackContainer.getChildren().get(index);

		MPartSashContainer psc = ms.createModelElement(MPartSashContainer.class);
		psc.setHorizontal(true);
		psc.getChildren().add((MPartSashContainerElement) stackSelElement);
		psc.getChildren().add(toInsert);
		psc.setSelectedElement((MPartSashContainerElement) stackSelElement);

		MCompositePart compPart = ms.createModelElement(MCompositePart.class);
		compPart.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
		compPart.setCloseable(true);
		compPart.getChildren().add(psc);
		compPart.setSelectedElement(psc);
		compPart.setLabel("dual-trace:" + containerEditor.getTitle() + " and " + editorToInsert.getTitle());

		parent.getChildren().add(index, compPart);
		ps.activate(compPart);

	}

	private MPartStack getStackFor(MPart part) {
		MUIElement presentationElement = part.getCurSharedRef() == null ? part : part.getCurSharedRef();
		MUIElement parent = presentationElement.getParent();
		while (parent != null && !(parent instanceof MPartStack))
			parent = parent.getParent();

		return (MPartStack) parent;
	}

	@Override
	protected void setBaseEnabled(boolean b) {
		return;
	}

	private IFile getPathOfSelectedFile(ExecutionEvent event) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			window = HandlerUtil.getActiveWorkbenchWindow(event);
			IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IFile) {
				return (IFile) firstElement;
			}
			if (firstElement instanceof IFolder) {
				IFolder folder = (IFolder) firstElement;
				AtlantisBinaryFormat binaryFormat = new AtlantisBinaryFormat(
						folder.getRawLocation().makeAbsolute().toFile());
				// arbitrary, just any file in the binary set is needed
				return AtlantisFileUtils.convertFileIFile(binaryFormat.getExecVtableFile());
			}
		}
		return null;
	}
}
