package ca.uvic.chisel.bfv.editor;

import org.eclipse.jface.text.IDocument;
import bfv.org.eclipse.ui.editors.text.FileDocumentProvider;

public class BfvDocumentProvider extends FileDocumentProvider {
	private DocumentContentManager manager;

	public BfvDocumentProvider(DocumentContentManager manager) {
		super();
		this.manager = manager;
	}
	
	@Override
	public IDocument getDocument(Object element) {
		// if we are here, we are probably just going to return the content managers document
		return manager.getDocument();
	}
}
