package application.command.parser;

public interface GitOutputParser<T> {
    T parse(String text);
}
