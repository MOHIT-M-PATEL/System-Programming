import re

# Constants
MAX_SYMBOLS = 100
MAX_CODE_LINES = 100
MAX_LITERALS = 100
MAX_POOLS = 100
MAX_LINE_LENGTH = 100

# Define classes for the different components
class MOTEntry:
    def __init__(self, name, type_, opcode):
        self.name = name
        self.type = type_
        self.opcode = opcode

class Register:
    def __init__(self, name, code):
        self.name = name
        self.code = code

class Symbol:
    def __init__(self, name, address):
        self.name = name
        self.address = address

class Literal:
    def __init__(self, name, address, pool_no):
        self.name = name
        self.address = address
        self.pool_no = pool_no

class Pool:
    def __init__(self, start_index, literal_count):
        self.start_index = start_index
        self.literal_count = literal_count

class IntermediateCode:
    def __init__(self, operation, code, reg, operandType, operandValue, lc):
        self.operation = operation
        self.code = code
        self.reg = reg
        self.operandType = operandType
        self.operandValue = operandValue
        self.lc = lc


# Initialize global variables
mot = [
    MOTEntry("STOP", "IS", 0),
    MOTEntry("ADD", "IS", 1),
    MOTEntry("SUB", "IS", 2),
    MOTEntry("MUL", "IS", 3),
    MOTEntry("MOVER", "IS", 4),
    MOTEntry("MOV", "IS", 5),
    MOTEntry("COMP", "IS", 6),
    MOTEntry("BC", "IS", 7),
    MOTEntry("DIV", "IS", 8),
    MOTEntry("READ", "IS", 9),
    MOTEntry("PRINT", "IS", 10),
    MOTEntry("START", "AD", 1),
    MOTEntry("END", "AD", 2),
    MOTEntry("ORIGIN", "AD", 3),
    MOTEntry("EQU", "AD", 4),
    MOTEntry("LTORG", "AD", 5),
    MOTEntry("DS", "DL", 1),
    MOTEntry("DC", "DL", 2)
]

registers = [
    Register("AREG", 1),
    Register("BREG", 2),
    Register("CREG", 3),
    Register("DREG", 4)
]

symbol_table = []
literal_table = []
pool_table = []
intermediate_code = []
location_counter = 500
pool_count = 0


def process_line(line):
    global location_counter, pool_count

    tokens = re.split(r'[\s,]+', line.strip())
    if not tokens:
        return

    label = None
    opcode = None
    operand1 = None
    operand2 = None
    tokenIndex = 0

    # Check if first token is a label
    if tokens[tokenIndex].endswith(":"):
        label = tokens[tokenIndex][:-1]
        tokenIndex += 1

    if tokenIndex < len(tokens):
        opcode = tokens[tokenIndex]
        tokenIndex += 1
    if tokenIndex < len(tokens):
        operand1 = tokens[tokenIndex]
        tokenIndex += 1
    if tokenIndex < len(tokens):
        operand2 = tokens[tokenIndex]

    # Process label
    if label:
        symbol_table.append(Symbol(label, location_counter))

    # Process opcode and operands
    if opcode:
        found = False
        for entry in mot:
            if entry.name == opcode:
                found = True
                if entry.type == "AD":
                    process_ad_instruction(entry, opcode, operand1)
                elif entry.type == "IS":
                    process_is_instruction(entry, operand1, operand2)
                elif entry.type == "DL":
                    process_dl_instruction(entry, opcode, operand1)
                break
        if not found:
            print(f"Error: Invalid instruction {opcode}")


def process_ad_instruction(entry, opcode, operand1):
    global location_counter, pool_count

    if opcode == "START":
        if operand1:
            location_counter = int(operand1)
            intermediate_code.append(IntermediateCode("AD", entry.opcode, -1, "C", location_counter, location_counter))
        else:
            print("Error: Missing operand for START directive")
    elif opcode == "END":
        intermediate_code.append(IntermediateCode("AD", entry.opcode, -1, None, 0, location_counter))
        fill_literal_addresses()
    elif opcode == "LTORG":
        intermediate_code.append(IntermediateCode("AD", entry.opcode, -1, None, 0, location_counter))
        fill_literal_addresses()
        pool_count += 1
        pool_table.append(Pool(len(literal_table), 0))


def process_is_instruction(entry, operand1, operand2):
    global location_counter  # Ensure location_counter is global
    global pool_count  # Ensure pool_count is global
    
    reg_code1 = -1
    reg_code2 = -1

    # Process first operand (always a register for MOVER)
    for reg in registers:
        if reg.name == operand1:
            reg_code1 = reg.code
            break

    if reg_code1 == -1:
        print(f"Error: Invalid register {operand1}")
        return

    ic = IntermediateCode("IS", entry.opcode, reg_code1, None, 0, location_counter)

    # Process second operand
    if operand2:
        if operand2.startswith("'"):
            # Literal
            literal_table.append(Literal(operand2, -1, pool_count))
            
            # Ensure pool_count index exists in pool_table
            if pool_count >= len(pool_table):
                pool_table.append(Pool(len(literal_table) - 1, 0))
            
            pool_table[pool_count].literal_count += 1
            ic.operandType = "L"
            ic.operandValue = len(literal_table) - 1
        else:
            # Check if it's a register
            for reg in registers:
                if reg.name == operand2:
                    reg_code2 = reg.code
                    break
            if reg_code2 != -1:
                ic.operandType = "R"
                ic.operandValue = reg_code2
            else:
                # Assume it's a symbol
                symbol_index = -1
                for i, symbol in enumerate(symbol_table):
                    if symbol.name == operand2:
                        symbol_index = i
                        break
                if symbol_index == -1:
                    symbol_table.append(Symbol(operand2, location_counter))
                    symbol_index = len(symbol_table) - 1
                ic.operandType = "S"
                ic.operandValue = symbol_index

    intermediate_code.append(ic)
    location_counter += 1



def process_dl_instruction(entry, opcode, operand1):
    global location_counter

    if opcode == "DS":
        size = int(operand1)
        symbol_table[-1].address = location_counter
        intermediate_code.append(IntermediateCode("DL", entry.opcode, -1, "C", size, location_counter))
        location_counter += size
    elif opcode == "DC":
        value = int(operand1)
        symbol_table[-1].address = location_counter
        intermediate_code.append(IntermediateCode("DL", entry.opcode, -1, "C", value, location_counter))
        location_counter += 1


def fill_literal_addresses():
    global location_counter

    for i in range(pool_table[pool_count].start_index, len(literal_table)):
        if literal_table[i].address == -1:
            literal_table[i].address = location_counter
            location_counter += 1


def write_output_to_file(filename):
    with open(filename, "w") as f:
        # Write Intermediate Code with LC (Variant I form)
        f.write("Intermediate Code with LC (Variant I form):\n")
        for ic in intermediate_code:
            f.write(f"{ic.lc} ({ic.operation}, {ic.code})")
            if ic.reg != -1:
                f.write(f" (R, {ic.reg})")
            if ic.operandType:
                f.write(f" ({ic.operandType}, {ic.operandValue})")
            f.write("\n")

        # Write Pool Table
        f.write("\nPool Table:\n")
        for pool in pool_table:
            f.write(f"{pool.start_index}\n")

        # Write Literal Table
        f.write("\nLiteral Table:\n")
        for i, lit in enumerate(literal_table):
            f.write(f"{i} {lit.name} {lit.address}\n")

        # Write Symbol Table
        f.write("\nSymbol Table:\n")
        for i, sym in enumerate(symbol_table):
            f.write(f"{i} {sym.name} {sym.address}\n")


def main(input_file):
    # Read the ALP program from the input file
    with open(input_file, "r") as file:
        alp_program = file.readlines()

    for line in alp_program:
        process_line(line.strip())

    write_output_to_file("C:\\1VU\\TY\\System Programming\\SPL Practice\\2. pass1 literal\\output_ass2.txt")
    print("Pass-1 Assembler completed. Output written to output.txt")


if __name__ == "__main__":
    input_file = "C:\\1VU\\TY\\System Programming\\SPL Practice\\2. pass1 literal\\assembly_code_ass2.txt"  # Name of the input file with ALP code
    main(input_file)
