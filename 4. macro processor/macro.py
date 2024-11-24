import re

class MNT:
    def __init__(self, name, pos_params, key_params, expansion_vars, mdt_ptr, kpdt_ptr, sst_ptr):
        self.name = name
        self.pos_params = pos_params
        self.key_params = key_params
        self.expansion_vars = expansion_vars
        self.mdt_ptr = mdt_ptr
        self.kpdt_ptr = kpdt_ptr
        self.sst_ptr = sst_ptr

    def __str__(self):
        return f" Name={self.name}, PosParams={self.pos_params}, KeyParams={self.key_params}, " \
               f"ExpVars={self.expansion_vars}, MDT Ptr={self.mdt_ptr}, KPDTAB Ptr={self.kpdt_ptr}, SST Ptr={self.sst_ptr}"

class PNTAB:
    def __init__(self):
        self.param_names = []

    def add_param(self, param):
        self.param_names.append(param)

    def get_param_index(self, param):
        return self.param_names.index(param)

    def __str__(self):
        return " " + " ".join(self.param_names)

class KPDTAB:
    def __init__(self, param_name, default_value):
        self.param_name = param_name
        self.default_value = default_value

    def __str__(self):
        return f" {self.param_name} = {self.default_value}"

class EVNTAB:
    def __init__(self):
        self.expansion_vars = []

    def add_var(self, var_name):
        self.expansion_vars.append(var_name)

    def __str__(self):
        return " " + " ".join(self.expansion_vars)

class SSTAB:
    def __init__(self, symbol_name, location_in_mdt):
        self.symbol_name = symbol_name
        self.location_in_mdt = location_in_mdt

    def __str__(self):
        return f"{self.symbol_name}: {self.location_in_mdt}"

class SSNTAB:
    def __init__(self):
        self.symbol_names = []

    def add_symbol(self, symbol_name):
        self.symbol_names.append(symbol_name)

    def get_symbol_index(self, symbol_name):
        return self.symbol_names.index(symbol_name)

    def __str__(self):
        return " " + " ".join(self.symbol_names)

class MDT:
    def __init__(self):
        self.instructions = []

    def add_instruction(self, instruction):
        self.instructions.append(instruction)

    def __str__(self):
        return " " + "\n".join(self.instructions)

class MacroProcessor:
    def __init__(self):
        self.mnt_list = []
        self.pntab_list = []
        self.kpdtab_list = []
        self.evntab_list = []
        self.sstab_list = []
        self.ssntab_list = []
        self.mdt = MDT()

    def process_macro_file(self, file_name):
        with open(file_name, 'r') as file:
            lines = file.readlines()

        is_macro = False
        macro_name = ""
        current_pntab = PNTAB()
        current_evntab = EVNTAB()
        current_ssntab = SSNTAB()
        pos_params = 0
        key_params = 0
        mdt_ptr = len(self.mdt.instructions)
        kpdtab_index = len(self.kpdtab_list)
        sstab_index = len(self.sstab_list)
        has_keywords = False
        counter = 0

        for line in lines:
            line = self.trim(line)
            if line.startswith("MACRO"):
                is_macro = True
                current_evntab = EVNTAB()
                current_ssntab = SSNTAB()
                continue
            elif line.startswith("MEND"):
                is_macro = False
                counter -= 1
                mnt = MNT(macro_name, pos_params, key_params, len(current_evntab.expansion_vars), mdt_ptr,
                          kpdtab_index if has_keywords else -1, sstab_index)
                self.mnt_list.append(mnt)
                mdt_ptr = len(self.mdt.instructions)
                self.pntab_list.append(current_pntab)
                self.evntab_list.append(current_evntab)
                self.ssntab_list.append(current_ssntab)

                current_pntab = PNTAB()
                macro_name = ""
                kpdtab_index = len(self.kpdtab_list)
                sstab_index = len(self.sstab_list)
                continue

            if is_macro:
                if not macro_name:
                    counter -= 1
                    parts = line.split(" ")
                    macro_name = parts[0]
                    for part in parts[1:]:
                        if part.startswith('.'):
                            self.sstab_list.append(SSTAB(part, counter))
                            current_pntab.add_param(part)
                            pos_params += 1
                        for param in part.split(","):
                            if "=" in param:
                                key_value = param.split("=")
                                self.kpdtab_list.append(KPDTAB(key_value[0], key_value[1]))
                                current_pntab.add_param(key_value[0])
                                pos_params += 1
                                key_params += 1
                                has_keywords = True
                            else:
                                current_pntab.add_param(param)
                                pos_params += 1
                elif line.startswith("LCL"):
                    # In process_macro_file, inside the LCL block
                    vars = line[3:].split(",")
                    for var in vars:
                        var = self.trim(var)  # Make sure to trim the variable before adding or looking for it
                        current_evntab.add_var(var)
                        ev_index = current_evntab.expansion_vars.index(var)  # Look for the trimmed variable
                        self.mdt.add_instruction(self.replace_all(line, var, f"(E,{ev_index}) "))

                else:
                    tokens = line.split(" ")
                    replaced_instruction = ""
                    symbol_name = ""

                    for token in tokens:
                        if token.startswith('.'):
                            symbol_name = token[1:]
                            current_ssntab.add_symbol(symbol_name)
                            self.sstab_list.append(SSTAB(symbol_name, counter))
                        else:
                            if "," in token:
                                sub_tokens = token.split(",")
                                for sub_token in sub_tokens:
                                    index = current_pntab.get_param_index(sub_token) if sub_token in current_pntab.param_names else -1
                                    if index != -1:
                                        replaced_instruction += f" (P,{index + 1})"
                                    elif sub_token in current_evntab.expansion_vars:
                                        ev_index = current_evntab.expansion_vars.index(sub_token) + 1
                                        replaced_instruction += f" (E,{ev_index})"
                                    else:
                                        replaced_instruction += sub_token
                                replaced_instruction += " "
                            else:
                                index = current_pntab.get_param_index(token) if token in current_pntab.param_names else -1
                                if index != -1:
                                    replaced_instruction += f" (P,{index + 1})"
                                elif token in current_evntab.expansion_vars:
                                    ev_index = current_evntab.expansion_vars.index(token) + 1
                                    replaced_instruction += f" (E,{ev_index})"
                                else:
                                    replaced_instruction += token
                    self.mdt.add_instruction(self.replace_all(self.trim(replaced_instruction), f".{symbol_name}", ""))

            counter += 1

    def print_tables(self, file_name):
        with open(file_name, 'w') as file:
            file.write("MNT:\n")
            for mnt in self.mnt_list:
                file.write(str(mnt) + "\n")

            file.write("\nMDT:\n")
            for line in self.mdt.instructions:
                file.write(line + "\n")

            file.write("\nPNTAB:\n")
            for pntab in self.pntab_list:
                file.write(f" [{', '.join(pntab.param_names).replace('&', '')}]\n")

            file.write("\nKPDTAB:\n")
            for kpdtab in self.kpdtab_list:
                file.write(str(kpdtab).replace('&', '') + "\n")

            file.write("\nEVNTAB:\n")
            for evntab in self.evntab_list:
                file.write(str(evntab).replace('&', '') + "\n")

            file.write("\nSSNTAB:\n")
            for ssntab in self.ssntab_list:
                file.write(str(ssntab).replace('&', '') + "\n")

            file.write("\nSSTAB:\n")
            for sstab in self.sstab_list:
                file.write(str(sstab) + "\n")

    def trim(self, string):
        return string.strip().replace("\n", " ").replace("\r", "")

    def replace_all(self, source, to_replace, replace_with):
        return source.replace(to_replace, replace_with)

if __name__ == '__main__':
    macro_processor = MacroProcessor()
    macro_processor.process_macro_file('C:\\1VU\\TY\\System Programming\\SPL Practice\\4. macro processor\\assembly_code_ass4.txt')
    macro_processor.print_tables('C:\\1VU\\TY\\System Programming\\SPL Practice\\4. macro processor\\output_ass4.txt')
