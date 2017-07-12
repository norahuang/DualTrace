REM Don't modify and commit this file, it is for reference.
REM Do copy it to wherever your trace files are, and copy the sqldiff.exe with it, and run it there.
sqldiff --help
PAUSE
sqldiff --summary new_atlantis_trace.sqlite.db previous_atlantis_trace.sqlite.db
PAUSE
