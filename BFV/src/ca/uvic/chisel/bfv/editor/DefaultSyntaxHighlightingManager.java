package ca.uvic.chisel.bfv.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.intervaltree.IInterval;
import ca.uvic.chisel.bfv.intervaltree.Interval;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;
import ca.uvic.chisel.bfv.utils.IntervalUtils;
import ca.uvic.chisel.bfv.utils.RegionUtils;

public class DefaultSyntaxHighlightingManager implements ISyntaxHighlightingManager {
	
	/**
	 * This variable is used as an approximation of the number of lines that will be visible in the viewer at any given time
	 */
	protected static final int DEFAULT_PAGE_LENGTH_APPROXIMATION = 50;
	
	/**
	 * This variable defines how far ahead of the top and bottom visible lines we will apply syntax highlighting
	 */
	protected static final int SYNTAX_HIGHLIGHT_LEAD_WIDTH = 15;
	
	protected ProjectionViewer viewer;
	protected List<Interval> highlightedIntervals;
	protected IFileModelDataLayer fileModel;
	
	/* 
	 * These are both needed so we can prevent clobbering of tag annotation styles by syntax highlighting.
	 * They are retrieved at runtime from the plugin.xml, and stored here, so if they are changed later this
	 * will not work out.
	 */
	protected int tagAnnotationUnderlineStyle;
	protected Color tagAnnotationUnderlineColor;
	private boolean highlightOnTextChange = true;
	
	public DefaultSyntaxHighlightingManager(ProjectionViewer viewer, IFileModelDataLayer fileModel) {
		this.fileModel = fileModel;
		highlightedIntervals = new ArrayList<>();
		this.viewer = viewer;
		
		intializeTagAnnotationStyles();
	}
	
	public void beginWatchingViewerTextChanges() {
		viewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				setHighlightingDirty();
				
				if(highlightOnTextChange) {
					adjustHighlighting();
				}
			}
		});
	}

	/**
	 * Removes all highlighted intervals that occur between the startLine and the end Line
	 */
	protected void removeHighlightedIntervals(int startLine, int endLine) {
		List<Interval> toRemove = new ArrayList<>();
		for(Interval highlighted : highlightedIntervals) {
			if(highlighted.intersects(startLine, endLine)) {
				toRemove.add(highlighted);
			}
		}
		
		highlightedIntervals.removeAll(toRemove);
	}

	@Override
	public void adjustHighlighting() {
		int startLine = viewer.getTopIndex();
		int endLine = viewer.getBottomIndex();
		
		// we need to ensure that the interval from startLine to endLine is fully covered by the highlighted intervals + collapsed regions.
		List<IInterval> ignoredHighlightingIntervals = getIgnoredHighlightingIntervals();
		
		// Check to see if the the visible page is either highlighted or hidden by a collapsed region
		if(!IntervalUtils.completelyCovered(new Interval(startLine, endLine), ignoredHighlightingIntervals)) {
			List<Interval> intervalsToHighlight = new ArrayList<>();
			
			intervalsToHighlight.addAll(RegionUtils.pageInToTheRight(startLine, ignoredHighlightingIntervals, SYNTAX_HIGHLIGHT_LEAD_WIDTH + DEFAULT_PAGE_LENGTH_APPROXIMATION, (int)fileModel.getNumberOfLines() - 1));
			if(startLine - 1 > 0){
				List<Interval> pageInTheLeft = RegionUtils.pageInTheLeft(startLine, ignoredHighlightingIntervals, SYNTAX_HIGHLIGHT_LEAD_WIDTH);
				intervalsToHighlight.addAll(pageInTheLeft);
			}
			
			syntaxHighlight(intervalsToHighlight);
		}
	}

	/**
	 * Returns all of the intervals in the document where highlighting does not need to be done.
	 * This includes collapsed regions and already highlighted intervals.
	 */
	protected List<IInterval> getIgnoredHighlightingIntervals() {
		List<Interval> hiddenRegionIntervals = RegionUtils.getCollapsedIntervals(fileModel.getRegions());
		List<IInterval> ignoreHighlightingIntervals = new ArrayList<>();
		ignoreHighlightingIntervals.addAll(hiddenRegionIntervals);
		ignoreHighlightingIntervals.addAll(highlightedIntervals);
		return ignoreHighlightingIntervals;
	}
	
	/**
	 * This method calculates the styles that will be used in order to calculate the styles that should be 
	 * applied to tag annotations in the editor.
	 */
	private void intializeTagAnnotationStyles() {
		// need to look at plugin.xml <extension point="org.eclipse.ui.editors.markerAnnotationSpecification",
		// <specification annotationType="ca.uvic.chisel.bfv.editor.tag" and grab 
		// colorPreferenceValue="128,128,255" and textStylePreferenceValue="SQUIGGLES" from that.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] extensions = registry.getConfigurationElementsFor("org.eclipse.ui.editors.markerAnnotationSpecification");
		for(int w = 0; w < extensions.length; w++){
			IConfigurationElement element = extensions[w];
			if(element.getAttribute("annotationType").equals(TagOccurrence.TAG_ANNOTATION_TYPE)){
				String underlineStyle = element.getAttribute("textStylePreferenceValue");
				switch(underlineStyle){
				case "SQUIGGLE": this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_SQUIGGLE; break;
				case "ERROR": this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_ERROR; break;
				case "SINGLE": this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_SINGLE; break;
				case "DOUBLE": this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_DOUBLE; break;
				case "LINK": this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_LINK; break;
				default: this.tagAnnotationUnderlineStyle = SWT.UNDERLINE_SQUIGGLE; break;
				}
				
				String colorString = element.getAttribute("colorPreferenceValue");
				String[] split = colorString.split(",");
				this.tagAnnotationUnderlineColor = new Color(null, Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
				break;
			}
		}
	}
	
	@Override
	public void setHighlightingDirty() {
		highlightedIntervals.clear();
	}

	/**
	 * This method iterates through the provided intervals, and highlight them one at a time.
	 * It is also responsible for keeping track of which intervals are highlighted.
	 */
	private void syntaxHighlight(List<Interval> intervalsToHighlight) {
		
		StyledText textWidget = viewer.getTextWidget();
		
		for(Interval interval : intervalsToHighlight) {
			highlightInterval(textWidget, (int)interval.getStartValue(), (int)interval.getEndValue());
		}
		
		this.highlightedIntervals.addAll(intervalsToHighlight);
	}

	/**
	 * This is the method responsible for actually performing the syntax highlighting, and is intended
	 * to be extended and customized by anyone intending to extend the plugin.
	 * 
	 * @param textWidget The widget for which the syntax highlighting is getting applied
	 * @param modelStartLine The start of the interval that needs to be highlighted
	 * @param modelEndLine The end of the interval that needs to be highlighted
	 */
	protected void highlightInterval(StyledText textWidget, int modelStartLine, int modelEndLine) {

	}

	protected int getRangeEndFromTag(int endLine, TagOccurrence tag, StyledText textWidget) {
		int tagEnd = viewer.modelLine2WidgetLine(tag.getEndLine());
		if(tagEnd == -1 || tagEnd > endLine)  {
			return textWidget.getOffsetAtLine(endLine + 1) - 1;
		} else {
			return textWidget.getOffsetAtLine(tagEnd) + tag.getEndChar();
		}
	}

	protected int getRangeStartFromTag(int startLine, TagOccurrence tag, StyledText textWidget) {
		int tagStart = viewer.modelLine2WidgetLine(tag.getStartLine());
		if(tagStart == -1 || tagStart < startLine)  {
			return textWidget.getOffsetAtLine(startLine);
		} else {
			return textWidget.getOffsetAtLine(tagStart) + tag.getStartChar();
		}
	}

	@Override
	public void setHighlightOnTextChange(boolean highlight) {
		this.highlightOnTextChange  = highlight;
	}

	protected void applyStylesToTextWidget(List<StyleRange> styleRanges, StyledText textWidget) {
	
		if(styleRanges.isEmpty()) {
			return;
		}
		
		Collections.sort(styleRanges, new Comparator<StyleRange>() {
			@Override
			public int compare(StyleRange o1, StyleRange o2) {
				return Integer.compare(o1.start, o2.start);
			}
		});
		
		StyleRange[] styles = styleRanges.toArray(new StyleRange[styleRanges.size()]);
		int[] ranges = new int[styles.length * 2];
		
		int start = -1;
		int end = -1;
		
		for(int i=0; i < styles.length * 2; i += 2) {
			int stylesIndex = i/2;
			
			if(start == -1) {
				start = styles[stylesIndex].start;
			}
			
			ranges[i] = styles[stylesIndex].start;
			ranges[i+1] = styles[stylesIndex].length;
			
			end = styles[stylesIndex].start + styles[stylesIndex].length;
		}
		
//		textWidget.setStyleRanges(start, end - start + 1, ranges, styles);
		textWidget.setStyleRanges(0, 0, ranges, styles);
	}
}