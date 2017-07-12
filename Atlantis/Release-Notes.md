Release Notes Atlantis 0.3.0

Included in this release:

1. The Atlantis Trace Analysis Environment (Atlantis.exe)
2. Gibraltar trace processing program (Gibraltar_executable.jar and run_gibraltar.bat)
2. No sample trace files are included, unlike the previous release
3. No installation scripts for the DB (changed from MySQL to SQLite, which requires no setup).

Major Changes
  - Support for DRDC multi-file binary trace format (tool suite v4.0, dated 2015.07)
  - Support for files at least as large as 82million trace lines, as seen in 02B-AdobeReaderWithFiles (4.81GB). Produces 25GB SQLite DB file, takes 5.3hours on development machine with SSD. Requires just a couple of GB of RAM to run.
  
Usage Notes
  - To open a processed trace in Atlantis, the trace files and the atlantis_trace.sqlite.db must be in a 'Trace Analysis Project'. These projects are created/imported in the Big File Project Navigator view (top left panel). The meta data associated with these projects is not important; only the trace files are important 
  - Processing large traces takes a very long time, and is best done from command line using Gibraltar. The only argument is the path to the trace file folder.
  - If views are not displayed, go to Window->Show View
  - An Error View is shown by default, to facilitate bug feedback from users.

Known Issues (see BugLog.txt for detailed bug listing)
  - The Function Recomposition View is broken
  - Memory Search is not functional (sacrificed for performance, can be fixed)

Release Notes Atlantis 0.2.0

Included in this release:

1. The Atlantis Trace Analysis Environment
2. Sample trace files
    a. small_40meg.trace is a 40MB trace file
    b. large_1gig.trace is a 1.2GB trace file
    c. small_40meg_b.trace as a variant of a file above.
3. The Atlantis Trace Analysis Database & sample indexes

Major Changes
  - New Views: Functions, Thread Functions, Hex Memory, Registers
  
  Usage Notes

  - Use context menu to open large files the first time, or if Eclipse causes them to be opened differently.
  - Files *must* end in ".trace" to be opened by Atlantis.
  - If views are not displayed, go to Window->Show View, and check out the new views!

Known Issues (see BugLog.txt for detailed bug listing)
  - An error might be thrown when first indexing the large 1GB file. If so, restart Atlantis.
  - See release notes below for 0.1.8 release.

Release Notes Atlantis 0.1.8

Included in this release:

1. The Atlantis Trace Analysis Environment
2. No sample trace files. New format received, we did not produce smaller files for this release.

Major Changes
  - Support for new thread syntax, confirmed DB, memory, assembly, thread views for new format.
  
  Usage Notes

  - Use context menu to open large files the first time, or if Eclipse causes them to be opened differently.
  - FIles *must* end in ".trace" to be opened by Atlantis.

Known Issues (see BugLog.txt for detailed bug listing)
  - See release notes below for 0.1.7 release.

Release Notes Atlantis 0.1.7

Include in this release:

1. The Atlantis Trace Analysis Environment
2. Sample trace files
    a. small_40meg.trace is a 40MB trace file
    b. large_1gig.trace is a 1.2GB trace file
    c. small_40meg_slightly_different.trace and small_40meg_very_different.trace for use in diff/comparison
3. The Atlantis Trace Analysis Database & sample indexes

Major Changes
  - Change to DB schema, which requires re-processing files or loading of provided sample DB dump
  - Reduced memory requirements for loaded files (custom line tracker)
  - Reduced memory requirements for very large regions
  - Support for larger files (tested 5GB)
  - Revision to memory index DB structures, for faster indexing and querying (30 minute indexing down to 18 minute for a 1GB file)
  - See ChangeLog.txt for detailed changes.


Usage Notes

  - Use context menu to open large files the first time, or if Eclipse causes them to be opened differently.

Known Issues (see BugLog.txt for detailed bug listing)

  - Memory View  
    * View contents can frequently be incorrect. They are derived using a different algorithm than before, and development is still in progress.  
  - Visualizations  
    * none known visualization issues for this release
  - Regions  
    * 
  - Indexing time
    * Improved indexing time: a 1GB file takes ~18 minutes. This is still long enough to make it an offline batch activity.  


Release Notes Atlantis 0.1.6

Include in this release:

1. The Atlantis Trace Analysis Environment
2. Sample trace files
    a. small_40meg.trace is a 40MB trace file
    b. large_1gig.trace is a 1.2GB trace file
    c. small_40meg_slightly_different.trace and small_40meg_very_different.trace for use in diff/comparison
3. The Atlantis Trace Analysis Database & sample indexes

Major Changes
  - Change to DB schema, which requires re-processing files or loading of provided sample DB dump
  - Support for large regions, with fast scrolling
  - Made searching within trace files use backing DB for faster results, and allowing for search on large files
  - Ctr-L now allows jumping to any line in the trace file. Try it!
  - Fixed bug with annotation rendering
  - See ChangeLog.txt for detailed changes.


Usage Notes

  - N/A  

Known Issues

  - Memory View  
    * View contents can frequently be incorrect. They are derived using a different algorithm than before, and development is still in progress.  
  - Visualizations  
    * none known visualization issues for this release
  - Regions  
    * Creating regions causes memory usage to increase. This is due to the way eclipse duplicates files in memory when you create a region. We are working on a new implementation of regions that will rectify this issue.
    * Creating a region including the first line of the trace will result in a region that is not collapsible.  
  - Indexing time  
    * The indexing time required for a 1GB file is ~30 minutes. Most of this is building the memory reference delta index. We have not done much optimization in this area yet as we view this as an offline batch activity, but I think there is significant scope for performance improvements in the future.  


Release Notes Atlantis 0.1.5

Include in this release:

1. The Atlantis Trace Analysis Environment
2. Sample trace files
    a. small_40meg.trace is a 40MB trace file
    b. large_1gig.trace is a 1.2GB trace file
    c. small_40meg_slightly_different.trace and small_40meg_very_different.trace for use in diff/comparison
3. The Atlantis Trace Analysis Database & sample indexes

Major Changes

  - Trace file comparison mode allows two trace files to be diffed against one another, and the results viewed within Atlantis.  
  - Display of much larger (>1GB) trace files. This competes with some dedicated large text file editors, and is likely the largest for any Eclipse implementation at present.  
  - Using a database for major data structures allows display of a 1.2GB file in 1.6GB of RAM, compared to previous requirement of 2GB RAM for a 20MB trace file. This approaches to linear requirements,  
  - Improved memory view query speed, using R-Tree index on memory delta tree structured data.  
  - Known bottlenecks in memory view database queries have been fixed.  
  - See ChangeLog.txt for detailed changes.  

Usage Notes

  - To use the diff editor, select the ".original" copies of the two files in the "Trace Management View". Right click one of them, and select "Compare With->Each Other". When the view has opened, change it from "Binary Compare" or "Default ComparE" to 'Text Compare". This procedure will be streamlined in the next release.  

Known Issues

  - Memory View  
    * View contents can frequently be incorrect. They are derived using a different algorithm than before, and development is still in progress.  
    * Occasional queries to the database for memory view contents take longer (12s) on their first attempt, due to MySQL needing to page more of the index from disk. Subsequent queries in the adjacent segments of the file are quick. Queries will not queue up and cause bottlenecks any longer.  
  - Visualizations  
    * none known visualization issues for this release
  - Regions  
    * Creating regions causes memory usage to spike. This is due to the way eclipse duplicates files in memory when you create a region. We are working on a new implementation of regions that will rectify this issue.  
  - Search  
    * The search feature has not been moved to the new backend yet. As such executing a search will work but against the large files it will be very slow. We will see significant improvements in search when we move to the new backend.  
  - Indexing time  
    * The indexing time required for a 1GB file is ~30 minutes. Most of this is building the memory reference delta index. We have not done much optimization in this area yet as we view this as an offline batch activity, but I think there is significant scope for performance improvements in the future.  


Release Notes Atlantis 0.1.4

Include in this release:

1. The Atlantis Trace Analysis Environment
2. Sample trace files
    a. A000000000001a.trace is a 40MB trace file
    b. test1.trace is a 1.2GB trace file
3. The Atlantis Trace Analysis Database & sample indexes

Major Changes

  - This build of Atlantis contains significant changes to the Atlantis backend allowing us to scale Atlantis to handle ~20M trace lines. This is easily the largest file size that has ever been loaded in an eclipse based tool and even competes with some dedicated large text file editors.  
  - Also included is a new memory reference delta index that allows near real time access to the complete memory state of the trace at any trace line. Performance of the memory index should be close to linear for any point in the trace file (this is a big deal).  
  - We have updated the Trace View to significantly improve performance and stability.  
  - We have also worked on improving the memory footprint. Atlantis will not load 20M line files in about 2GB of memory. In theory our new architecture should allow loading any size file in the same memory footprint.  
  - See ChangeLog.txt for detailed changes.

Known Issues

  - Memory View  
    * There is a slight issue with some memory delta index queries not completing and then queuing up which causes the memory view to stop responding. It appears to only affect certain parts of the trace. We are working on a fix. If the memory view stops responding restart Atlantis.  
  - Visualizations  
    * The Trace visualizations are not working fully at the moment, this is due to the change to the backend. This will be fixed for the next release.  
  - Regions  
    * Creating regions causes memory usage to spike. This is due to the way eclipse duplicates files in memory when you create a region. We are working on a new implementation of regions that will rectify this issue.  
  - Search  
    * The search feature has not been moved to the new backend yet. As such executing a search will work but against the large files it will be very slow. We will see significant improvements in search when we move to the new backend.  
  - Indexing time  
    * The indexing time required for a 1GB file is ~30 minutes. Most of this is building the memory reference delta index. We have not done much optimization in this area yet as we view this as an offline batch activity, but I think there is significant scope for performance improvements in the future.   
