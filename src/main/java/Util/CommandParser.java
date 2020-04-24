package Util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandParser {
    private static final String TYPE_EVAL = "eval";
    
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    
    public CommandParser() {
    
    }
    
    public String parse(String input) {
        List<String> expressionList = getExpressions(input);
        List<String> replacements = new ArrayList<>();
        for (String expression : expressionList) {
            String[] split = expression.split(" ", 2);
            String type = split[0];
            String content = split[1];
            switch (type) {
                case TYPE_EVAL:
                    replacements.add(evalJavaScript(content));
                default:
                    replacements.add("ERROR");
            }
        }
        
        String output = input;
        for (int i = 0; i < expressionList.size(); i++) {
            output = output.replaceFirst(Pattern.quote("$(" + expressionList.get(i) + ")"), replacements.get(i));
        }
        return output;
    }
    
    private static List<String> getExpressions(String input) {
        ArrayList<String> expressions = new ArrayList<>();
        try {
            for (int i = 0; i < input.length(); i++) {
                if (input.charAt(i) == '$' && input.charAt(i + 1) == '(') {
                    i += 2;
                    int start = i;
                    int depth = 1;
                    while (depth != 0) {
                        if (input.charAt(i) == '(') {
                            depth += 1;
                        } else if (input.charAt(i) == ')') {
                            depth -= 1;
                        }
                        i++;
                    }
                    i--;
                    int end = i;
                    expressions.add(input.substring(start, end));
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            //do nothing, reached the end
        }
        return expressions;
    }
    
    private static String evalJavaScript(String js) {
        try {
            return engine.eval(js).toString();
        } catch (ScriptException e) {
            return "ERROR";
        }
    }
}
