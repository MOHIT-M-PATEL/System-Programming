# Import required libraries
import os

# Define tables
reloc_tabs = []  # RELOCTAB for each module
link_tabs = []  # LINKTAB for each module, includes both Public and EXTERN variables with LCs
global_ntab = {}  # NTAB for global Public symbols and linked addresses
combined_code = []  # Combined code with LC values for display


# Helper functions
def read_module(filename):
    """
    Read a module file and extract address-sensitive instructions,
    ENTRY, and EXTERN symbols along with their LCs. Also, create a
    complete symbol table for public symbols, labels, and externs.
    """
    with open(filename, 'r') as file:
        lines = file.readlines()

    instructions = []
    entry_symbols = []
    extern_symbols = []
    symbol_table = {}  # Full symbol table including labels, public symbols, and externs
    public_symbols = {}  # To track public symbols and their first declaration LCs

    lc = 0  # Location Counter for each module
    for line in lines:
        if ";" in line:
            line = line.split(";")[0]
        line = line.strip()
        if not line or line.startswith(';'):  # Ignore empty lines and comments
            continue
        if line.startswith('START') or line.startswith('END'):
            continue

        # Process each line
        if line.startswith("ENTRY"):
            entry_symbol = line.split()[1]
            entry_symbols.append((entry_symbol, lc))
            continue  # Skip processing this as an instruction

        elif line.startswith("EXTERN"):
            extern_symbol = line.split()[1]
            extern_symbols.append(extern_symbol)  # Capture the symbol name
            symbol_table[extern_symbol] = lc
            # Don't assign LC for externs, only record them in the symbol table if needed later
            continue  # Skip processing this as an instruction

        elif "DC" in line:  # Check for Declare Constant
            parts = line.split()
            symbol = parts[1]
            value = parts[2]
            public_symbols[symbol] = lc  # Assign the current LC to the public symbol
            symbol_table[symbol] = lc  # Track symbol in the symbol table
            instructions.append((lc, value))  # Append value instead of instruction
            lc += 1  # Increment LC for DC

        elif "DS" in line:  # Check for Declare Static
            parts = line.split()
            symbol = parts[1]
            size = parts[2]
            public_symbols[symbol] = lc  # Assign the current LC to the public symbol
            symbol_table[symbol] = lc  # Track symbol in the symbol table
            instructions.append((lc, "-"))  # Append size instead of instruction
            lc += int(size)  # Increment LC for DS
        else:
            # Detect labels if present (format: LABEL:)

            # Assuming it's an address-sensitive instruction if not ENTRY/EXTERN
            instructions.append((lc, line))
            lc += 1  # Increment LC for valid instructions
        # print(symbol_table)
    # Update the global NTAB with the addresses of public symbols
    for symbol, addr in public_symbols.items():
        global_ntab[symbol] = addr

    return instructions, entry_symbols, extern_symbols, lc, symbol_table  # Return symbol_table directly


def process_module(filename, base_address):
    """
    Process a module file, creating RELOCTAB, LINKTAB, and updating NTAB.
    """
    reloc_tab = []  # RELOCTAB for the current module
    link_tab = []  # LINKTAB for the current module
    symbol_table = {}  # Symbol table for tracking labels and variables within the module

    # Read module data and get module length
    instructions, entry_symbols, extern_symbols, module_length, public_symbols = read_module(filename)

    # Populate the symbol table with public symbols and labels
    symbol_table.update(public_symbols)  # Add public symbols with their addresses
    for lc, instr in instructions:
        if ':' in instr:  # Detect labels in the format "LABEL:"
            label = instr.split(':')[0]
            symbol_table[label] = lc

    # Record EXTERN symbols in LINKTAB with None as placeholders
    for extern_symbol in extern_symbols:
        link_tab.append((extern_symbol, "EXTERN", None))  # Use None as a placeholder

    # Process instructions to identify address-sensitive ones and fill RELOCTAB
    for lc, instr in instructions:
        for symbol in symbol_table.keys():
            # Check if the instruction contains a reference to a symbol (address-sensitive)
            if symbol in instr:
                reloc_tab.append(base_address + lc)  # Adjust LC to start from the base address
                break  # Break as soon as one symbol is found, since it's address-sensitive

    # Record Public symbols in LINKTAB with adjusted addresses
    for symbol, lc in public_symbols.items():
        if symbol not in extern_symbols:
            adjusted_address = base_address + lc
            link_tab.append((symbol, "Public", adjusted_address))  # Adjusted LC for base address
            global_ntab[symbol] = adjusted_address  # Update NTAB with the correct adjusted address
       
           

    # Generate combined code output with adjusted LCs for address-sensitive instructions only
    for lc, instr in instructions:
        adjusted_instr = instr
        # Only include address-sensitive instructions in the combined code display
        if any(symbol in instr for symbol in symbol_table.keys()):
            adjusted_instr = f"{instr}"  # Just for clarity in display, mark as adjusted
        combined_code.append((base_address + lc, adjusted_instr))
    
    return reloc_tab, link_tab, module_length  # Return only needed data


def linker(modules, link_origin):
    """
    Perform linking and relocation for the modules starting from link_origin.
    """
    base_address = link_origin

    for module_file in modules:
        # Process each module and get RELOCTAB, LINKTAB, and module length
        reloc_tab, link_tab, module_length = process_module(module_file, base_address)

        # Store tables
        reloc_tabs.append(reloc_tab)
        link_tabs.append(link_tab)

        # Update base address for the next module based on module length
        base_address += module_length

    # Resolve EXTERN addresses in LINKTAB using global NTAB
    for link_tab in link_tabs:
        for i in range(len(link_tab)):
            symbol, symbol_type, addr = link_tab[i]
            if symbol_type == "EXTERN":
                # Assign the address from global_ntab if it exists
               
                if (symbol) in global_ntab.keys():
                    link_tab[i] = (symbol, symbol_type, global_ntab[symbol])  # Update with actual address


# Predefined values for testing
link_origin = int(input("Enter link origin: "))  # Example starting address
modules = ["module1.txt", "module2.txt", "module3.txt", "module4.txt"]  # Module file names

# Perform linking
linker(modules, link_origin)

# Display combined code with LC values
print("\nCombined Code with LC values:")
for lc, instr in combined_code:
    print(f"LC: {lc} -> {instr}")

# Output RELOCTAB, LINKTAB with both Public and EXTERN variables, and NTAB tables
for i, (reloc_tab, link_tab) in enumerate(zip(reloc_tabs, link_tabs), start=1):
    print(f"\nModule {i}:")
    print("RELOCTAB:", reloc_tab)
    print("LINKTAB:", link_tab)  # LINKTAB now includes both Public and EXTERN entries

# Display Global NTAB (Public symbols) with their LCs
print("\nGlobal NTAB (Public symbols with LC):")
for symbol, lc in global_ntab.items():
    print(f"{symbol} -> LC: {lc}")