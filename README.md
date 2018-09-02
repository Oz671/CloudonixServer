First, clone the repository to your computer from https://github.com/Oz671/CloudonixServer.git
	
In order to run the server you need to import the project as maven project.
For running the server run this cmd command in the project directory:

mvn org.codehaus.mojo:exec-maven-plugin:exec -Dexec.executable=java -Dexec.args="-cp %classpath io.vertx.core.Launcher run main.Server"

For testing the server you can run:

curl http://localhost:8080/analyze -d "{\"text\":\"aa\"}"