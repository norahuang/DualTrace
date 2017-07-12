package ca.uvic.chisel.bfv.annotations;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayer;

public class RegionAnnotation extends ProjectionAnnotation {
		
		private RegionModel model;
		private IFileModelDataLayer fileModel;
		
		public RegionAnnotation(RegionModel model, IFileModelDataLayer fileModel) {
			super();
			this.model = model;
			this.fileModel = fileModel;
		}

		public RegionModel getModel() {
			return model;
		}
		
		@Override
		public void markCollapsed() {
			super.markCollapsed();
			fileModel.collapseRegion(model);
		}
		
		@Override
		public void markExpanded() {
			super.markExpanded();
			fileModel.expandRegion(model);
		}
		
		@Override
		public String toString() {
			return "[" + model.getStartLine() + "," + model.getEndLine() + "]";
		}
}
