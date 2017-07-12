package ca.uvic.chisel.bfv.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.projection.ProjectionDocument;
import org.eclipse.jface.text.projection.ProjectionDocumentManager;

/**
 * This class is responsible for creating our own custom ProjectionDocument for the projection annotation stuff.
 * The reason we do this is that we need to inject our own line tracker into the document in order
 * to ensure that we do not use a TreeLineTracker, which consumes a ton of memory.
 */
public class BigFileSlaveDocumentManager extends ProjectionDocumentManager {

	private DocumentContentManager contentManager;
	private BigFileProjectionDocument slaveDoc;

	public BigFileSlaveDocumentManager() {
	}
	
	public void setContentManager(DocumentContentManager contentManager) {
		this.contentManager = contentManager;
	}
	
	@Override
	protected ProjectionDocument createProjectionDocument(IDocument master) {
		slaveDoc = new BigFileProjectionDocument(master, contentManager);
		return slaveDoc;
	}
}
