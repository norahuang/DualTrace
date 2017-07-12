package ca.uvic.chisel.bfv.intervaltree;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * The Node class contains the interval tree information for one single node
 * 
 * @author Kevin Dolan
 */
public class IntervalNode<Type> {

	private SortedMap<IntervalElement<Type>, List<IntervalElement<Type>>> intervals;
	private long center;
	private IntervalNode<Type> leftNode;
	private IntervalNode<Type> rightNode;
	
	public IntervalNode() {
		intervals = new TreeMap<IntervalElement<Type>, List<IntervalElement<Type>>>();
		center = 0;
		leftNode = null;
		rightNode = null;
	}
	
	public IntervalNode(List<IntervalElement<Type>> intervalList) {
		
		intervals = new TreeMap<IntervalElement<Type>, List<IntervalElement<Type>>>();
		
		SortedSet<Long> endpoints = new TreeSet<Long>();
		
		for(IntervalElement<Type> node: intervalList) {
			endpoints.add(node.getInterval().getStartValue());
			endpoints.add(node.getInterval().getEndValue());
		}
		
		Long median = getMedian(endpoints);
		center = median == null ? 0 : median;
		
		List<IntervalElement<Type>> left = new ArrayList<IntervalElement<Type>>();
		List<IntervalElement<Type>> right = new ArrayList<IntervalElement<Type>>();
		
		for(IntervalElement<Type> node : intervalList) {
			if(node.getInterval().getEndValue() < median)
				left.add(node);
			else if(node.getInterval().getStartValue() > median)
				right.add(node);
			else {
				List<IntervalElement<Type>> posting = intervals.get(node);
				if(posting == null) {
					posting = new ArrayList<IntervalElement<Type>>();
					intervals.put(node, posting);
				}
				posting.add(node);
			}
		}

		if(left.size() > 0)
			leftNode = new IntervalNode<Type>(left);
		if(right.size() > 0)
			rightNode = new IntervalNode<Type>(right);
	}

	/**
	 * Perform a stabbing query on the node
	 * @param time the time to query at
	 * @return	   all intervals containing time
	 */
	public List<IntervalElement<Type>> stab(long time) {		
		List<IntervalElement<Type>> result = new ArrayList<IntervalElement<Type>>();

		for(Entry<IntervalElement<Type>, List<IntervalElement<Type>>> entry : intervals.entrySet()) {
			Interval nodeInterval = entry.getKey().getInterval();
			
			if(nodeInterval.contains(time))
				for(IntervalElement<Type> interval : entry.getValue())
					result.add(interval);
			else if(nodeInterval.getStartValue() > time)
				break;
		}
		
		if(time < center && leftNode != null)
			result.addAll(leftNode.stab(time));
		else if(time > center && rightNode != null)
			result.addAll(rightNode.stab(time));
		return result;
	}
	
	/**
	 * Perform an interval intersection query on the node
	 * @param target the interval to intersect
	 * @return		   all intervals containing time
	 */
	public List<IntervalElement<Type>> query(IntervalElement<?> target) {
		
		Interval targetInterval = target.getInterval();
		
		List<IntervalElement<Type>> result = new ArrayList<IntervalElement<Type>>();
		
		for(Entry<IntervalElement<Type>, List<IntervalElement<Type>>> entry : intervals.entrySet()) {
			Interval nodeInterval = entry.getKey().getInterval();
			
			if(nodeInterval.intersects(targetInterval))
				for(IntervalElement<Type> interval : entry.getValue())
					result.add(interval);
			else if(nodeInterval.getStartValue() > nodeInterval.getEndValue())
				break;
		}
		
		if(targetInterval.getStartValue() < center && leftNode != null)
			result.addAll(leftNode.query(target));
		if(targetInterval.getEndValue() > center && rightNode != null)
			result.addAll(rightNode.query(target));
		return result;
	}
	
	public long getCenter() {
		return center;
	}

	public void setCenter(long center) {
		this.center = center;
	}

	public IntervalNode<Type> getLeft() {
		return leftNode;
	}

	public void setLeft(IntervalNode<Type> left) {
		this.leftNode = left;
	}

	public IntervalNode<Type> getRight() {
		return rightNode;
	}

	public void setRight(IntervalNode<Type> right) {
		this.rightNode = right;
	}
	
	/**
	 * @param set the set to look on
	 * @return	  the median of the set, not interpolated
	 */
	private Long getMedian(SortedSet<Long> set) {
		int i = 0;
		int middle = set.size() / 2;
		for(Long point : set) {
			if(i == middle)
				return point;
			i++;
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(center + ": ");
		for(Entry<IntervalElement<Type>, List<IntervalElement<Type>>> entry : intervals.entrySet()) {
			sb.append("[" + entry.getKey().getInterval().getStartValue() + "," + entry.getKey().getInterval().getEndValue() + "]:{");
			for(IntervalElement<Type> node : entry.getValue()) {
				sb.append("("+node.getInterval().getStartValue()+","+node.getInterval().getEndValue()+","+node.getData()+")");
			}
			sb.append("} ");
		}
		return sb.toString();
	}
	
}
