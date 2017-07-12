package ca.uvic.chisel.atlantis.tracedisplayer;

import java.util.Collection;

import ca.uvic.chisel.bfv.editor.BigFileOpenActionProvider;

public class AtlantisBigFileOpenActionProvider extends
		BigFileOpenActionProvider {

	@Override
	protected Collection<String> initConversionEditors() {
		Collection<String> editorIds = super.initConversionEditors();
		editorIds.add(AtlantisTraceEditor.ID);
		return editorIds;
	}
	
}
