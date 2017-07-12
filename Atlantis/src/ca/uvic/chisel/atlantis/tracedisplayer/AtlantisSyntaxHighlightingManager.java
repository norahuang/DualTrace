package ca.uvic.chisel.atlantis.tracedisplayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.AnnotationPreference;

import bfv.org.eclipse.ui.internal.editors.text.EditorsPlugin;
import bfv.org.eclipse.ui.texteditor.MarkerAnnotation;

import ca.uvic.chisel.bfv.annotations.MarkerAnnotationConstants;
import ca.uvic.chisel.bfv.annotations.TagOccurrence;
import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;
import ca.uvic.chisel.bfv.editor.DefaultSyntaxHighlightingManager;
import ca.uvic.chisel.bfv.projectionsupport.ProjectionViewer;

public class AtlantisSyntaxHighlightingManager extends DefaultSyntaxHighlightingManager {
	
	Map<String, TextAttribute> stylingRules;
	Pattern[] highlightingPatterns;
	
	List<Annotation> annotationsHighlighted = new ArrayList<>();
	
	public AtlantisSyntaxHighlightingManager(ProjectionViewer viewer, IFileModelDataLayer fileModel) {
		super(viewer, fileModel);
		intializeStylingRules();
	}

	/**
	 * Creates the stylingRules map which is used to map pattern matchers to the style that they should receive
	 */
	private void intializeStylingRules() {
		ThreadScanner threadScanner = new ThreadScanner();
		InstructionScanner instructionScanner = new InstructionScanner();
		SystemEventScanner sysEventScanner = new SystemEventScanner();
		stylingRules = threadScanner.getRuleMap();
		stylingRules.putAll(instructionScanner.getRuleMap());
		stylingRules.putAll(sysEventScanner.getRuleMap());
		for(String s : stylingRules.keySet()) {
			highlightingPatterns = ArrayUtils.add(highlightingPatterns, Pattern.compile(s)); 
		}
	}

	/**
	 * See {@link DefaultSyntaxHighlightingManager} for details
	 */
	@Override
	protected void highlightInterval(StyledText textWidget, int modelStartLine, int modelEndLine) {
		List<StyleRange> styleRanges = new ArrayList<>();
		
		modelStartLine = Math.max(0, modelStartLine);
		
		int viewerStartLine = viewer.modelLine2WidgetLine(modelStartLine);
		int viewerEndLine = viewer.modelLine2WidgetLine(modelEndLine);
		
		viewerStartLine = Math.max(viewerStartLine, 0);
		
		for(int lineIndex = viewerStartLine; lineIndex <= viewerEndLine; lineIndex++) {
			
			String line = textWidget.getLine(lineIndex);
			for(Pattern pattern : highlightingPatterns) {
				Matcher matcher = pattern.matcher(line);
				while(matcher.find()) {
					StyleRange range = new StyleRange();
					TextAttribute tokenRule = stylingRules.get(matcher.pattern().toString());
					range.background = tokenRule.getBackground();
					range.foreground = tokenRule.getForeground();
					range.font = tokenRule.getFont();
					range.fontStyle = tokenRule.getStyle();
					range.start = textWidget.getOffsetAtLine(lineIndex) + matcher.start();
					range.length = matcher.end() - matcher.start();
									
					IAnnotationModel annotationModel = viewer.getAnnotationModel();
					
					// Note: the positions of annotations seems to be in the master document space, not the projection space.
					@SuppressWarnings("unchecked")
					Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
					while(annotationIterator.hasNext()){
						Annotation annot = annotationIterator.next();
						
						int modelLineIndex = viewer.widgetLine2ModelLine(lineIndex);
						
						if(annot instanceof TagOccurrence){
							TagOccurrence tag = (TagOccurrence) annot;
							
							boolean tagOverlapsRange = checkAnnotationOverlap(modelLineIndex, matcher.start(), matcher.end(), 
									tag.getStartLine(false), tag.getEndLine(false), tag.getStartChar(false), tag.getEndChar(false));
							
							if(tagOverlapsRange) {
								// Update the StyleRange with our predetermined tag annotation properties
								range.underlineStyle = this.tagAnnotationUnderlineStyle;
								range.underlineColor = this.tagAnnotationUnderlineColor;
								range.underline = true;
							}
						} else if(annot instanceof MarkerAnnotation){

							IMarker marker = ((MarkerAnnotation) annot).getMarker();
							
							// XXX Only marker annotations from tours will have these parameters, this is making the assumption
							// that we only receive marker annotations from tours.  We were having trouble precisely determining
							// a markers location using only default marker parameters.
							int startLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
							int endLine = marker.getAttribute(MarkerAnnotationConstants.END_LINE_NUMBER, -1);
							int startChar = marker.getAttribute(MarkerAnnotationConstants.START_INTRA_LINE_OFFSET, -1);
							int endChar = marker.getAttribute(MarkerAnnotationConstants.END_INTRA_LINE_OFFSET, -1);
							
							boolean annotationOverlapsRange = checkAnnotationOverlap(modelLineIndex, matcher.start(), matcher.end(), startLine, endLine, startChar, endChar);
							
							if(annotationOverlapsRange && startLine != -1 && endLine != -1 && startChar != -1 && endChar != -1) {
								mergeAnnotationAndStyleRange(range, annot);
							}
						}
					}
									
					styleRanges.add(range);
				}
			}
		}
		
		applyStylesToTextWidget(styleRanges, textWidget);
	}

	/**
	 * This method will correctly combine the styles from an annotation (for example a tours marker annotation) 
	 * with the style that is about to be applied by the syntax highlighting manager.  This is very important, since syntax
	 * highlighting a region in an annotation will destroy the annotation's styles otherwise.
	 */
	protected void mergeAnnotationAndStyleRange(StyleRange range, Annotation annot) {
		IPreferenceStore preferenceStore = bfv.org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getPreferenceStore();
		AnnotationPreference pref = EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(annot);
		
		boolean shouldHighlight = preferenceStore.getBoolean(pref.getHighlightPreferenceKey());
		boolean specialHighlight = preferenceStore.getBoolean(pref.getTextPreferenceKey());
		
		if(!shouldHighlight && !specialHighlight) {
			return;
		}
		
		String textStyle = specialHighlight ? preferenceStore.getString(pref.getTextStylePreferenceKey()) : "NONE";
		
		switch(textStyle) {
		case "SQUIGGLES":
			range.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
			range.underlineColor = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			range.underline = true;
			break;
		case "NONE":
			range.background = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			break;
		case "UNDERLINE":
			range.underlineStyle = SWT.UNDERLINE_SINGLE;
			range.underlineColor = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			range.underline = true;
			break;
		case "BOX":
			range.borderStyle = SWT.BORDER_SOLID;
			range.borderColor = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			break;
		case "IBEAM":
			break;
		case "DASHED_BOX":
			range.borderStyle = SWT.BORDER_DASH;
			range.borderColor = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			break;
		case "PROBLEM_UNDERLINE":
			range.underlineStyle = SWT.UNDERLINE_ERROR;
			range.underlineColor = new Color(Display.getCurrent(), getRGBFromColorPref(preferenceStore, pref));
			range.underline = true;
			break;
			
		default:
			break;
		}
		
		annotationsHighlighted.add(annot);
	}

	protected RGB getRGBFromColorPref(IPreferenceStore preferenceStore, AnnotationPreference pref) {
		String colorPref = preferenceStore.getString(pref.getColorPreferenceKey());
		String[] splits = colorPref.split(",");
		RGB rgb = new RGB(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]), Integer.parseInt(splits[2]));
		return rgb;
	}
	
	protected boolean checkAnnotationOverlap(int line, int startChar, int endChar, int annotationStartLine, int annotationEndLine, int annotationStartChar, int annotationEndChar) {
		if(annotationEndLine == line) {
			// return if (0 -> annotationEndChar) overlaps with (startChar -> endChar)
			return startChar <= annotationEndChar;
		} else if(annotationStartLine == line) {
			//return if (annotation startChar -> infinity) overlaps with (startChar -> endChar)
			return endChar >= annotationStartChar;
		} else {
			return annotationStartLine < line && line < annotationEndLine;
		}
	}
}
