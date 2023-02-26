package io.protobj;

import java.io.BufferedReader;
import java.io.IOException;

public class Main {
    public static final String PROJECT_PATH = "C:\\Users\\79871\\IdeaProjects\\game-server";
    public static final String OUTPUT_MODULE = "game";

    public static void main(String[] args) throws IOException {
        var protobjcPath = PROJECT_PATH + "\\protocol\\protobjc.exe";
        var source = PROJECT_PATH + "\\protocol\\src\\main\\java";
        var output = PROJECT_PATH + "\\%s\\src\\main\\java".formatted(OUTPUT_MODULE);
        var params = " -s %s -o %s -lang Java".formatted(source, output);
        String command = protobjcPath + params;
        System.err.println(command);
        Process exec = Runtime.getRuntime().exec(command);
        try (BufferedReader bufferedReader = exec.inputReader()) {
            bufferedReader.lines().forEach(System.out::println);
        }
        try (BufferedReader bufferedReader = exec.errorReader()) {
            bufferedReader.lines().forEach(System.out::println);
        }
    }
}