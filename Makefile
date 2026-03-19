run:
	javac -d bin src/com/craftinginterpreters/lox/*.java
	java -cp bin com.craftinginterpreters.lox.Lox
