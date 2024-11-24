import os

# Class to represent a line of assembly code
class AssemblyLine:
    def __init__(self, label, instruction, operand1, operand2, operand3, comment, lc):
        self.label = label
        self.instruction = instruction
        self.operand1 = operand1
        self.operand2 = operand2
        self.operand3 = operand3
        self.comment = comment
        self.lc = lc

    def __str__(self):
        return (f"LC: {self.lc}\t, Label: {self.label}\t, Instruction: {self.instruction} "
                f"\t, Operand1: {self.operand1}\t, Operand2: {self.operand2}")


# Class to represent opcode, size, and type
class OpcodeInfo:
    def __init__(self, opcode, size, type_):
        self.opcode = opcode
        self.size = size
        self.type_ = type_


def create_machine_opcode_table():
    mot = {
        "MOVER": OpcodeInfo("20", 2, "IS"),
        "ADD": OpcodeInfo("21", 2, "IS"),
        "CMP": OpcodeInfo("22", 2, "IS"),
        "BNE": OpcodeInfo("23", 2, "IS"),
        "START": OpcodeInfo("24", 0, "AD"),
        "END": OpcodeInfo("25", 0, "AD"),
        "A": OpcodeInfo("1", 0, "R"),
        "B": OpcodeInfo("2", 0, "R"),
        "C": OpcodeInfo("3", 0, "R"),
        "D": OpcodeInfo("4", 0, "R"),
        "JUMP": OpcodeInfo("26", 3, "IS"),  # Added JUMP instruction
        "DC": OpcodeInfo("27", 0, "DC"),  # Added DC instruction
    }
    return mot


def parse_assembly_code(code, mot):
    lines = []
    lc = 100  # Initialize Location Counter at 100 (as per your example)
    symbol_table = {}
    symbol_index = 1  # Index for symbols
    
    for line in code.splitlines():
        trimmed_line = line.strip()
        if not trimmed_line or trimmed_line.startswith(";"):
            continue

        label, instruction, operand1, operand2, operand3, comment = None, None, None, None, None, None
        parts = trimmed_line.split()
        part_index = 0

        if parts[0].endswith(":"):
            label = parts[0][:-1]
            part_index += 1

        if part_index < len(parts):
            instruction = parts[part_index]
            part_index += 1

        if instruction and instruction.upper() == "START" and part_index < len(parts):
            try:
                lc = int(parts[part_index])
                part_index += 1
            except ValueError:
                lc = 0

        if part_index < len(parts):
            operands = " ".join(parts[part_index:]).split(",")
            operand1 = operands[0].strip() if len(operands) > 0 else None
            operand2 = operands[1].strip() if len(operands) > 1 else None
            operand3 = operands[2].strip() if len(operands) > 2 else None

        if ";" in trimmed_line:
            comment = trimmed_line.split(";", 1)[1]

        lines.append(AssemblyLine(label, instruction, operand1, operand2, operand3, comment, lc))

        # Update symbol table
        if label and label not in symbol_table:
            symbol_table[label] = symbol_index
            symbol_index += 1
        
        # If operands are symbols, add them to the symbol table
        for operand in [operand1, operand2, operand3]:
            if operand and not operand.isdigit() and operand not in mot and operand not in symbol_table:
                symbol_table[operand] = symbol_index
                symbol_index += 1
        
        opcode_info = mot.get(instruction)
        if opcode_info:
            lc += opcode_info.size
        else:
            lc += 1  # Default increment

    return lines, symbol_table


def generate_intermediate_code(line, opcode_info, symbol_table, mot):
    if not opcode_info:
        return "Unknown"

    ic = f"({opcode_info.type_},{opcode_info.opcode})"
    
    # Handle operands
    def get_operand_info(operand, symbol_table, mot):
        # If operand is a register (e.g., A, B, C, D), treat it as a register (R)
        if operand and operand.upper() in mot and mot[operand.upper()].type_ == "R":
            return f"(R,{mot[operand.upper()].opcode})"
        # If operand is numeric, treat it as a constant (C)
        elif operand and operand.isdigit():
            return f"(C,{operand})"
        # If operand is a symbol, find its index in the symbol table
        elif operand and operand in symbol_table:
            return f"(ST,{symbol_table[operand]})"
        return None  # If operand is not recognized
    
    # Only append operand info if it's not None
    if line.operand1:
        operand_info = get_operand_info(line.operand1, symbol_table, mot)
        if operand_info:
            ic += " " + operand_info
    if line.operand2:
        operand_info = get_operand_info(line.operand2, symbol_table, mot)
        if operand_info:
            ic += " " + operand_info
    if line.operand3:
        operand_info = get_operand_info(line.operand3, symbol_table, mot)
        if operand_info:
            ic += " " + operand_info
        
    return ic


def main():
    mot = create_machine_opcode_table()

    try:
        with open("C:\\1VU\\TY\\System Programming\\SPL Practice\\pass1 assembler\\assembly_code_ass1.txt", "r") as f:
            code = f.read()
    except FileNotFoundError:
        print("Error: assembly_code_ass1.txt not found.")
        return

    lines, symbol_table = parse_assembly_code(code, mot)

    try:
        with open("C:\\1VU\\TY\\System Programming\\SPL Practice\\pass1 assembler\\output_ass1.txt", "w") as f:
            f.write("Intermediate Code:\n")
            for line in lines:
                f.write(f"LC: {line.lc} ")
                opcode_info = mot.get(line.instruction)
                if opcode_info:
                    f.write(generate_intermediate_code(line, opcode_info, symbol_table, mot) + "\n")
                else:
                    f.write("Machine Opcode: Unknown\n")
                    f.write("Intermediate Code: Unknown\n")

            f.write("\nSymbol Table:\n")
            for label, index in symbol_table.items():
                f.write(f"Label: {label}, Index: {index}\n")
    except:
        print("Error: unable to write to output_ass1.txt")

if __name__ == "__main__":
    main()
