package ca.uvic.chisel.atlantis.functionparsing;

public class NamedPipeFunctions{
	
    private static final NamedPipeFunctions instance = new NamedPipeFunctions();
    private ChannelType namedPipe;
    


	//private constructor to avoid client applications to use constructor
    private NamedPipeFunctions(){
    	namedPipe = new ChannelType("NamedPipeChannel");
		namedPipe.addFuncInChannelOpenStage(new ChannelFunction(CreateNamedPipeAFuncName, RetrunValReg, CreateNamedPipeFileNameReg, null, null, true));
    	namedPipe.addFuncInChannelOpenStage(new ChannelFunction(CreateFileAFuncName, RetrunValReg, CreateFileFileNameReg, null, null, true));
    	namedPipe.addFuncInDataTransStage(new ChannelFunction(WriteFileFuncName, RetrunValReg, WriteFileFileHandleReg, WriteFileDataAddrReg, null, false));
    	namedPipe.addFuncInDataTransStage(new ChannelFunction(ReadFileFuncName, RetrunValReg, ReadFileFileHandleReg, null, ReadFileDataAddrReg, false));
    	namedPipe.addFuncInDataTransStage(new ChannelFunction(GetOverlappedResultFuncName, RetrunValReg, GetOverlappedResultFileHandleReg, null, GetOverlappedResultOverLapReg, false));
    	namedPipe.addFuncInChannelCloseStage(new ChannelFunction(CloseHandleFuncName, RetrunValReg, CloseHandleFileHandleReg, null, null, false));
    	
    }
    
    public static NamedPipeFunctions getInstance(){
        return instance;
    }
	
	private Instruction CreateNamedPipeA;
	private Instruction CreateNamedPipeW;
	private String CreateNamedPipeAFuncName = "CreateNamedPipeA";
	private String CreateNamedPipeWFuncName = "CreateNamedPipeW";
	private Register CreateNamedPipeFileNameReg = new Register("RCX", false);
	private Register CreateNamedPipeFileHandleReg = new Register("RAX", true);
	private Instruction CreateFileA;
	private Instruction CreateFileW;
	private String CreateFileAFuncName = "CreateFileA";
	private String CreateFileWFuncName = "CreateFileW";
	private Register CreateFileFileNameReg = new Register("RCX", false);
	private Register CreateFileFileHandleReg = new Register("RAX", true);
	private Instruction WriteFile;
	private String WriteFileFuncName = "WriteFile";
	private Register WriteFileFileHandleReg = new Register("RCX", true);
	private Register WriteFileDataAddrReg = new Register("RDX", false);
	private Instruction ReadFile;
	private String ReadFileFuncName = "ReadFile";
	private Register ReadFileFileHandleReg = new Register("RCX", true);
	private Register ReadFileDataAddrReg = new Register("RDX", false);
	private Instruction GetOverlappedResult;
	private String GetOverlappedResultFuncName = "GetOverlappedResult";
	private Register GetOverlappedResultFileHandleReg = new Register("RCX", true);
	private Register GetOverlappedResultOverLapReg = new Register("RDX", false);
	private Instruction CloseHandle;
	private String CloseHandleFuncName = "CloseHandle";
	private Register CloseHandleFileHandleReg = new Register("RCX", true);
	private Register RetrunValReg = new Register("RAX", true);
	
	public Register getRetrunValReg() {
		return RetrunValReg;
	}
	public void setRetrunValReg(Register retrunValReg) {
		RetrunValReg = retrunValReg;
	}
	public Register getWriteFileFileHandleReg() {
		return WriteFileFileHandleReg;
	}
	public void setWriteFileFileHandleReg(Register writeFileFileHandleReg) {
		WriteFileFileHandleReg = writeFileFileHandleReg;
	}
	public Register getWriteFileDataAddrReg() {
		return WriteFileDataAddrReg;
	}
	public void setWriteFileDataAddrReg(Register writeFileDataAddrReg) {
		WriteFileDataAddrReg = writeFileDataAddrReg;
	}
	public Register getReadFileFileHandleReg() {
		return ReadFileFileHandleReg;
	}
	public void setReadFileFileHandleReg(Register readFileFileHandleReg) {
		ReadFileFileHandleReg = readFileFileHandleReg;
	}
	public Register getReadFileDataAddrReg() {
		return ReadFileDataAddrReg;
	}
	public void setReadFileDataAddrReg(Register readFileDataAddrReg) {
		ReadFileDataAddrReg = readFileDataAddrReg;
	}
	public Register getGetOverlappedResultFileHandleReg() {
		return GetOverlappedResultFileHandleReg;
	}
	public void setGetOverlappedResultFileHandleReg(Register getOverlappedResultFileHandleReg) {
		GetOverlappedResultFileHandleReg = getOverlappedResultFileHandleReg;
	}
	public Register getGetOverlappedResultOverLapReg() {
		return GetOverlappedResultOverLapReg;
	}
	public void setGetOverlappedResultOverLapReg(Register getOverlappedResultOverLapReg) {
		GetOverlappedResultOverLapReg = getOverlappedResultOverLapReg;
	}
	public Register getCloseHandleFileHandleReg() {
		return CloseHandleFileHandleReg;
	}
	public void setCloseHandleFileHandleReg(Register closeHandleFileHandleReg) {
		CloseHandleFileHandleReg = closeHandleFileHandleReg;
	}
	public Instruction getCreateNamedPipeW() {
		return CreateNamedPipeW;
	}
	public void setCreateNamedPipeW(Instruction createNamedPipeW) {
		CreateNamedPipeW = createNamedPipeW;
	}
	public Instruction getCreateNamedPipeA() {
		return CreateNamedPipeA;
	}
	public void setCreateNamedPipeA(Instruction createNamedPipeA) {
		CreateNamedPipeA = createNamedPipeA;
	}
	public String getCreateNamedPipeAFuncName() {
		return CreateNamedPipeAFuncName;
	}
	public void setCreateNamedPipeFuncName(String createNamedPipeAFuncName) {
		CreateNamedPipeAFuncName = createNamedPipeAFuncName;
	}
	
	public String getCreateNamedPipeWFuncName() {
		return CreateNamedPipeWFuncName;
	}
	public void setCreateNamedPipeWFuncName(String createNamedPipeWFuncName) {
		CreateNamedPipeWFuncName = createNamedPipeWFuncName;
	}
	
	public Register getCreateNamedPipeFileNameReg() {
		return CreateNamedPipeFileNameReg;
	}
	public void setCreateNamedPipeFileNameReg(Register createNamedPipeFileNameReg) {
		CreateNamedPipeFileNameReg = createNamedPipeFileNameReg;
	}
	public Register getCreateNamedPipeFileHandleReg() {
		return CreateNamedPipeFileHandleReg;
	}
	public void setCreateNamedPipeFileHandleReg(Register createNamedPipeFileHandleReg) {
		CreateNamedPipeFileHandleReg = createNamedPipeFileHandleReg;
	}
	public Instruction getCreateFileA() {
		return CreateFileA;
	}
	public void setCreateFileA(Instruction createFileA) {
		CreateFileA = createFileA;
	}
	public String getCreateFileAFuncName() {
		return CreateFileAFuncName;
	}
	public void setCreateFileAFuncName(String createFileAFuncName) {
		CreateFileAFuncName = createFileAFuncName;
	}
	public Instruction getCreateFileW() {
		return CreateFileW;
	}
	public void setCreateFileW(Instruction createFileW) {
		CreateFileW = createFileW;
	}
	public String getCreateFileWFuncName() {
		return CreateFileWFuncName;
	}
	public void setCreateFileWFuncName(String createFileWFuncName) {
		CreateFileWFuncName = createFileWFuncName;
	}
	public Instruction getWriteFile() {
		return WriteFile;
	}
	public void setWriteFile(Instruction writeFile) {
		WriteFile = writeFile;
	}
	public String getWriteFileFuncName() {
		return WriteFileFuncName;
	}
	public void setWriteFileFuncName(String writeFileFuncName) {
		WriteFileFuncName = writeFileFuncName;
	}
	public Instruction getReadFile() {
		return ReadFile;
	}
	public void setReadFile(Instruction readFile) {
		ReadFile = readFile;
	}
	public String getReadFileFuncName() {
		return ReadFileFuncName;
	}
	public void setReadFileFuncName(String readFileFuncName) {
		ReadFileFuncName = readFileFuncName;
	}
	public Instruction getGetOverlappedResult() {
		return GetOverlappedResult;
	}
	public void setGetOverlappedResult(Instruction getOverlappedResult) {
		GetOverlappedResult = getOverlappedResult;
	}
	public String getGetOverlappedResultFuncName() {
		return GetOverlappedResultFuncName;
	}
	public void setGetOverlappedResultFuncName(String getOverlappedResultFuncName) {
		GetOverlappedResultFuncName = getOverlappedResultFuncName;
	}
	public Instruction getCloseHandle() {
		return CloseHandle;
	}
	public void setCloseHandle(Instruction closeHandle) {
		CloseHandle = closeHandle;
	}
	public String getCloseHandleFuncName() {
		return CloseHandleFuncName;
	}
	public void setCloseHandleFuncName(String closeHandleFuncName) {
		CloseHandleFuncName = closeHandleFuncName;
	}
	
	public Register getCreateFileFileNameReg() {
		return CreateFileFileNameReg;
	}
	public void setCreateFileFileNameReg(Register createFileFileNameReg) {
		CreateFileFileNameReg = createFileFileNameReg;
	}
	public Register getCreateFileFileHandleReg() {
		return CreateFileFileHandleReg;
	}
	public void setCreateFileFileHandleReg(Register createFileFileHandleReg) {
		CreateFileFileHandleReg = createFileFileHandleReg;
	}

    public ChannelType getNamedPipe() {
		return namedPipe;
	}

}
