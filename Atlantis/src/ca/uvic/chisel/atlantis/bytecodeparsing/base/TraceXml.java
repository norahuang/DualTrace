package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;


@Root(name="Data")
public class TraceXml implements ITraceXml{
// http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php

// <?xml version="1.0" encoding="utf-8"?>
	
//	@Element(name="TraceMetaData")
//	@Path("CPU")
//	public String cpu_hasXmm;
		
//	@Element(name="CPU")
//	public String cpu;
	
//	<CPU>
//	<HasXMM>true</HasXMM>
	@Element(name="HasXMM")
	@Path("TraceMetaData/CPU")
	public String cpu_hasXmm;
//	<hasYMM>false</hasYMM>
	@Element(name="hasYMM")
	@Path("TraceMetaData/CPU")
	public String cpu_hasYmm;
//	<hasZMM>false</hasZMM>
	@Element(name="hasZMM")
	@Path("TraceMetaData/CPU")
	public String cpu_hasZmm;
//	</CPU>


	//	<OS>
	//	<Id>06010110</Id>
	@Element(name="Id")
	@Path("TraceMetaData/OS")
	public String os_id;
	//	<Name>Windows 7 SP1 64-bits</Name>
	@Element(name="Name")
	@Path("TraceMetaData/OS")
	public String os_name;
	//</OS>


//	<Pin>
//	<Version>2.12.56759</Version>
	@Element(name="Version")
	@Path("TraceMetaData/Pin")
	public String pin_version;
//	<CommandLine>C:\RDDC\Dev\Commun\ThirdParties\Pin 2.12\intel64\bin\pin.exe -p32 C:\RDDC\Dev\Commun\ThirdParties\Pin 2.12\ia32\bin\pin.exe -follow_execv -pause_tool 0 -t C:\RDDC\Dev\Mandats\UMTracer\Release\UMTracer64.dll -r 2 -m 0 -- C:\Program Files\Microsoft Office\Office14\WINWORD.EXE</CommandLine>
	@Element(name="CommandLine")
	@Path("TraceMetaData/Pin")
	public String pin_commandLine;
	@Element(name="WorkingDirectory")
	@Path("TraceMetaData/Pin")
	public String pin_workingDirectory;
//	</Pin>


//	<Xed>
	@Element(name="Version")
	@Path("TraceMetaData/Xed")
	public String xed_version;
//	<Version>56759</Version>
//	</Xed>


//	<App>
//	<CommandLine>&quot;C:\Program Files\Microsoft Office\Office14\WINWORD.EXE&quot;</CommandLine>
	@Element(name="CommandLine")
	@Path("TraceMetaData/App")
	public String app_commandLine;
//	<Bitness>64</Bitness>
	@Element(name="Bitness")
	@Path("TraceMetaData/App")
	public String app_bitness;
//	<ProcessID>7472</ProcessID>
	@Element(name="ProcessID")
	@Path("TraceMetaData/App")
	public String app_processId;
//	</App>

	@Element(name="DateTime")
	@Path("TraceMetaData/RawTrace/")
	public String umTracer_dateTime;
	
// <!-- UMTracer -->
//	<UMTracer>
//	<Ins>
//	<Format>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("TraceMetaData/RawTrace/UMTracer")
	public String umTracer_version;
//	</Format>
//	<BeginRecord>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("TraceMetaData/RawTrace/UMTracer/Ins/Format")
	public String umTracer_ins_version;
//	</BeginRecord>
//	</Ins>
//
//	<Exec>
//	<Format>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("TraceMetaData/RawTrace/UMTracer/Exec/Format")
	public String umTracer_exec_format__version;
//	</Format>
	
	
//	<SyscallTable>
//	<Version>0</Version>
//	@Element(name="Version")
//	@Path("TraceMetaData/RawTrace/UMTracer/Exec/SyscallTable")
//	public String umTracer_syscallTable_format__commandLine;
//	</SyscallTable>
//	</Exec>
//	</UMTracer>
	
	@Override
	public String getAppBitness() {
		return this.app_bitness;
	}
	
	@Override
	public String getXedVersion() {
		return this.xed_version;
	}
}
