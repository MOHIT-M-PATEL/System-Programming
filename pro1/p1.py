#wrong output in MDT

class MacroProcessor:
    def __init__(self):
        self.mnt = {}  # Macro Name Table
        self.pntab = {}  # Positional Name Table
        self.kptab = {}  # Keyword Parameter Table
        self.mdt = []  # Macro Definition Table
        self.sstab = {}  # Symbol Substitution Table
        self.ssntab = {}  # Symbol Substitution Name Table
        self.evntab = {}  # Expansion Variable Name Table

    def pass_1(self, source_code):
        """Pass 1: Parse the source code to build tables."""
        lines = source_code.split('\n')
        current_macro = None
        mdt_pointer = 0
        macro_definition_started = False

        for line in lines:
            line = line.strip()
            if line == 'MACRO':
                macro_definition_started = True
                continue

            if macro_definition_started:
                tokens = line.split()
                current_macro = tokens[0]
                self.mnt[current_macro] = {
                    'name': current_macro,
                    'pos_params': len(tokens) - 1,
                    'key_params': 0,
                    'exp_vars': 0,
                    'mdt_pointer': mdt_pointer,
                    'kptab_pointer': len(self.kptab),
                    'sstab_pointer': len(self.sstab)
                }
                self.pntab[current_macro] = list(dict.fromkeys(tokens[1:]))
                self.ssntab[current_macro] = []
                macro_definition_started = False
            elif line.startswith('MEND'):
                current_macro = None
            elif current_macro:
                self.mdt.append(line)
                mdt_pointer += 1

                tokens = line.split()
                for token in tokens:
                    if token.startswith('&'):
                        if token not in self.pntab[current_macro]:
                            self.pntab[current_macro].append(token)
                    elif '=' in token:
                        key, value = token.split('=')
                        if key not in self.kptab:
                            self.kptab[key] = value
                            self.mnt[current_macro]['key_params'] += 1
                    elif token.startswith('?'):
                        if token not in self.evntab:
                            self.evntab[token] = len(self.evntab)
                            self.mnt[current_macro]['exp_vars'] += 1

    def get_mdt_end_ptr(self, macro_name):
        """Determine the end of the MDT section for the given macro."""
        mnt_list = list(self.mnt.keys())
        mnt_index = mnt_list.index(macro_name)
        mdt_end_ptr = len(self.mdt)

        if mnt_index < len(mnt_list) - 1:
            next_macro = mnt_list[mnt_index + 1]
            mdt_end_ptr = self.mnt[next_macro]['mdt_pointer']

        return mdt_end_ptr

    def pass_2(self, input_file, output_file='expanded_code.txt'):
        """Pass-2 for macro expansion with parameter numbering."""
        with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
            for line in infile:
                tokens = line.strip().split()
                if len(tokens) == 0:
                    continue
                macro_name = tokens[0]
                
                if macro_name in self.mnt:
                    # Macro invocation detected, expand the macro
                    macro_details = self.mnt[macro_name]
                    mdt_pointer = macro_details['mdt_pointer']
                    mdt_end_ptr = self.get_mdt_end_ptr(macro_name)
                    
                    # Map the arguments provided in the macro call
                    args = tokens[1:]
                    param_mapping = {f'P{i+1}': arg for i, arg in enumerate(args)}

                    # Expand the macro
                    for i in range(mdt_pointer, mdt_end_ptr):
                        mdt_line = self.mdt[i]
                        # Replace parameters (P1, P2, ...) with the actual arguments
                        expanded_line = self.replace_placeholders(mdt_line, param_mapping, macro_name)
                        outfile.write(expanded_line + '\n')
                else:
                    # Regular instruction, write as is
                    outfile.write(line + '\n')

        print(f"Macro expanded code written to '{output_file}'.")

    def expand_macro_call(self, macro_name, arguments):
        """Expand the given macro based on MDT, PNTAB, and other tables."""
        expanded_code = []
        mnt_entry = self.mnt[macro_name]  # Ensure this references the macro name correctly
        mdt_pointer = int(mnt_entry['mdt_pointer'])
        mdt_end_ptr = self.get_mdt_end_ptr(macro_name)  # Get the end of this macro's MDT section
        pos_params = self.pntab[macro_name]  # Ensure pntab[macro_name] exists and is correct
        substitutions = dict(zip(pos_params, arguments))

        while mdt_pointer < mdt_end_ptr:
            line = self.mdt[mdt_pointer]
            line = self.replace_placeholders(line, substitutions, macro_name)  # Ensure macro_name is passed correctly

            # Check if the current line contains a macro call
            tokens = line.split()
            if tokens and tokens[0] in self.mnt:  # Check MNT before making the nested call
                nested_macro_name = tokens[0]
                nested_args = tokens[1:]  # Get arguments for the nested macro
                if len(nested_args) == 1:
                    nested_args = nested_args[0].split(',')  # Handle comma-separated arguments
                expanded_code.extend(" ")

                expanded_nested_lines = self.expand_macro_call(nested_macro_name, nested_args)
                expanded_code.extend(expanded_nested_lines)  # Expand the nested macro
                expanded_code.extend(" ")
            else:
                expanded_code.append(line.strip())

            mdt_pointer += 1

        return expanded_code

    def replace_placeholders(self, line, param_mapping, macro):
        """Replace parameters in a macro with corresponding numbers (P1, P2, ...) based on their order."""
        pos_params = self.pntab[macro]
        for i, param in enumerate(pos_params):
            # Replace the parameter with its assigned number (P1, P2, etc.)
            line = line.replace(param, f'(P,{i+1})')
        return line
        

    def write_expanded_source(self, expanded_source, output_filename='expanded_source.txt'):
        """Write the expanded source to a file."""
        with open(output_filename, 'w') as file:
            for line in expanded_source:
                file.write(line + '\n')

    def write_tables(self, filename='tables.txt'):
        """Write all the tables to a file, including replacing parameters with parameter numbers in the MDT."""
        with open(filename, 'w') as file:
            # Write the MNT (Macro Name Table)
            file.write("Macro Name Table (MNT):\n")
            for macro, details in self.mnt.items():
                file.write(f"{macro}: {details}\n")

            # Write the PNTAB (Positional Name Table)
            file.write("\nPositional Name Table (PNTAB):\n")
            for macro, params in self.pntab.items():
                file.write(f"{macro}: {params}\n")

            # Write the MDT (Macro Definition Table) with parameter numbers (P1, P2, ...)
            file.write("\nMacro Definition Table (MDT):\n")
            for macro in self.mnt:
                #file.write(f"MDT for {macro}:\n")
                mdt_pointer = self.mnt[macro]['mdt_pointer']
                mdt_end_ptr = self.get_mdt_end_ptr(macro)
                pos_params = self.pntab[macro]
                for i in range(mdt_pointer, mdt_end_ptr):
                    line = self.mdt[i]
                    # Replace parameters with P1, P2, etc.
                    line_with_params = self.replace_placeholders(line, pos_params, macro)
                    file.write(f"{i}: {line_with_params}\n")

        print("Tables written to 'tables.txt' with parameter numbers in the MDT.")

# Example usage
source_code = """
MACRO 
SORT_STUDENTS &start &end
    MOV R1, &start          
    MOV R2, &end            
LOOP:
    CMP R1, R2              
    JGE END_LOOP            
    SWAP_STUDENTS R1   
    ADD R1, 1               
    JMP LOOP               
END_LOOP:
MEND

MACRO 
SWAP_STUDENTS R1
    MOV R3, [R1]            
    MOV R4, [R1+1]         
    CMP R3, R4              
    JGE CONTINUE           
    SWAP R3, R4             
    MOV [R1], R3            
    MOV [R1+1], R4          
CONTINUE:
MEND
"""

macro_processor = MacroProcessor()
macro_processor.pass_1(source_code)
macro_processor.write_tables()
macro_processor.pass_2('source.txt')  # Read and expand the macros in the 'source.txt'
