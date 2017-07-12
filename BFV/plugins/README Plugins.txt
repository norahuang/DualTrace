I created this folder for plugins that are a hassle to set up.

Do NOT confuse this plugins directory with your actual Eclipse plugins directory.
This directory is where we can store things that need to go into the Eclipse plugins
directory. Nothing functions here; it is for storage, and backups of plugins used
by Eclipse that require additional steps otherwise.

1) JDBC for database connection

I added the jar produced from the below process to the plugins dir in the repo.
Copy this to your Eclipse installation plugins directory and restart eclipse. 
Also you should update the run configuration to add the plugin as a required plugin.

To get the jar in question, I did as follows...

JDBC from MySQL was created from the process documented at the first link, with the
included files downloaded from the MySQL site (in mysql-connector-java-5.1.22). To create
it, we had to make another project in Eclipse, and create a plugin from it, then export that
plugin in jar form. They advise to export to the Eclipse plugins dir, but I exported to this
*project* plugins directory to make it easier to check the project out.

Instructions:
http://wiki.eclipse.org/Create_and_Export_MySQL_JDBC_driver_bundle

To access the connector har in the plugins dir it required an entry in the MANIFEST.MF
(com.mysql.jdbc;bundle-version="1.0.0").


2) ...