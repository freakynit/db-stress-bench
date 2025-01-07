package com.freakynit.sql.db.stress.bench.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {
    private static final Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)(:-([^}]*))?}");

    public static String render(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template; // Return empty or null template as is
        }

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String defaultValue = matcher.group(3);

            String replacementValue = variables.get(variableName);

            if (replacementValue != null) {
                matcher.appendReplacement(sb, replacementValue);
            } else if (defaultValue != null) {
                matcher.appendReplacement(sb, defaultValue);
            } else {
                throw new IllegalArgumentException("Missing value for template variable: " + variableName);
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void main(String[] args) {
        benchmark();
    }

    private static void correctnessChecks(String[] args) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "Alice");
        vars.put("city", "New York");

        String template1 = "Hello, ${name}! You are in ${city:-Unknown city}.";
        String result1 = render(template1, vars);
        System.out.println(result1);  // Output: Hello, Alice! You are in New York.

        String template2 = "Your age is ${age:-25}, and your name is ${name}.";
        String result2 = render(template2, vars);
        System.out.println(result2);  // Output: Your age is 25, and your name is Alice.

        String template3 = "Welcome, ${user:-Guest}. Your country is ${country}.";
        try{
            String result3 = render(template3, vars);
            System.out.println(result3);
        } catch (IllegalArgumentException e)
        {
            System.err.println(e.getMessage()); //Missing value for template variable: country
        }

        String template4 = "${user1}";
        try{
            String result4 = render(template4, vars);
            System.out.println(result4);
        } catch (IllegalArgumentException e)
        {
            System.err.println(e.getMessage()); //Missing value for template variable: user1
        }
    }

    private static void benchmark() {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "Alice");
        vars.put("city", "New York");

        String template1 = "Hello, ${name}! You are in ${city:-Unknown city}.";
        String template2 = "Your age is ${age:-25}, and your name is ${name}.";

        long startTime = System.currentTimeMillis();

        for(int i = 0; i < 1_000_000; i++) {
            String result1 = render(template1, vars);
            String result2 = render(template2, vars);
        }

        System.out.println("Time taken (ms): " + (System.currentTimeMillis() - startTime));
    }
}