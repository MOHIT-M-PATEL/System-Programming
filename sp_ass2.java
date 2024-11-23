import java.io.*;
import java.util.*;

public class sp_ass2 { // Change the class name to sp_ass2
    static final int MAX_SYMBOLS = 100;
    static final int MAX_CODE_LINES = 100;
    static final int MAX_LITERALS = 100;
    static final int MAX_POOLS = 100;
    static final int MAX_LINE_LENGTH = 100;

    static class MOTEntry {
        String name;
        String type;
        int opcode;

        MOTEntry(String name, String type, int opcode) {
            this.name = name;
            this.type = type;
            this.opcode = opcode;
        }
    }

    static class Register {
        String name;
        int code;

        Register(String name, int code) {
            this.name = name;
            this.code = code;
        }
    }

    static class Symbol {
        String name;
        int address;

        Symbol(String name, int address) {
            this.name = name;
            this.address = address;
        }
    }

    static class Literal {
        String name;
        int address;
        int pool_no;

        Literal(String name, int address, int pool_no) {
            this.name = name;
            this.address = address;
            this.pool_no = pool_no;
        }
    }

    static class Pool {
        int start_index;
        int literal_count;

        Pool(int start_index, int literal_count) {
            this.start_index = start_index;
            this.literal_count = literal_count;
        }
    }

    static class IntermediateCode {
        String operation;
        int code;
        int reg;
        String operandType;
        int operandValue;
        int lc;

        IntermediateCode(String operation, int code, int reg, String operandType, int operandValue, int lc) {
            this.operation = operation;
            this.code = code;
            this.reg = reg;
            this.operandType = operandType;
            this.operandValue = operandValue;
            this.lc = lc;
        }
    }

    static MOTEntry[] mot = {
            new MOTEntry("STOP", "IS", 0),
            new MOTEntry("ADD", "IS", 1),
            new MOTEntry("SUB", "IS", 2),
            new MOTEntry("MUL", "IS", 3),
            new MOTEntry("MOVER", "IS", 4),
            new MOTEntry("MOV", "IS", 5),
            new MOTEntry("COMP", "IS", 6),
            new MOTEntry("BC", "IS", 7),
            new MOTEntry("DIV", "IS", 8),
            new MOTEntry("READ", "IS", 9),
            new MOTEntry("PRINT", "IS", 10),
            new MOTEntry("START", "AD", 1),
            new MOTEntry("END", "AD", 2),
            new MOTEntry("ORIGIN", "AD", 3),
            new MOTEntry("EQU", "AD", 4),
            new MOTEntry("LTORG", "AD", 5),
            new MOTEntry("DS", "DL", 1),
            new MOTEntry("DC", "DL", 2)
    };

    static Register[] registers = {
            new Register("AREG", 1),
            new Register("BREG", 2),
            new Register("CREG", 3),
            new Register("DREG", 4)
    };

    static ArrayList<Symbol> symbol_table = new ArrayList<>();
    static ArrayList<Literal> literal_table = new ArrayList<>();
    static ArrayList<Pool> pool_table = new ArrayList<>();
    static ArrayList<IntermediateCode> intermediate_code = new ArrayList<>();
    static int location_counter = 0;
    static int pool_count = 0;

    static void process_line(String line) {
        String[] tokens = line.split("[ ,\t]+");
        if (tokens.length == 0) return;

        String label = null, opcode = null, operand1 = null, operand2 = null;
        int tokenIndex = 0;

        // Check if first token is a label
        if (tokens[tokenIndex].endsWith(":")) {
            label = tokens[tokenIndex].substring(0, tokens[tokenIndex].length() - 1);
            tokenIndex++;
        }

        if (tokenIndex < tokens.length) opcode = tokens[tokenIndex++];
        if (tokenIndex < tokens.length) operand1 = tokens[tokenIndex++];
        if (tokenIndex < tokens.length) operand2 = tokens[tokenIndex];

        // Process label if exists
        if (label != null) {
            symbol_table.add(new Symbol(label, location_counter));
        }

        // Process opcode and operands
        if (opcode != null) {
            boolean found = false;
            for (MOTEntry entry : mot) {
                if (entry.name.equals(opcode)) {
                    found = true;
                    if (entry.type.equals("AD")) {
                        processADInstruction(entry, opcode, operand1);
                    } else if (entry.type.equals("IS")) {
                        processISInstruction(entry, operand1, operand2);
                    } else if (entry.type.equals("DL")) {
                        processDLInstruction(entry, opcode, operand1);
                    }
                    break;
                }
            }
            if (!found) {
                System.out.println("Error: Invalid instruction " + opcode);
            }
        }
    }

    static void processADInstruction(MOTEntry entry, String opcode, String operand1) {
        if (opcode.equals("START")) {
            if (operand1 != null) {
                location_counter = Integer.parseInt(operand1);
                intermediate_code.add(new IntermediateCode("AD", entry.opcode, -1, "C", location_counter, location_counter));
            } else {
                System.out.println("Error: Missing operand for START directive");
            }
        } else if (opcode.equals("END")) {
            intermediate_code.add(new IntermediateCode("AD", entry.opcode, -1, null, 0, location_counter));
            fill_literal_addresses();
        } else if (opcode.equals("LTORG")) {
            intermediate_code.add(new IntermediateCode("AD", entry.opcode, -1, null, 0, location_counter));

            fill_literal_addresses();
            pool_count++;
            pool_table.add(new Pool(literal_table.size(), 0));
        }
    }


    static void processISInstruction(MOTEntry entry, String operand1, String operand2) {
        int reg_code1 = -1;
        int reg_code2 = -1;

        // Process first operand (always a register for MOVER)
        for (Register reg : registers) {
            if (reg.name.equals(operand1)) {
                reg_code1 = reg.code;
                break;
            }
        }
        if (reg_code1 == -1) {
            System.out.println("Error: Invalid register " + operand1);
            return;
        }

        IntermediateCode ic = new IntermediateCode("IS", entry.opcode, reg_code1, null, 0, location_counter);

        // Process second operand
        if (operand2 != null) {
            if (operand2.startsWith("'")) {
                // Literal
                literal_table.add(new Literal(operand2, -1, pool_count));
                pool_table.get(pool_count).literal_count++;
                ic.operandType = "L";
                ic.operandValue = literal_table.size() - 1;
            } else {
                // Check if it's a register
                for (Register reg : registers) {
                    if (reg.name.equals(operand2)) {
                        reg_code2 = reg.code;
                        break;
                    }
                }
                if (reg_code2 != -1) {
                    // It's a register
                    ic.operandType = "R";
                    ic.operandValue = reg_code2;
                } else {
                    // Assume it's a symbol
                    int sym_index = -1;
                    for (int j = 0; j < symbol_table.size(); j++) {
                        if (symbol_table.get(j).name.equals(operand2)) {
                            sym_index = j;
                            break;
                        }
                    }
                    if (sym_index == -1) {
                        symbol_table.add(new Symbol(operand2, location_counter));
                        sym_index = symbol_table.size() - 1;
                    }
                    ic.operandType = "S";
                    ic.operandValue = sym_index;
                }
            }
        }

        intermediate_code.add(ic);
        location_counter++;
    }

    static void processDLInstruction(MOTEntry entry, String opcode, String operand1) {
        if (opcode.equals("DS")) {
            int size = Integer.parseInt(operand1);
            symbol_table.get(symbol_table.size() - 1).address = location_counter;
            intermediate_code.add(new IntermediateCode("DL", entry.opcode, -1, "C", size, location_counter));
            location_counter += size;
        } else if (opcode.equals("DC")) {
            int value = Integer.parseInt(operand1);
            symbol_table.get(symbol_table.size() - 1).address = location_counter;
            intermediate_code.add(new IntermediateCode("DL", entry.opcode, -1, "C", value, location_counter));
            location_counter++;
        }
    }

    static void read_file_and_process_lines(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                process_line(line.trim());
            }
        } catch (IOException e) {
            System.out.println("Error: Unable to open file " + filename);
        }
    }

    static void fill_literal_addresses() {
        for (int i = pool_table.get(pool_count).start_index; i < literal_table.size(); i++) {
            if (literal_table.get(i).address == -1) {
                literal_table.get(i).address = location_counter++;
            }
        }
    }

    static void write_output_to_file(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {

            // Write Intermediate Code with LC (Variant I form)
            bw.write("Intermediate Code with LC (Variant I form):\n");
            for (IntermediateCode ic : intermediate_code) {
                bw.write(ic.lc + " (" + ic.operation + ", " + ic.code + ")");
                if (ic.reg != -1) {
                    bw.write(" (R, " + ic.reg + ")");
                }
                if (ic.operandType != null) {
                    bw.write(" (" + ic.operandType + ", " + ic.operandValue + ")");
                }
                bw.write("\n");
            }

            // Write Pool Table
            bw.write("\nPool Table:\n");
            for (Pool pool : pool_table) {
                bw.write(pool.start_index + "\n");
            }

            // Write Literal Table
            bw.write("\nLiteral Table:\n");
            for (int i = 0; i < literal_table.size(); i++) {
                Literal lit = literal_table.get(i);
                bw.write(i + " " + lit.name + " " + lit.address + "\n");
            }

            // Write Symbol Table
            bw.write("\nSymbol Table:\n");
            for (int i = 0; i < symbol_table.size(); i++) {
                Symbol sym = symbol_table.get(i);
                bw.write(i + " " + sym.name + " " + sym.address + "\n");
            }

        } catch (IOException e) {
            System.out.println("Error: Unable to write to file " + filename);
        }
    }


    public static void main(String[] args) {
        // Adjust the input ALP filename and output filename accordingly
        String input_filename = "assembly_code.txt";
        String output_filename = "output.txt";

        pool_table.add(new Pool(0, 0));
        read_file_and_process_lines(input_filename);
        write_output_to_file(output_filename);

        System.out.println("Pass-1 Assembler completed. Output written to " + output_filename);
    }
}
