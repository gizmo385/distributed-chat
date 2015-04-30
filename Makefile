all:
	javac $$(find . -name "*.java")
	mkdir -p bin
	find src -name "*.class" -exec mv {} bin \;

debug:
	javac -Xlint $$(find . -name "*.java")
	mkdir -p bin
	find src -name "*.class" -exec mv {} bin \;

clean:
	@rm ./bin/*.class 2> /dev/null || true
