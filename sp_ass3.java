import java.io.*;
import java.util.*;

public class sp_ass3 {
    private static final Map<String, String[]> MOT = new HashMap<>();
    private static final Map<Integer, String> symbolTable = new HashMap<>();
    private static final List<String> intermediateCode = new ArrayList<>();
    private static final List<String> machineCode = new ArrayList<>();

    static {
        // Initialize MOT (Machine Operation Table)
    	String[][] motData = {
                {"MOVE", "20", "2", "2", "LS"},
                {"ADD", "21", "1", "2", "LS"},
                {"JUMP", "22", "1", "3", "LS"},
                {"SUB", "23", "1", "2", "LS"},
                {"MUL", "24", "1", "2", "LS"},
                {"DIV", "25", "1", "2", "LS"},
                {"START", "26", "0", "0", "AD"},
                {"END", "27", "0", "0", "AD"},
                {"DC", "1", "0", "0", "DL"},
                {"DS", "2", "0", "0", "DL"},
                {"A", "1", "0", "0", "R"},
                {"B", "2", "0", "0", "R"},
                {"C", "3", "0", "0", "R"},
                {"D", "4", "0", "0", "R"}
        };


        for (String[] entry : motData) {
            MOT.put(entry[0], Arrays.copyOfRange(entry, 1, entry.length));
        }
    }

    public static void main(String[] args) {
        readIntermediateCode("output_ass1.txt");
        printOutput();
    }

    private static void readIntermediateCode(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean isIntermediateCode = false;
            boolean isSymbolTable = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("Intermediate Code:")) {
                    isIntermediateCode = true;
                    isSymbolTable = false;
                    continue;
                } else if (line.equals("Symbol Table:")) {
                    isIntermediateCode = false;
                    isSymbolTable = true;
                    continue;
                }

                if (isIntermediateCode && !line.isEmpty()) {
                    intermediateCode.add(line);
                } else if (isSymbolTable && !line.isEmpty()) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String[] symbolInfo = parts[1].trim().split("-");
                        symbolTable.put(Integer.parseInt(parts[0].trim()), symbolInfo[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Intermediate Code Lines: " + intermediateCode.size());
        System.out.println("Symbol Table Entries: " + symbolTable.size());
    }

    private static void generateMachineCode() {
        for (String line : intermediateCode) {
            try {
                String[] parts = line.split("\\s+", 3);
                if (parts.length < 2) {
                    System.out.println("Skipping invalid line: " + line);
                    continue;
                }

                String lc = parts[0].replace("LC:", "").trim();
                StringBuilder machineInstruction = new StringBuilder(lc + " ");

                String[] tokens = parts[1].replaceAll("[()]", "").split(",");
                for (String token : tokens) {
                    String[] tokenParts = token.split(",");
                    String prefix = tokenParts[0];
                    String value = tokenParts.length > 1 ? tokenParts[1] : "";

                    switch (prefix) {
                        case "AD":
                        case "DL":
                            machineInstruction.append("--");
                            break;
                        case "ST":
                        case "LS":
                            String[] opInfo = MOT.get(value);
                            if (opInfo != null) {
                                machineInstruction.append(opInfo[0]);
                            } else {
                                machineInstruction.append(value);
                            }
                            break;
                        case "R":
                            String[] regInfo = MOT.get(value.toLowerCase());
                            if (regInfo != null) {
                                machineInstruction.append(regInfo[0]);
                            } else {
                                machineInstruction.append(value);
                            }
                            break;
                        case "C":
                            machineInstruction.append(value);
                            break;
                        default:
                            machineInstruction.append(value);
                    }
                }

                machineCode.add(machineInstruction.toString());
            } catch (Exception e) {
                System.out.println("Error processing line: " + line);
                e.printStackTrace();
            }
        }
    }

    private static void printOutput() {
        System.out.println("\nIntermediate Code:");
        for (String line : intermediateCode) {
            System.out.println(line);
        }

        System.out.println("\nSymbol Table:");
        for (Map.Entry<Integer, String> entry : symbolTable.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        System.out.println("\nMachine Code:");
        for (String line : intermediateCode){
            line = line.replace("(","");
            line = line.replace(")","");
            line = line.replace(":","");

            String[] parts= line.split(" ");
            for (String part:parts){
                String[] subpart = part.split(",");
                String prefix = subpart[0];
                //System.out.println(prefix);
                if(Objects.equals(prefix, "IS")){
                    line = line.replace(part,subpart[1]);
                } else if (Objects.equals(prefix, "AD")){
                    line = line.replace(line,"---");
                } else if (Objects.equals(prefix, "R")){
                    line = line.replace(part,subpart[1]);
                } else if (Objects.equals(prefix, "C")){
                    line = line.replace(part,subpart[1]);
                } else if (Objects.equals(prefix, "DL")){

                    if(Objects.equals(subpart[1], "1")){
                        line = line.replace(part,"");
                        line = line.replace("C,","");
                        break;
                    }
                    line = line.replace(part,"NULL Character x");
                    line = line.replace(parts[3],parts[3].split(",")[1]);
                } else if (Objects.equals(prefix, "ST")){
                    for (Map.Entry<Integer, String> entry : symbolTable.entrySet()) {
                        if(entry.getKey().equals(Integer.parseInt(subpart[1]))) {
                            line = line.replaceFirst(entry.getValue(),"");
                            line = line.replace(part,entry.getValue());
                            line = line.replace("LC","LC:");
                        }
                    }
                }
            }
            System.out.println(line);
        }
    }
}