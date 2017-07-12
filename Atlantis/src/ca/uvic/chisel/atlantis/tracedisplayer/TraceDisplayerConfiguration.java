package ca.uvic.chisel.atlantis.tracedisplayer;

import ca.uvic.chisel.atlantis.AtlantisActivator;
import ca.uvic.chisel.atlantis.preferences.SyntaxHighlightingPreference;

import java.util.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;

/**
 * Source Viewer Configuration for the Trace Displayer. Used to configure document partitioning, syntax highlighting, and 
 * hover tooltips for annotations.
 * @author Laura Chan
 */
public class TraceDisplayerConfiguration extends SourceViewerConfiguration {
	private static final String INSTRUCTION_SCANNER = "instructionScanner";
	private static final String MODULE_LOAD_UNLOAD_SCANNER = "moduleLoadUnloadScanner";
	private static final String SYSTEM_EVENT_SCANNER = "systemEventScanner";
	private static final String THREAD_SCANNER = "threadScanner";
	
	private Map<String, TraceDisplayerScanner> scanners;
	
	/**
	 * Creates a new TraceDisplayerConfiguration and prepares the rule-based scanners used for syntax highlighting.
	 */
	public TraceDisplayerConfiguration() {
		scanners = new HashMap<String, TraceDisplayerScanner>();
		scanners.put(INSTRUCTION_SCANNER, new InstructionScanner());
		scanners.put(MODULE_LOAD_UNLOAD_SCANNER, new ModuleLoadUnloadScanner());
		scanners.put(SYSTEM_EVENT_SCANNER, new SystemEventScanner());
		scanners.put(THREAD_SCANNER, new ThreadScanner());
	}
	
	/**
	 * Returns a presentation reconciler for the source viewer, with document partitioning and syntax highlighting configured.
	 * @param sourceViewer sourceViewer the source viewer to be configured by this configuration
	 * @return the presentation reconciler to be used
	 */
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		return reconciler;
	}
	
	/**
	 * Returns the document partitioning to be used by the source viewer. 
	 * This implementation always returns the ID for trace partitioning. 
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return the configured partitioning
	 */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return "";
	}
	
	/**
	 * Returns an array of content types to be used by the source viewer. The content types are based on those
	 * defined by the document partitioning.
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return an array of the configured content types
	 */
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		String[] contentTypes = new String[TracePartitionScanner.PARTITION_TYPES.length + 1];
		contentTypes[0] = IDocument.DEFAULT_CONTENT_TYPE;
		for (int i = 1; i < contentTypes.length; i++) {
			contentTypes[i] = TracePartitionScanner.PARTITION_TYPES[i - 1];
		}
		return contentTypes;
	}
	
	/**
	 * Returns the annotation hover to be used to show a tooltip when the user hovers over an annotation in the vertical or 
	 * overview rulers. This implementation just uses a DefaultAnnotationHover instance.
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return a DefaultAnnotationHover instance
	 */
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new DefaultAnnotationHover();
	}
	
	/**
	 * Returns the text hover to be used to show a tooltip when the user hovers over applicable portions of text (e.g.: annotations).
	 * This implementation just uses a DefaultTextHover instance.
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param contentType (not used in this implementation)
	 * @return text hover to be used
	 */
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		// TODO: if a larger tag overlaps with a smaller tag, the hover for the smaller tag may not appear (especially if the larger
		// tag was added later)
		return new DefaultTextHover(sourceViewer);
	}
	
	/**
	 * Used by TraceDisplayer to update syntax highlighting colours when the colour associated with the specified syntax highlighting 
	 * preference has changed.
	 * @param preference preference whose colour has been updated
	 */
	public void updateSyntaxHighlightingColour(SyntaxHighlightingPreference preference) {
		for (TraceDisplayerScanner scanner : scanners.values()) {
			if (scanner.usesSyntaxHighlightingPref(preference)) {
				scanner.updateColour(preference);
			}
		}
	}
}
