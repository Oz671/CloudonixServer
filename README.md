First, clone the repository to your computer:
	
	
In order to run the server you need to import this maven project.
For running the server run this cmd command in the project directory:

mvn org.codehaus.mojo:exec-maven-plugin:exec -Dexec.executable=java -Dexec.args="-cp %classpath io.vertx.core.Launcher run main.Server"