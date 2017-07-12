package ca.uvic.chisel.atlantis.bytecodeparsing.base;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;


@Root(name="Data")
public class Pre2015TraceXml implements ITraceXml {
// http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php

// <?xml version="1.0" encoding="utf-8"?>
	
//	@Element(name="CPU")
//	public String cpu;
	
//	<CPU>
//	<HasXMM>true</HasXMM>
	@Element(name="HasXMM")
	@Path("CPU")
	public String cpu_hasXmm;
//	<hasYMM>false</hasYMM>
	@Element(name="hasYMM")
	@Path("CPU")
	public String cpu_hasYmm;
//	<hasZMM>false</hasZMM>
	@Element(name="hasZMM")
	@Path("CPU")
	public String cpu_hasZmm;
//	</CPU>


	//	<OS>
	//	<Id>06010110</Id>
	@Element(name="Id")
	@Path("OS")
	public String os_id;
	//	<Name>Windows 7 SP1 64-bits</Name>
	@Element(name="Name")
	@Path("OS")
	public String os_name;
	//</OS>


//	<Pin>
//	<Version>2.12.56759</Version>
	@Element(name="Version")
	@Path("Pin")
	public String pin_version;
//	<CommandLine>C:\RDDC\Dev\Commun\ThirdParties\Pin 2.12\intel64\bin\pin.exe -p32 C:\RDDC\Dev\Commun\ThirdParties\Pin 2.12\ia32\bin\pin.exe -follow_execv -pause_tool 0 -t C:\RDDC\Dev\Mandats\UMTracer\Release\UMTracer64.dll -r 2 -m 0 -- C:\Program Files\Microsoft Office\Office14\WINWORD.EXE</CommandLine>
	@Element(name="CommandLine")
	@Path("Pin")
	public String pin_commandLine;
//	</Pin>


//	<Xed>
	@Element(name="Version")
	@Path("Xed")
	public String xed_version;
//	<Version>56759</Version>
//	</Xed>


//	<App>
//	<CommandLine>&quot;C:\Program Files\Microsoft Office\Office14\WINWORD.EXE&quot;</CommandLine>
	@Element(name="CommandLine")
	@Path("App")
	public String app_commandLine;
//	<Bitness>64</Bitness>
	@Element(name="Bitness")
	@Path("App")
	public String app_bitness;
//	<ProcessID>7472</ProcessID>
	@Element(name="ProcessID")
	@Path("App")
	public String app_processId;
//	</App>

	
// <!-- UMTracer -->
//	<UMTracer>
//	<Ins>
//	<Format>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("UMTracer/Ins/Format")
	public String umTracer_ins_format__commandLine;
//	</Format>
//	<BeginRecord>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("UMTracer/Ins/BeginRecord")
	public String umTracer_ins_beginRecord__commandLine;
//	</BeginRecord>
//	</Ins>
//
//	<Exec>
//	<Format>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("UMTracer/Exec/Format")
	public String umTracer_exec_format__commandLine;
//	</Format>

//	<SyscallTable>
//	<Version>0</Version>
	@Element(name="Version")
	@Path("UMTracer/Exec/SyscallTable")
	public String umTracer_syscallTable_format__commandLine;
//	</SyscallTable>
//	</Exec>
//	</UMTracer>

	@Override
	public String getAppBitness() {
		return this.app_bitness;
	}

	@Override
	public String getXedVersion() {
		return xed_version;
	}
	
}
