import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class MNT {
    String name;
    int posParams;
    int keyParams;
    int expansionVars;
    int mdtPtr;
    int kpdtPtr;
    int sstPtr;

    public MNT(String name, int posParams, int keyParams, int expansionVars, int mdtPtr, int kpdtPtr, int sstPtr) {
        this.name = name;
        this.posParams = posParams;
        this.keyParams = keyParams;
        this.expansionVars = expansionVars;
        this.mdtPtr = mdtPtr;
        this.kpdtPtr = kpdtPtr;
        this.sstPtr = sstPtr;
    }

    @Override
    public String toString() {
        return " Name=" + name + ", PosParams=" + posParams + ", KeyParams=" + keyParams + 
               ", ExpVars=" + expansionVars + ", MDT Ptr=" + mdtPtr + ", KPDTAB Ptr=" + kpdtPtr + 
               ", SST Ptr=" + sstPtr;
    }
}

class PNTAB {
    List<String> paramNames = new ArrayList<>();

    public void addParam(String param) {
        paramNames.add(param);
    }

    public int getParamIndex(String param) {
        return paramNames.indexOf(param);
    }

    @Override
    public String toString() {
        return " " + String.join(" ", paramNames);
    }
}

class KPDTAB {
    String paramName;
    String defaultValue;

    public KPDTAB(String paramName, String defaultValue) {
        this.paramName = paramName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return " " + paramName + " = " + defaultValue;
    }
}

class EVNTAB {
    List<String> expansionVars = new ArrayList<>();

    public void addVar(String varName) {
        expansionVars.add(varName);
    }

    @Override
    public String toString() {
        return " " + String.join(" ", expansionVars);
    }
}

class SSTAB {
    String symbolName;
    int locationInMDT;

    public SSTAB(String symbolName, int locationInMDT) {
        this.symbolName = symbolName;
        this.locationInMDT = locationInMDT;
    }

    @Override
    public String toString() {
        return symbolName + ": " + locationInMDT;
    }
}

class SSNTAB {
    List<String> symbolNames = new ArrayList<>();

    public void addSymbol(String symbolName) {
        symbolNames.add(symbolName);
    }

    public int getSymbolIndex(String symbolName) {
        return symbolNames.indexOf(symbolName);
    }

    @Override
    public String toString() {
        return " " + String.join(" ", symbolNames);
    }
}

class MDT {
    List<String> instructions = new ArrayList<>();

    public void addInstruction(String instruction) {
        instructions.add(instruction);
    }

    @Override
    public String toString() {
        return " " + String.join("\n", instructions);
    }
}

class MacroProcessor {
    private List<MNT> mntList = new ArrayList<>();
    private List<PNTAB> pntabList = new ArrayList<>();
    private List<KPDTAB> kpdtabList = new ArrayList<>();
    private List<EVNTAB> evntabList = new ArrayList<>();
    private List<SSTAB> sstabList = new ArrayList<>();
    private List<SSNTAB> ssntabList = new ArrayList<>();
    private MDT mdt = new MDT();

    public void processMacroFile(String fileName) throws IOException {
        BufferedReader file = new BufferedReader(new FileReader(fileName));
        String line;
        boolean isMacro = false;
        String macroName = "";
        PNTAB currentPNTAB = new PNTAB();
        EVNTAB currentEVNTAB = new EVNTAB();
        SSNTAB currentSSNTAB = new SSNTAB();
        int posParams = 0, keyParams = 0, mdtPtr = mdt.instructions.size();
        int kpdtabIndex = kpdtabList.size();
        AtomicInteger sstabIndex = new AtomicInteger(sstabList.size());
        boolean hasKeywords = false;
        int counter = 0;

        while ((line = file.readLine()) != null) {
            line = trim(line);
            if (line.startsWith("MACRO")) {
                isMacro = true;
                currentEVNTAB = new EVNTAB();
                currentSSNTAB = new SSNTAB();
                continue;
            } else if (line.startsWith("MEND")) {
                isMacro = false;
                counter--;
                MNT mnt = new MNT(macroName, posParams, keyParams, currentEVNTAB.expansionVars.size(), mdtPtr, 
                                  hasKeywords ? kpdtabIndex : -1, sstabIndex.get());
                mntList.add(mnt);
                mdtPtr = mdt.instructions.size();
                pntabList.add(currentPNTAB);
                evntabList.add(currentEVNTAB);
                ssntabList.add(currentSSNTAB);

                currentPNTAB = new PNTAB();
                macroName = "";
                kpdtabIndex = kpdtabList.size();
                sstabIndex.set(sstabList.size());
                continue;
            }

            if (isMacro) {
                if (macroName.isEmpty()) {
                    counter--;
                    String[] parts = line.split(" ");
                    macroName = parts[0];
                    for (int i = 1; i < parts.length; i++) {
                        String par = parts[i];
                        if (par.charAt(0) == '.') {
                            sstabList.add(new SSTAB(par, counter++));
                            currentPNTAB.addParam(par);
                            posParams++;
                        }
                        String[] parax = par.split(",");
                        for (String param : parax) {
                            if (param.contains("=")) {
                                String[] keyValue = param.split("=");
                                kpdtabList.add(new KPDTAB(keyValue[0], keyValue[1]));
                                currentPNTAB.addParam(keyValue[0]);
                                posParams++;
                                keyParams++;
                                hasKeywords = true;
                            } else {
                                currentPNTAB.addParam(param);
                                posParams++;
                            }
                        }
                    }
                } else if (line.startsWith("LCL")) {
                    String[] vars = line.substring(3).split(",");
                    for (String var : vars) {
                        currentEVNTAB.addVar(trim(var));
                        int evIndex = currentEVNTAB.expansionVars.indexOf(var);
                        mdt.addInstruction(replaceAll(line, var, "(E," + evIndex + ") "));
                    }
                } else {
                    String[] tokens = line.split(" ");
                    StringBuilder replacedInstruction = new StringBuilder();
                    String symbolName = "";

                    for (String token : tokens) {
                        if (token.charAt(0) == '.') {
                            symbolName = token.substring(1);
                            currentSSNTAB.addSymbol(symbolName);
                            sstabList.add(new SSTAB(symbolName, counter));
                        } else if (token.contains(",")) {
                            String[] subTokens = token.split(",");
                            for (String subToken : subTokens) {
                                int index = currentPNTAB.getParamIndex(subToken);
                                if (index != -1) {
                                    replacedInstruction.append(" (P,").append(index + 1).append(")");
                                } else if (currentEVNTAB.expansionVars.contains(subToken)) {
                                    int evIndex = currentEVNTAB.expansionVars.indexOf(subToken) + 1;
                                    replacedInstruction.append(" (E,").append(evIndex).append(")");
                                } else {
                                    replacedInstruction.append(subToken);
                                }
                            }
                            replacedInstruction.append(" ");
                        } else {
                            int index = currentPNTAB.getParamIndex(token);
                            if (index != -1) {
                                replacedInstruction.append(" (P,").append(index + 1).append(")");
                            } else if (currentEVNTAB.expansionVars.contains(token)) {
                                int evIndex = currentEVNTAB.expansionVars.indexOf(token) + 1;
                                replacedInstruction.append(" (E,").append(evIndex).append(")");
                            } else {
                                replacedInstruction.append(token);
                            }
                        }
                    }
                    mdt.addInstruction(replaceAll(trim(replacedInstruction.toString()), "." + symbolName, ""));
                }
            }
            counter++;
        }
        file.close();
    }

    public void printTables(String fileName) throws IOException {
        BufferedWriter file = new BufferedWriter(new FileWriter(fileName));

        file.write("MNT:\n");
        for (MNT mnt : mntList) {
            file.write(mnt.toString() + "\n");
        }

        file.write("\nMDT:\n");
        for (String line : mdt.instructions) {
            file.write(line + "\n");
        }

        file.write("\nPNTAB:\n");
        for (PNTAB pntab : pntabList) {
            file.write(" [" + String.join(", ", pntab.paramNames).replace("&", "") + "]\n");
        }

        file.write("\nKPDTAB:\n");
        for (KPDTAB kpdtab : kpdtabList) {
            file.write(kpdtab.toString().replace("&", "") + "\n");
        }

        file.write("\nEVNTAB:\n");
        for (EVNTAB evntab : evntabList) {
            file.write(evntab.toString().replace("&", "") + "\n");
        }

        file.write("\nSSNTAB:\n");
        for (SSNTAB ssntab : ssntabList) {
            file.write(ssntab.toString().replace("&", "") + "\n");
        }

        file.write("\nSSTAB:\n");
        for (SSTAB sstab : sstabList) {
            file.write(sstab.toString() + "\n");
        }
        file.close();
    }

    public String trim(String str) {
        return str.strip().replaceAll("\\s+", " ");
    }

    public String replaceAll(String source, String toReplace, String replaceWith) {
        return source.replaceAll(toReplace, replaceWith);
    }
}

public class sp_ass4 {
    public static void main(String[] args) throws IOException {
        MacroProcessor processor = new MacroProcessor();
        processor.processMacroFile("assembly_code_ass4.txt");
        processor.printTables("output_ass4.txt");
    }
}
