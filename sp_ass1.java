import java.nio.file.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class sp_ass1 {

    // A class to represent a line of assembly code
    static class AssemblyLine {
        String label;
        String instruction;	
        String operand1;
        String operand2;
        String operand3;
        String comment;
        int lc;  // Location Counter

        AssemblyLine(String label, String instruction, String operand1, String operand2, String operand3, String comment, int lc) {
            this.label = label;
            this.instruction = instruction;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.operand3 = operand3;
            this.comment = comment;
            this.lc = lc;
        }

        @Override
        public String toString() {
            return "LC: " + lc + "\t, Label: " + label + "\t, Instruction: " + instruction +
                   "\t, Operand1: " + operand1 + "\t, Operand2: " + operand2;
        }
    }

    // A class to represent opcode, size, and type
    static class OpcodeInfo {
        String opcode;
        int size;
        String type;

        OpcodeInfo(String opcode, int size, String type) {
            this.opcode = opcode;
            this.size = size;
            this.type = type;
        }
    }

    public static void main(String[] args) {
        // Create the Machine Opcode Table (MOT)
        Map<String, OpcodeInfo> mot = createMachineOpcodeTable();

        // Read the assembly code from a text file
        String code = "";
        try {
            code = new String(Files.readAllBytes(Paths.get("assembly_code_ass1.txt")));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        List<AssemblyLine> lines = parseAssemblyCode(code, mot);
        Map<String, Integer> symbolTable = createSymbolTable(lines);

        // Write the results to output.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output_ass1.txt"))) {
            writer.write("Intermediate Code:\n");
            for (AssemblyLine assemblyLine : lines) {
                writer.write("LC: " + assemblyLine.lc + " ");
                OpcodeInfo opcodeInfo = mot.get(assemblyLine.instruction);
                if (opcodeInfo != null) {
                    //writer.write("(" + opcodeInfo.type + "," + opcodeInfo.opcode + ") ");
                    writer.write(generateIntermediateCode(assemblyLine, opcodeInfo) + "\n");
                } else {
                    writer.write("Machine Opcode: Unknown\n");
                    writer.write("Intermediate Code: Unknown\n");
                }
            }

            // Print DS (Declarative Statements) and their LC
            writer.write("\nDeclarative Statements:\n");
            for (AssemblyLine line : lines) {
                OpcodeInfo opcodeInfo = mot.get(line.instruction);
                if (opcodeInfo != null && "DC".equals(opcodeInfo.type)) {
                    writer.write("LC: " + line.lc + " Instruction: " + line.instruction + " Operand1: " + line.operand1 + "\n");
                }
            }

            // Print the Symbol Table
            writer.write("\nSymbol Table:\n");
            for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
                writer.write("Label: " + entry.getKey() + ", LC: " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<AssemblyLine> parseAssemblyCode(String code, Map<String, OpcodeInfo> mot) {
        List<AssemblyLine> lines = new ArrayList<>();
        String[] codeLines = code.split("\n");

        int lc = 0; // Initialize Location Counter

        for (int i = 0; i < codeLines.length; i++) {
            String trimmedLine = codeLines[i].trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith(";")) {
                continue;
            }

            String label = null;
            String instruction = null;
            String operand1 = null;
            String operand2 = null;
            String operand3 = null;
            String comment = null;

            String[] parts = trimmedLine.split("\\s+");

            int partIndex = 0;

            if (parts[0].endsWith(":")) {
                label = parts[0].substring(0, parts[0].length() - 1);
                partIndex++;
            }

            if (partIndex < parts.length) {
                instruction = parts[partIndex];
                partIndex++;
            }

            if (i == 0 && "START".equalsIgnoreCase(instruction) && partIndex < parts.length) {
                try {
                    lc = Integer.parseInt(parts[partIndex]);
                    partIndex++;
                } catch (NumberFormatException e) {
                    lc = 0; // Default to 0 if the operand is not a valid number
                }
            }

            if (partIndex < parts.length) {
                String operandsPart = "";
                StringBuilder operandBuilder = new StringBuilder();
                while (partIndex < parts.length && !parts[partIndex].startsWith(";")) {
                    if (operandBuilder.length() > 0) {
                        operandBuilder.append(" ");
                    }
                    operandBuilder.append(parts[partIndex]);
                    partIndex++;
                }
                operandsPart = operandBuilder.toString();
                String[] operands = operandsPart.split(",");

                if (operands.length > 0) {
                    operand1 = operands[0].trim();
                }
                if (operands.length > 1) {
                    operand2 = operands[1].trim();
                }
                if (operands.length > 2) {
                    operand3 = operands[2].trim();
                }
            }

            if (partIndex < parts.length && parts[partIndex].startsWith(";")) {
                comment = trimmedLine.substring(trimmedLine.indexOf(";"));
            }

            lines.add(new AssemblyLine(label, instruction, operand1, operand2, operand3, comment, lc));

            // Increment the Location Counter based on instruction size
            OpcodeInfo opcodeInfo = mot.get(instruction);
            if (opcodeInfo != null) {
                if ("DS".equals(opcodeInfo.type)) {
                    // For DS instructions (like DC), don't increment LC
                } else {
                    lc += opcodeInfo.size;
                }
            } else {
                lc++; // Default size if instruction not found in MOT
            }
        }

        return lines;
    }

    public static Map<String, Integer> createSymbolTable(List<AssemblyLine> lines) {
        Map<String, Integer> symbolTable = new HashMap<>();

        for (AssemblyLine line : lines) {
            if (line.label != null && !line.label.isEmpty()) {
                symbolTable.put(line.label, line.lc);
            }
        }

        return symbolTable;
    }

    // Method to create the Machine Opcode Table (MOT)
    public static Map<String, OpcodeInfo> createMachineOpcodeTable() {
        Map<String, OpcodeInfo> mot = new HashMap<>();
        mot.put("MOVER", new OpcodeInfo("20", 2, "IS"));
        mot.put("ADD", new OpcodeInfo("21", 2, "IS"));
        mot.put("CMP", new OpcodeInfo("22", 2, "IS"));
        mot.put("BNE", new OpcodeInfo("23", 2, "IS"));
        mot.put("START", new OpcodeInfo("24", 0, "AD"));
        mot.put("END", new OpcodeInfo("25", 0, "AD"));
        mot.put("A", new OpcodeInfo("1", 0, "R"));
        mot.put("B", new OpcodeInfo("2", 0, "R"));
        mot.put("C", new OpcodeInfo("3", 0, "R"));
        mot.put("D", new OpcodeInfo("4", 0, "R"));
        mot.put("JUMP", new OpcodeInfo("26", 3, "IS")); // Added JUMP instruction
        mot.put("DC", new OpcodeInfo("27", 0, "DC")); // Added DC instruction
        return mot;
    }

    // Method to generate Intermediate Code
    public static String generateIntermediateCode(AssemblyLine line, OpcodeInfo opcodeInfo) {
        if (opcodeInfo == null) {
            return "Unknown"; // Handle unknown opcode
        }

        StringBuilder ic = new StringBuilder();
        ic.append("(").append(opcodeInfo.type).append(",").append(opcodeInfo.opcode).append(") ");
        if ("DC".equals(opcodeInfo.type) && line.operand1 != null) {
            ic.append("(C,").append(line.operand1).append(")");
        } else if (line.operand1 != null) {
            if (line.operand1.matches("\\d+")) {
                ic.append("(C,").append(line.operand1).append(")");
            } else {
                ic.append("(ST,").append(line.operand1).append(")");
            }
            if (line.operand2 != null) {
                ic.append(" (");
                if (line.operand2.matches("\\d+")) {
                    ic.append("C,").append(line.operand2);
                } else {
                    ic.append("ST,").append(line.operand2);
                }
                ic.append(")");
            }
            if (line.operand3 != null) {
                ic.append(" (");
                if (line.operand3.matches("\\d+")) {
                    ic.append("C,").append(line.operand3);
                } else {
                    ic.append("ST,").append(line.operand3);
                }
                ic.append(")");
            }
        }
        return ic.toString();
    }
}
