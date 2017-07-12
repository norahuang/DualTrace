package ca.uvic.chisel.bfv.editor;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

public class BigFileViewerConfiguration {

	private IFileModelDataLayer fileModel;
	private DocumentContentManager documentManager;

	public BigFileViewerConfiguration(IFileModelDataLayer fileModel, DocumentContentManager documentManager) {
		this.fileModel = fileModel;
		this.documentManager = documentManager;
	}

	public IFileModelDataLayer getFileModel() {
		return fileModel;
	}

	public DocumentContentManager getDocumentManager() {
		return documentManager;
	}
}
