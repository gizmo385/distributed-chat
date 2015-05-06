all: client_files server_files
	@echo "Moving class files to bin"
	@mkdir -p bin
	@find src -name "*.class" -exec mv {} bin \;

debug: FLAGS_ADDED=-Xlint
debug: client_files server_files
	@echo "Moving class files to bin"
	@mkdir -p bin
	@find src -name "*.class" -exec mv {} bin \;

protocol_files:
	@echo "Compiling protocol files..."
	@javac ${FLAGS_ADDED} src/protocol/*.java

client_files: protocol_files
	@echo "Compiling client files..."
	@javac ${FLAGS_ADDED} -cp src/protocol src/client/*.java

server_files: protocol_files
	@echo "Compiling server files..."
	@javac ${FLAGS_ADDED} -cp src/protocol src/server/*.java

client: client_files
	@mkdir -p bin
	@echo "Main-Class: ChatClient" > Manifest.txt
	@echo "Building jar file..."
	@mv src/client/*.class src/protocol/*.class .
	@jar -cfm Client.jar Manifest.txt *.class
	@echo "Cleaning up..."
	@mv *.class bin
	@rm -rf Manifest.txt META-INF

server: server_files
	@mkdir -p bin
	@echo "Main-Class: ChatServer" > Manifest.txt
	@echo "Building jar file..."
	@mv src/server/*.class src/protocol/*.class .
	@jar -cfm Server.jar Manifest.txt *.class
	@echo "Cleaning up..."
	@mv *.class bin
	@rm -rf Manifest.txt META-INF

clean:
	@rm ./bin/*.class 2> /dev/null || true
