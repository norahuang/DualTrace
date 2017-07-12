package ca.uvic.chisel.atlantis.recomposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.zest.layouts.dataStructures.DisplayIndependentPoint;
import org.eclipse.zest.layouts.dataStructures.InternalNode;
import org.eclipse.zest.layouts.dataStructures.InternalRelationship;

public class OverlapRemover extends AbstractLayoutAlgorithm {

	private final int margin;
	
	public OverlapRemover(int styles, int margin) {
		super(styles);
		
		this.margin = margin;
	}

	@Override
	protected void applyLayoutInternal(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double boundsX, double boundsY, double boundsWidth,
			double boundsHeight) {
		
		Map<Double, List<InternalNode>> nodesByXPosition = new TreeMap<Double, List<InternalNode>>();
		
		// separate the nodes by x position
		for(InternalNode node : entitiesToLayout) {
			if(!nodesByXPosition.containsKey(node.getInternalLocation().x)) {
				nodesByXPosition.put(node.getInternalLocation().x, new ArrayList<InternalNode>());
			}
			
			nodesByXPosition.get(node.getInternalLocation().x).add(node);
		}
		
		for(List<InternalNode> nodes : nodesByXPosition.values()) {
			// sort the nodes in this x offset by y position
			Collections.sort(nodes, new Comparator<InternalNode>() {
				@Override
				public int compare(InternalNode node1, InternalNode node2) {
					double yDifference = node1.getInternalLocation().y - node2.getInternalLocation().y;
					
					if(yDifference < 0.0) {
						return -1;
					} else if(yDifference > 0.0) {
						return 1;
					}
					
					return 0;
				}
			});
			
			// offset the nodes vertically so that they don't overlap
			for(int i = 1; i < nodes.size(); i++) {
				DisplayIndependentPoint upperPosition = nodes.get(i - 1).getInternalLocation();
				DisplayIndependentPoint lowerPosition = nodes.get(i).getInternalLocation();
				double upperHeight = nodes.get(i - 1).getHeightInLayout();
				
				if(upperPosition.y + upperHeight + margin > lowerPosition.y) {	
					nodes.get(i).setInternalLocation(lowerPosition.x, upperPosition.y + upperHeight + margin);
				}
			}
		}
		
		// now order the lists of x partitions by x value
		List<List<InternalNode>> xPartitions = new ArrayList<List<InternalNode>>();
		for(List<InternalNode> nodeList : nodesByXPosition.values()) {
			xPartitions.add(nodeList);
		}
		
		Collections.sort(xPartitions, new Comparator<List<InternalNode>>() {
			@Override
			public int compare(List<InternalNode> partition1, List<InternalNode> partition2) {
				double xDifference = partition1.get(0).getInternalLocation().x - partition2.get(0).getInternalLocation().x;
				
				if(xDifference < 0.0) {
					return -1;
				} else if(xDifference > 0.0) {
					return 1;
				}
				
				return 0;
			}
		});
		
		// offset each partition horizontally by the maximum width of the previous partition plus margin
		int currentXOffset = margin;
		
		for(int i = 0; i < xPartitions.size(); i++) {
			
			double maxWidth = 0;
			for(int j = 0; j < xPartitions.get(i).size(); j++) {
				
				xPartitions.get(i).get(j).setInternalLocation(currentXOffset,
						xPartitions.get(i).get(j).getInternalLocation().y);
				
				double currentWidth = xPartitions.get(i).get(j).getWidthInLayout();
				if(currentWidth > maxWidth) {
					maxWidth = currentWidth;
				}
			}
			
			currentXOffset += maxWidth + margin;
		}
	}

	@Override
	protected int getCurrentLayoutStep() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getTotalNumberOfLayoutSteps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean isValidConfiguration(boolean asynchronous, boolean continuous) {
		if (asynchronous && continuous) {
			return false;
		} else if (asynchronous && !continuous) {
			return true;
		} else if (!asynchronous && continuous) {
			return false;
		} else if (!asynchronous && !continuous) {
			return true;
		}

		return false;
	}

	@Override
	protected void postLayoutAlgorithm(InternalNode[] nodes,
			InternalRelationship[] arg1) {
		updateLayoutLocations(nodes);
	}

	@Override
	protected void preLayoutAlgorithm(InternalNode[] arg0,
			InternalRelationship[] arg1, double arg2, double arg3, double arg4,
			double arg5) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLayoutArea(double arg0, double arg1, double arg2, double arg3) {
		// TODO Auto-generated method stub
		
	}

}
