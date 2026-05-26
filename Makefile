run:
	javac -d bin src/com/craftinginterpreters/tool/GenerateAst.java
	java -cp bin com.craftinginterpreters.tool.GenerateAst src/com/craftinginterpreters/lox
	javac -d bin src/com/craftinginterpreters/lox/*.java
	java -cp bin com.craftinginterpreters.lox.Lox
