Use this directory to store your personal xml files for exporting Gibraltar. The export process involves going
to right click Atlantis->Export->Java->Runnable Jar, and using the Gibraltar launch file included in the repo.
Then, specify that libraries should be packaged, and specify this directory for both the export location and
the ANT script save location. Then, use and reuse your personal script whenever you want to export Gibraltar.
Other developers will not be able to easily use it, since it will necessarily include machine-specific paths
based on your Eclipse installation.