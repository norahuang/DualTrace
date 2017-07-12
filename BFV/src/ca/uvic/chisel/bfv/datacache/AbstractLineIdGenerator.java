package ca.uvic.chisel.bfv.datacache;

abstract public class AbstractLineIdGenerator <L extends AbstractLine> {
	abstract public void generateLineId(String line, L lineData);
}