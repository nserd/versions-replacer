package ru.nserd;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    private static final List<Component> componentsList = new ArrayList<>();
    
    public static void main(String[] args) throws IOException {
        argHandler(args);

        Path srcFile = Paths.get(args[0]);
        Path versionsFile = Paths.get(args[1]);

        parseComponentsFromFile(srcFile);
        replaceVersionsInFile(versionsFile);
    }

    private static void argHandler(String[] args) {
        if (args.length == 0 || (args.length == 1 && ! args[0].equals("--help"))) {
            System.err.println("Requred arguments not found. Use --help.");
            System.exit(-1);
        } else if (args[0].equals("--help")) {
            printHelp();
            System.exit(0);
        }
    }

    private static void printHelp() {
        StringBuilder sb = new StringBuilder();

        sb.append("\u001B[1mVersions Replacer\u001B[0m\n");
        sb.append("\n");
        sb.append("Usage: java -jar versions-replacer.jar <txt-file> <yml-file>\n");
        sb.append("\n");
        sb.append("Arguments:\n");
        sb.append("\n");
        sb.append("  txt-file    File with content copied from update task\n");
        sb.append("  yml-file    YAML file with versions\n");

        System.out.println(sb);
    }

    private static void replaceVersionsInFile(Path versionsFile) throws IOException {
        List<String> lines = Files.readAllLines(versionsFile, StandardCharsets.UTF_8);
        List<String> newLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            for (Component comp : componentsList) {
                if (lines.get(i).contains(comp.name + ':')) {
                    newLines.add(lines.get(i++));
                    newLines.add(lines.get(i++)); // vars:

                    i = replaceComponentData(comp, lines, newLines, i);
                } 
            }

            newLines.add(lines.get(i));
        }

        writeToFile(newLines, versionsFile);
    }

    private static void writeToFile(List<String> lines, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 0; i < lines.size(); i++) {
                writer.write(lines.get(i));
                if (i != lines.size() -1 ) writer.newLine();
            }
        }
    }

    private static int replaceComponentData(Component comp, List <String> input, List <String> output, int index) {
        if (comp.branch != null && ! haveBranch(input, index)) {
            output.add("    component_branch: " + comp.branch);
        }

        while (hasIndent(input.get(index), 4)) {
            if (input.get(index).contains("component_branch")) {
                output.add(comp.branch == null ? input.get(index) : "    component_branch: " + comp.branch);
            } else if (input.get(index).contains("component_tag")) {
                output.add("    component_tag: " + comp.trunk);
            }
            index++;
        }

        return index;
    }

    private static boolean haveBranch(List<String> list, int index) {
        while (hasIndent(list.get(index), 4)) {
            if (list.get(index).contains("component_branch")) {
                return true;
            }
            index++;
        }

        return false;
    }

    private static boolean hasIndent (String str, int numbOfSpaces) {
        for (int i = 0; i < numbOfSpaces; i++) {
            if (str.charAt(i) != ' ') {
                return false;
            }
        }

        return true;
    }

    private static void parseComponentsFromFile(Path srcFile) throws IOException {
        List<String> lines = removeExtraChars(Files.readAllLines(srcFile, StandardCharsets.UTF_8));
        String[] wordsArray = lines.stream().collect(Collectors.joining("\n")).split(":\\n|:|\\n");

        if (wordsArray[0].equals("component_branch") && wordsArray[0].equals("component_tag") && wordsArray[0].matches(".*\\d.*")) {
            System.err.println("Incorrect list format");
            System.exit(-1);
        }

        int iterator = 0;
        while (iterator < wordsArray.length) {
            iterator = parseComponent(wordsArray, iterator);
        }
    }

    private static int parseComponent(String[] words, int iterator) {
        String name = words[iterator++];

        String branch = new String();
        String trunk = new String();

        if (words[iterator].equals("component_branch")) {
            branch = words[iterator + 1];
            iterator += 2;
        } 

        if (words[iterator].equals("component_tag")) {
            trunk = words[iterator + 1];
            iterator += 2;

            if (branch.length() == 0 && words[iterator].equals("component_branch")) {
                branch = words[iterator + 1];
                iterator += 2;
            }
        } else {
            System.err.println("Incorrect list format (" + words[iterator] + ")");
            System.exit(-1);
        }

        componentsList.add(branch.length() > 0 ? new Component(name, branch, trunk) : new Component(name, trunk));

        return iterator;
    }

    private static List<String> removeExtraChars(List<String> list) {
        return list.stream()
            .map(s -> s.replaceAll("\u00A0", ""))   // Remove no-break space symbol
            .map(s -> s.replaceAll("\u0441", "c"))  // Replacing a cyrillic character with a latin one
            .map(s -> s.strip())
            .map(s -> s.replaceAll(" ",""))
            .filter(s -> s.length() > 0)
            .collect(Collectors.toList());
    }

    private static class Component {
        public final String name;
        public final String branch;
        public final String trunk;

        public Component(String name, String branch, String trunk) {
            this.name = name;
            this.branch = branch;
            this.trunk = trunk;
        }

        public Component(String name, String trunk) {
            this(name, null, trunk);
        }

        @Override
        public String toString() {
            return branch != null ? new String("[" + name + ": " + branch + ", " + trunk + "]") : new String("[" + name + ": " + trunk + "]");
        }
    }
}
