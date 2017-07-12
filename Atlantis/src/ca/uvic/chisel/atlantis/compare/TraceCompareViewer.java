package ca.uvic.chisel.atlantis.compare;

import ca.uvic.chisel.bfv.ImageConstants;
import ca.uvic.chisel.atlantis.database.TraceFileLineDbConnection;
import ca.uvic.chisel.atlantis.handlers.*;

import java.util.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.menus.*;
import org.eclipse.ui.services.IServiceLocator;

import ca.uvic.chisel.bfv.*;
import ca.uvic.chisel.bfv.datacache.FileLine;
import ca.uvic.chisel.bfv.editor.BigFileViewer;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

/**
 * Content Merge Viewer for displaying the results of comparing two traces. This Content Merge Viewer provides toolbar commands for showing the 
 * two traces being compared in full and for showing only the matching or differing portions of the two traces. However, since there isn't a 
 * good way for these commands' handlers to retrieve the active TraceCompareViewer, the toolbar commands also contain a parameter containing
 * the ID of their corresponding TraceCompareViewer. When the commands are executed, their handlers are able to retrieve this ID parameter
 * from the ExecutionEvent and then use this class' getInstance() method to retrieve the corresponding TraceCompareViewer (which will be the 
 * current one). 
 * 
 * TODO this is really hacky code--this should be replaced by a properly implemented version that doesn't require copying and/or modifying the code
 * for org.eclipse.compare.contextmergeviewer.TextMergeViewer and org.eclipse.compare.contextmergeviewer.ContentMergeViewer, and that also doesn't
 * reference internal Eclipse classes.  
 * 
 * @author Laura Chan
 */
@SuppressWarnings("restriction")
public class TraceCompareViewer extends TextMergeViewer {
	
	
	// Stores TraceCompareViewer instances by their IDs for later retrieval by command handlers. 
	private static Map<String, TraceCompareViewer> instances = new HashMap<String, TraceCompareViewer>();
	private static int instanceNumber = 0;
	
	private String id;
	
	// Full contents of left and right trace documents
	private IDocument leftDoc;
	private IDocument rightDoc;
//	public ArrayList<ArrayList<DiffRegion>> results = null;
	public List<DiffRegion> matchingRegions = null;
	public List<DiffRegion> differentRegions = null;
	/*private static final char ANCESTOR_CONTRIBUTOR = 'A';
	private static final char LEFT_CONTRIBUTOR = 'L';
	private static final char RIGHT_CONTRIBUTOR = 'R';
	
	private TraceViewer ancestorTraceViewer;
	private TraceViewer leftTraceViewer;
	private TraceViewer rightTraceViewer;*/
	
	/**
	 * Creates a new TraceCompareViewer.
	 * @param parent the parent control
	 * @param configuration compare configuration to use
	 */
	public TraceCompareViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
	}

	/**
	 * Creates a new TraceCompareViewer.
	 * @param parent the parent control
	 * @param style SWT style bits for the parent control
	 * @param configuration compare configuration to use
	 */
	public TraceCompareViewer(Composite parent, int style, CompareConfiguration configuration) {
		super(parent, style, configuration);
	}
	
	@Override
	protected SourceViewer createSourceViewer(Composite parent, int textOrientation) {
	//	return new ProjectionViewer(parent, new CompositeRuler(), new OverviewRuler(null, 10, EditorsUI.getSharedTextColors()), false, 
	//			textOrientation | SWT.H_SCROLL | SWT.V_SCROLL);
		BigFileViewer viewer =  new BigFileViewer(parent, new CompositeRuler(), new OverviewRuler(null, 10, EditorsUI.getSharedTextColors()), true, 
				textOrientation | SWT.H_SCROLL | SWT.V_SCROLL, true, null);
		return viewer;
	}
	
	@Override
	protected SourceViewer createSourceViewer(Composite parent, int textOrientation, char type) {
	//	return new ProjectionViewer(parent, new CompositeRuler(), new OverviewRuler(null, 10, EditorsUI.getSharedTextColors()), false, 
	//			textOrientation | SWT.H_SCROLL | SWT.V_SCROLL);
		BigFileViewer viewer =  new BigFileViewer(parent, new CompositeRuler(), new OverviewRuler(null, 10, EditorsUI.getSharedTextColors()), true, 
				textOrientation | SWT.H_SCROLL | SWT.V_SCROLL, true, null);
		
	/* I don't think this actually makes any sense
	 * switch(type){
			case ANCESTOR_CONTRIBUTOR:
				ancestorTraceViewer = viewer;
				break;
			case LEFT_CONTRIBUTOR:
				leftTraceViewer = viewer;
				break;
			case RIGHT_CONTRIBUTOR:
				rightTraceViewer = viewer;
				break;
		}*/
		
		return viewer;
	}

	
	@Override
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof ProjectionViewer) {
			// Apply syntax highlighting to trace
			ProjectionViewer viewer = (ProjectionViewer) textViewer;
			this.setEditable(viewer, false);
			//viewer.configure(new TraceDisplayerConfiguration());
		}
	}
	
	@Override
	public String getTitle() {
		return "Trace Compare";
	}
	
	/**
	 * Adds default toolbar buttons and additional buttons for switching between Show Differences, Show Matches and Show All modes.
	 * @param tbm ToolBarManager to which to add the buttons
	 */
	@Override
	protected void createToolItems(ToolBarManager tbm) {
		super.createToolItems(tbm);
		// Make sure that this instance has an ID and is stored in the map of TraceCompareViewer instances
		if (id == null) {
			storeInstance();
		}
		
		// Add toolbar buttons for showing differences, showing matches, and showing everything
		// Also, add the ID of this TraceCompareViewer as a parameter to each command so that their handlers can retrieve it 
		// upon command execution.
		IServiceLocator serviceLocator = this.getCompareConfiguration().getContainer().getServiceLocator();
		CommandContributionItemParameter showAll = new CommandContributionItemParameter(serviceLocator, null, 
				"ca.uvic.chisel.atlantis.commands.ShowAll", CommandContributionItem.STYLE_PUSH);
		showAll.icon = BigFileActivator.getDefault().getImageRegistry().getDescriptor(ImageConstants.ICON_SHOW_ALL);
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(ShowAllHandler.PARAM_TRACE_COMPARE_VIEWER_ID, id);
		showAll.parameters = parameters;
		
		CommandContributionItemParameter showDifferences = new CommandContributionItemParameter(serviceLocator, null, 
				"ca.uvic.chisel.atlantis.commands.ShowDifferences", CommandContributionItem.STYLE_PUSH);
		showDifferences.icon = BigFileActivator.getDefault().getImageRegistry().getDescriptor(ImageConstants.ICON_SHOW_DIFFERENCES);
		parameters = new HashMap<String, String>();
		parameters.put(ShowDifferencesHandler.PARAM_TRACE_COMPARE_VIEWER_ID, id);
		showDifferences.parameters = parameters;
		
		CommandContributionItemParameter showMatches = new CommandContributionItemParameter(serviceLocator, null, 
				"ca.uvic.chisel.atlantis.commands.ShowMatches", CommandContributionItem.STYLE_PUSH);
		showMatches.icon = BigFileActivator.getDefault().getImageRegistry().getDescriptor(ImageConstants.ICON_SHOW_MATCHES);
		parameters = new HashMap<String, String>();
		parameters.put(ShowMatchesHandler.PARAM_TRACE_COMPARE_VIEWER_ID, id);
		showMatches.parameters = parameters;
		
		tbm.appendToGroup("modes", new CommandContributionItem(showAll));
		tbm.appendToGroup("modes", new CommandContributionItem(showDifferences));
		tbm.appendToGroup("modes", new CommandContributionItem(showMatches));
	}
	
	/**
	 * Shows the two traces being compared in their entirety.
	 */
	public void showAll() {
		// Restore full contents of left and right trace documents
		this.getLeftSourceViewer().setDocument(leftDoc, new AnnotationModel());
		this.getRightSourceViewer().setDocument(rightDoc, new AnnotationModel());
		this.getLeftTraceViewer().getSyntaxHighlightingManager().setHighlightingDirty();
		this.getRightTraceViewer().getSyntaxHighlightingManager().setHighlightingDirty();
		this.getLeftTraceViewer().getSyntaxHighlightingManager().adjustHighlighting();
		this.getRightTraceViewer().getSyntaxHighlightingManager().adjustHighlighting();
		fBirdsEyeCanvas.setVisible(true);
//		fCenterDiffCanvas.setVisible(true);
		setHighlightRanges(true);
		//this.leftTraceViewer.setDocument(leftDoc);
		//this.rightTraceViewer.setDocument(rightDoc);
		refreshDiffResults();
	}
	
	public StringBuffer addNewLines(StringBuffer buffer, DiffRegion region, boolean isLeft){
		if(isLeft){
			for(int i = 0; i< region.getRightRegionHeight(); i++){
				buffer.append("...\n");
			}
		}else{
			for(int i = 0; i < region.getLeftRegionHeight(); i++){
				buffer.append("...\n");
			}
		}
		return buffer;
	}
	
	/**
	 * Method for showing only the differences or only the matching portions of the traces being compared.
	 * @param showDifferences if true, shows only the differences; if false, shows only the matching portions
	 * @throws Exception 
	 */
	public void showFiltered(boolean showDifferences) throws Exception {
		String referenceTableName = this.getLeftContributorKey().getName().replace("trace", "original");
		String challengerTableName = this.getRightContributorKey().getName().replace("trace", "original");
		// TODO I needed to get the actual file reference for use with individualized database file per trace.
		// I had to comment this out. The diffs are not production ready at this time,
		// and should be modified to make use of the function call data now available anyway.
		System.out.println("Disabled TextMergeView");
		TraceFileLineDbConnection referenceDB = null; //= new TraceFileLineDbConnection(referenceTableName, null);
		TraceFileLineDbConnection challengerDB = null; // = new TraceFileLineDbConnection(challengerTableName, null);
		matchingRegions = this.getMatches();
		differentRegions = this.getDifferences();
		List<FileLine> fileLine;
			
		StringBuffer left = new StringBuffer();
		StringBuffer right = new StringBuffer();
		
		if(showDifferences){
			for(DiffRegion region : differentRegions){
				if(region.getLeftRegionHeight() == 0){
					left = addNewLines(left, region, true);
				}
				else if(region.getRightRegionHeight() == 0){
					right = addNewLines(right, region, false);
				}
				fileLine = referenceDB.getFileLineRange(region.getLeftStartLine() - 1, region.getLeftEndLine() -1 );
				for(FileLine line : fileLine){
					left.append(line + "\n");
				}
				fileLine = challengerDB.getFileLineRange(region.getRightStartLine() -1 , region.getRightEndLine() - 1);
				for(FileLine line : fileLine){
					right.append(line + "\n");
				}
				left.append("...\n");
				right.append("...\n");
			}
			
		}else{
			for(DiffRegion region : matchingRegions){
				fileLine = referenceDB.getFileLineRange(region.getLeftStartLine() -1, region.getLeftEndLine() -1);
				for(FileLine line : fileLine){
					left.append(line + "\n");
				}
				fileLine = challengerDB.getFileLineRange(region.getRightStartLine() -1, region.getRightEndLine() -1);
				for(FileLine line : fileLine){
					right.append(line + "\n");
				}
				left.append("...\n");
				right.append("...\n");
			}

		}
		
		// Prepare filtered versions of the left and right traces, applying document partitioning to them so syntax highlighting will be done
		Document leftDocument = new Document(left.toString());
		Document rightDocument = new Document(right.toString());
	//	partitionDocument(leftDocument);
	//	partitionDocument(rightDocument);

		// Show the filtered versions of the left and right traces
		this.getLeftSourceViewer().setDocument(leftDocument, new AnnotationModel());
		this.getRightSourceViewer().setDocument(rightDocument, new AnnotationModel());
		this.getLeftTraceViewer().getSyntaxHighlightingManager().setHighlightingDirty();
		this.getRightTraceViewer().getSyntaxHighlightingManager().setHighlightingDirty();
		this.getLeftTraceViewer().getSyntaxHighlightingManager().adjustHighlighting();
		this.getRightTraceViewer().getSyntaxHighlightingManager().adjustHighlighting();
		fBirdsEyeCanvas.setVisible(false);
//		fCenterDiffCanvas.setVisible(false);
		setHighlightRanges(false);
		refreshDiffResults();
	}
	
	/**
	 * Applies document partitioning to the specified document. This allows syntax highlighting to be applied to it.
	 * @param document document to be partitioned
	 */
	private void partitionDocument(Document document) {
		/*FastPartitioner partitioner = new FastPartitioner(Activator.getDefault().getTracePartitionScanner(), 
				TracePartitionScanner.PARTITION_TYPES);
		document.setDocumentPartitioner(Activator.TRACE_PARTITIONING, partitioner);
		partitioner.connect(document);*/
	}
	
	/**
	 * Refreshes diff results and presentation. Called after updating the contents of the left and right source viewers in a show all, 
	 * show matches or show differences operation.
	 */
	private void refreshDiffResults() {
	//	this.doDiff(); // refresh diff results and update presentation
		this.updatePresentation(); // redraw centre control
	}
	
	@Override 
	protected void updateContent(Object ancestor, Object left, Object right) {
		super.updateContent(ancestor, left, right);
		// Store full contents of left and right trace documents 
		leftDoc = this.getLeftSourceViewer().getDocument();
		rightDoc = this.getRightSourceViewer().getDocument();		
	}
	
	@Override
	protected void handleDispose(DisposeEvent event) {
		instances.remove(id);
		super.handleDispose(event);
	}
	
	/**
	 * Generates an ID for this instance and stores it in the instances map so that the instance can be retrieved by ID later.
	 * This allows the handlers for the show all, show differences and show matches commands to retrieve the current 
	 * TraceCompareViewer instance.
	 */
	private void storeInstance() {
		id = "TraceCompareViewer-" + instanceNumber++;
		instances.put(id, this);
	}
	
	/**
	 * Retrieves a TraceCompareViewer instance by its ID. Used by the show all, show differences and show matches handlers to get
	 * the current TraceCompareViewer (whose ID will be stored as a parameter of the command that they are executing)
	 * @param id id of desired (i.e.: current) TraceCompareViewer instance
	 * @return the corresponding TraceCompareViewer, or null if there isn't one with that ID
	 */
	public static TraceCompareViewer getInstance(String id) {
		return instances.get(id);
	}
}
