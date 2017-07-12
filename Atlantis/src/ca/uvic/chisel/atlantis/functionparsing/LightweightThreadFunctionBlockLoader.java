package ca.uvic.chisel.atlantis.functionparsing;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;

public class LightweightThreadFunctionBlockLoader {
	
	private Deque<Integer> pagesToLoad;
	private ReentrantLock pagesToLoadLock;
	private ArrayList<LightweightThreadFunctionBlock>[] pages;
	private ReentrantLock[] pageLocks;
	private CountDownLatch[] pageLatches;
	private long threadLength;
	private int pageSize = 1000;
	private int numPages;
	private AtlantisFileModelDataLayer fileModel;
	private int threadId;
	private volatile boolean notifyThreadEnd = false;
	private Thread loaderThread;
	
	public LightweightThreadFunctionBlockLoader(AtlantisFileModelDataLayer fileModel, int threadId, long threadLength) {
		this.threadLength = threadLength;
		this.fileModel = fileModel;
		this.threadId = threadId;
		
		numPages = (int)(Math.ceil((double)threadLength / (double)pageSize));
		
		pageLocks = new ReentrantLock[numPages];
		pageLatches = new CountDownLatch[numPages];
		pagesToLoad = new LinkedList<Integer>();
		pages = (ArrayList<LightweightThreadFunctionBlock>[])Array.newInstance(new ArrayList<LightweightThreadFunctionBlock>().getClass(), numPages);
		
		pagesToLoadLock = new ReentrantLock();
		pagesToLoadLock.lock();
		for(int i = numPages - 1; i >= 0; i--) {
			pagesToLoad.push(i);
			
			pageLocks[i] = new ReentrantLock();
			pageLatches[i] = new CountDownLatch(1);
		}
		pagesToLoadLock.unlock();
	}
	
	private int getPageContainingOffset(long offset) {
		if(offset < 0) {
			return 0;
		}
		
		if(offset >= numPages * pageSize) {
			return numPages - 1;
		}
		
		return (int) (offset / pageSize);
	}
	
	public boolean areBlocksInRangeAvailable(long start, long end) {
		int startPage = getPageContainingOffset(start);
		int endPage = getPageContainingOffset(end);
		
		pageLocks[startPage].lock();
		boolean havePage = pages[startPage] != null;
		pageLocks[startPage].unlock();
		
		if(havePage && endPage != startPage) {
			pageLocks[endPage].lock();
			havePage = pages[endPage] != null;
			pageLocks[endPage].unlock();
		}
		
		return havePage;
	}
	
	public void putRangeOnTopOfQueue(long start, long end) {
		if(!areBlocksInRangeAvailable(start, end)) {
			pagesToLoadLock.lock();
			
			pagesToLoad.push(getPageContainingOffset(start));
			pagesToLoad.push(getPageContainingOffset(end));
			
			pagesToLoadLock.unlock();
		}
	}
	
	public void beginAsyncPageLoading() {
		loaderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				pagesToLoadLock.lock();
				boolean hitNull = false;
				
				while(!pagesToLoad.isEmpty() && !notifyThreadEnd) {
					int page = pagesToLoad.pop();
					pagesToLoadLock.unlock();
					
					pageLocks[page].lock();
					boolean havePage = pages[page] != null;
					pageLocks[page].unlock();
					
					if(havePage) {
						pagesToLoadLock.lock();
						continue;
					}
					
					ArrayList<LightweightThreadFunctionBlock> blocks = fileModel.getThreadFunctionBlockDb().getLightweightThreadFunctionBlocks(
							threadId, page * pageSize, (page + 1) * pageSize);
					
					pageLocks[page].lock();
					pages[page] = blocks;
					pageLocks[page].unlock();
					
					pageLatches[page].countDown();
							
					pagesToLoadLock.lock();
					
					if(null == blocks){
						// Kill yourself; exceptions deeper down mean we should abort.
						notifyThreadEnd = true;
						hitNull = true;
						break;
					}
				}
			
				pagesToLoadLock.unlock();
			
				if(notifyThreadEnd || hitNull) {
		        	// received a stop signal.
		        	fileModel.getThreadFunctionBlockDb().cancelThreadFuncBlockQueries();
		        }
			}
		});
		loaderThread.start();
	}
	
	public List<LightweightThreadFunctionBlock> getBlocksInRange(long start, long end) {
		if(areBlocksInRangeAvailable(start, end)) {
			int startPage = getPageContainingOffset(start);
			int endPage = getPageContainingOffset(end);
			
			List<LightweightThreadFunctionBlock> resultBlocks = new ArrayList<LightweightThreadFunctionBlock>();
			
			for(LightweightThreadFunctionBlock tfb : pages[startPage]) {
				if(tfb.getXOffset() + tfb.getWidth() >= start && tfb.getXOffset() <= end) {
					resultBlocks.add(tfb);
				}
			}
			
			if(startPage != endPage) {
				for(LightweightThreadFunctionBlock tfb : pages[endPage]) {
					if(tfb.getXOffset() + tfb.getWidth() >= start && tfb.getXOffset() <= end && !tfb.equals(resultBlocks.get(resultBlocks.size() - 1))) {
						resultBlocks.add(tfb);
					}
				}
			}
			
			return resultBlocks;
		}
		
		return null;
	}
	
	public void listenForBlocksLoaded(final long start, final long end, final Runnable callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					pageLatches[getPageContainingOffset(start)].await();
					pageLatches[getPageContainingOffset(end)].await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				PlatformUI.getWorkbench().getDisplay().asyncExec(callback);
			}
		}).start();
	}
	
	public int getPageSize() {
		return this.pageSize;
	}
	
	public void kill() {
		this.notifyThreadEnd = true;
		try {
			this.loaderThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
