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
    private static boolean DEBUG = false;
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
        } else if (args.length == 3 && args[2].equals("--debug")) {
            DEBUG = true;
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

        if (DEBUG) {
            System.out.println("=======================================");
            System.out.println("           Comoponents list            ");
            System.out.println("=======================================");
            componentsList.forEach(System.out::println);
            System.out.println();
            System.out.println("=======================================");
            System.out.println("        Replace component data         ");
            System.out.println("=======================================");
        }
        
        for (int i = 0; i < lines.size(); i++) {
            int indexAfterReplace = searchAndReplaceComponent(i, lines, newLines);
            
            if (indexAfterReplace == i) {
                newLines.add(lines.get(i));
            } else {
                i = indexAfterReplace;
            }
        }

        addNewComponents(newLines);
        writeToFile(newLines, versionsFile);
    }

    private static int searchAndReplaceComponent(int i, List<String> lines, List<String> newLines) {
        for (int j = 0; j < componentsList.size(); j++) {
            Component comp = componentsList.get(j);

            if (lines.get(i).contains(comp.name + ':')) {
                if (DEBUG) {
                    System.out.println(i + ": " + lines.get(i) + " (FOUND " + comp.name + ")");
                }
                
                newLines.add(lines.get(i++));
                newLines.add(lines.get(i++)); // vars:

                i = replaceComponentData(comp, lines, newLines, i);

                if (DEBUG) {
                    System.out.println("Return index: " + i);
                }
                
                componentsList.remove(j);
                break; 
            } 
        }

        return i;
    }

    private static void addNewComponents(List<String> output) {
        if (!componentsList.isEmpty()) {
            if (DEBUG) {
                System.out.println();
                System.out.println("=======================================");
                System.out.println("          Add new components           ");
                System.out.println("=======================================");
            }

            for (Component comp : componentsList) {
                output.add(comp.name + ":");
                output.add("  vars:");
                if (comp.branch != null) {
                    output.add("    component_branch: " + comp.branch);
                }
                output.add("    component_tag: " + comp.trunk);

                if (DEBUG) {
                    System.out.println(comp);
                }
            }
        }
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
        if (comp.branch != null && !haveBranch(input, index)) {
            output.add("    component_branch: " + comp.branch);
        }

        while (index < input.size() && input.get(index).startsWith(" ".repeat(4))) {
            if (input.get(index).contains("component_branch")) {
                output.add(comp.branch == null ? input.get(index) : "    component_branch: " + comp.branch);
            } else if (input.get(index).contains("component_tag")) {
                output.add("    component_tag: " + comp.trunk);
            }
            index++;
        }

        return index - 1;
    }

    private static boolean haveBranch(List<String> list, int index) {
        while (list.get(index).startsWith(" ".repeat(4))) {
            if (list.get(index).contains("component_branch")) {
                return true;
            }
            index++;
        }

        return false;
    }

    private static void parseComponentsFromFile(Path srcFile) throws IOException {
        List<String> lines = removeExtraChars(Files.readAllLines(srcFile, StandardCharsets.UTF_8));
        String[] wordsArray = lines.stream().collect(Collectors.joining("\n")).split(":\\n|:|\\n");

        if (wordsArray[0].equals("component_branch") && wordsArray[0].equals("component_tag") && wordsArray[0].matches(".*\\d.*")) {
            System.err.println("Incorrect list format");
            System.exit(-1);
        }

        if (DEBUG) {
            System.out.println("=======================================");
            System.out.println("             Parsed words              ");
            System.out.println("=======================================");
            
            for (int i = 0; i < wordsArray.length; i++) {
                System.out.println(i + ": " + wordsArray[i]);
            }
            System.out.println();
        }

        int index = 0;
        while (index < wordsArray.length) {
            index = parseComponent(wordsArray, index);
        }

        botkeeperFix();
    }

    private static int parseComponent(String[] words, int index) {
        String name = words[index++];

        String branch = new String();
        String trunk = new String();

        if (words[index].equals("component_branch")) {
            branch = words[index + 1];
            index += 2;
        } 

        if (words[index].equals("component_tag")) {
            trunk = words[index + 1];
            index += 2;

            if (index > words.length && branch.length() == 0 && words[index].equals("component_branch")) {
                branch = words[index + 1];
                index += 2;
            }
        } else {
            System.err.println("Incorrect list format (" + words[index] + ")");
            System.exit(-1);
        }

        componentsList.add(branch.length() > 0 ? new Component(name, branch, trunk) : new Component(name, trunk));

        return index;
    }

    private static void botkeeperFix() {
        boolean haveBotkeeperServer = componentsList.stream().anyMatch(c -> c.name.equals("botkeeper-server"));
        boolean haveBotkeeperFront = componentsList.stream().anyMatch(c -> c.name.equals("botkeeper-front"));

        if(haveBotkeeperServer && !haveBotkeeperFront) {
            for (int i = 0; i < componentsList.size(); i++) {
                if (componentsList.get(i).name.equals("botkeeper-server")) {
                    componentsList.add(new Component("botkeeper-front", componentsList.get(i).branch, componentsList.get(i).trunk));
                    break;
                }
            }
        }
    }

    private static List<String> removeExtraChars(List<String> list) {
        return list.stream()
            .map(s -> s.replaceAll("telephony-health-check", "telephony_healthcheck")) 
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