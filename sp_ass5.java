import java.io.*;
import java.util.*;

class MNT {
    String name;
    int posParams;
    int keyParams;
    int mdtPtr;
    int kpdtPtr;
    int sstPtr;

    MNT(String name, int posParams, int keyParams, int mdtPtr, int kpdtPtr, int sstPtr) {
        this.name = name;
        this.posParams = posParams;
        this.keyParams = keyParams;
        this.mdtPtr = mdtPtr;
        this.kpdtPtr = kpdtPtr;
        this.sstPtr = sstPtr;
    }
}

class KPDTAB {
    String paramName;
    String defaultValue;

    KPDTAB(String paramName, String defaultValue) {
        this.paramName = paramName;
        this.defaultValue = defaultValue;
    }
}

class SSTAB {
    String symbolName;
    int locationInMDT;

    SSTAB(String symbolName, int locationInMDT) {
        this.symbolName = symbolName;
        this.locationInMDT = locationInMDT;
    }
}

class APTAB {
    String macroName;
    List<String> arguments;
    int lineNumber;

    APTAB(String macroName, List<String> arguments, int lineNumber) {
        this.macroName = macroName;
        this.arguments = arguments;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "APTAB{" +
                "macroName='" + macroName + '\'' +
                ", arguments=" + arguments +
                ", lineNumber=" + lineNumber +
                '}';
    }
}

public class sp_ass5 {
    private List<MNT> mntList = new ArrayList<>();
    private List<List<String>> pntabList = new ArrayList<>();
    private List<KPDTAB> kpdtabList = new ArrayList<>();
    private List<String> mdt = new ArrayList<>();
    private List<SSTAB> sstabList = new ArrayList<>();
    private List<List<String>> ssntabList = new ArrayList<>();
    private List<List<String>> evntabList = new ArrayList<>();
    private List<APTAB> aptabList = new ArrayList<>(); // List for APTAB entries

    public void readTables(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            String currentSection = "";
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MNT:")) {
                    currentSection = "MNT";
                } else if (line.startsWith("PNTAB:")) {
                    currentSection = "PNTAB";
                } else if (line.startsWith("KPDTAB:")) {
                    currentSection = "KPDTAB";
                } else if (line.startsWith("MDT:")) {
                    currentSection = "MDT";
                } else if (line.startsWith("SSTAB:")) {
                    currentSection = "SSTAB";
                } else if (line.startsWith("SSNTAB:")) {
                    currentSection = "SSNTAB";
                } else if (line.startsWith("EVNTAB:")) {
                    currentSection = "EVNTAB";
                } else if (!line.trim().isEmpty()) {
                    switch (currentSection) {
                        case "MNT":
                            String[] mntParts = line.split(",");
                            String name = mntParts[0].split("=")[1].trim();
                            int posParams = Integer.parseInt(mntParts[1].split("=")[1].trim());
                            int keyParams = Integer.parseInt(mntParts[2].split("=")[1].trim());
                            int mdtPtr = Integer.parseInt(mntParts[4].split("=")[1].trim());
                            int kpdtPtr = Integer.parseInt(mntParts[5].split("=")[1].trim());
                            int sstPtr = Integer.parseInt(mntParts[6].split("=")[1].trim());
                            mntList.add(new MNT(name, posParams, keyParams, mdtPtr, kpdtPtr, sstPtr));
                            break;
                        case "PNTAB":
                            List<String> pntab = new ArrayList<>(Arrays.asList(line.substring(2, line.length() - 1).split(", ")));
                            pntabList.add(pntab);
                            break;
                        case "KPDTAB":
                            String[] kpdtabParts = line.trim().split("=");
                            kpdtabList.add(new KPDTAB(kpdtabParts[0].trim(),
                                    kpdtabParts.length > 1 ? kpdtabParts[1].trim() : "null"));
                            break;
                        case "MDT":
                            mdt.add(line.trim());
                            break;
                        case "SSTAB":
                            String[] sstabParts = line.split(":");
                            sstabList.add(new SSTAB(sstabParts[0].trim(), Integer.parseInt(sstabParts[1].trim())));
                            break;
                        case "SSNTAB":
                            ssntabList.add(new ArrayList<>(Arrays.asList(line.substring(1, line.length() - 1).split(", "))));
                            break;
                        case "EVNTAB":
                            evntabList.add(new ArrayList<>(Arrays.asList(line.substring(1, line.length() - 1).split(", "))));
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading tables file: " + e.getMessage());
        }
    }

    public void processSourceFile(String inputFile, String outputFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            String line;
            boolean inMacroExpansion = false;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                lineNumber++;
                if (line.equalsIgnoreCase("START")) {
                    writer.println(line);
                    inMacroExpansion = true;
                    lineNumber--;
                } else if (line.equalsIgnoreCase("END")) {
                    writer.println(line);
                    inMacroExpansion = false;
                    lineNumber--;
                } else if (inMacroExpansion) {
                    String[] parts = line.split(" ");
                    String macroName = parts[0];
                    parts = parts[1].split(",");
                    System.out.println(Arrays.toString(parts));
                    MNT mnt = findMNT(macroName);
                    if (mnt != null) {
                        List<String> arguments = new ArrayList<>(Arrays.asList(parts).subList(0, parts.length));
                        aptabList.add(new APTAB(macroName, arguments, lineNumber)); // Add to APTAB
                        expandMacro(mnt, arguments, writer);
                    } else {
                        writer.println(line);
                    }
                } else {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing source file: " + e.getMessage());
        }
    }

    private MNT findMNT(String macroName) {
        for (MNT mnt : mntList) {
            if (mnt.name.equals(macroName)) {
                return mnt;
            }
        }
        return null;
    }

    private void expandMacro(MNT mnt, List<String> arguments, PrintWriter writer) {
        Map<String, String> argMap = new HashMap<>();
        List<String> pntab = pntabList.get(mntList.indexOf(mnt));

        // Process positional parameters
        for (int i = 0; i < Math.min(mnt.posParams, pntab.size()); i++) {
            if (i < arguments.size()) {
                if (arguments.get(i).contains("=")) {
                    argMap.put(pntab.get(i), arguments.get(i).split("=")[1]);
                    continue;
                }
                argMap.put(pntab.get(i), arguments.get(i).trim().isEmpty() ? "null" : arguments.get(i).trim());
            }
        }

        // Handle default values for keyword parameters not specified
        for (int i = mnt.kpdtPtr; i < Math.min(mnt.kpdtPtr + mnt.keyParams, kpdtabList.size()); i++) {
            KPDTAB kpdtab = kpdtabList.get(i);
            if (argMap.containsKey(kpdtab.paramName) && argMap.get(kpdtab.paramName).equals("null")) {
                argMap.put(kpdtab.paramName, kpdtab.defaultValue);
            }
        }

        // Determine the end of this macro's MDT section
        int mdtEndPtr = mdt.size();
        int mntIndex = mntList.indexOf(mnt);
        if (mntIndex < mntList.size() - 1) {
            mdtEndPtr = mntList.get(mntIndex + 1).mdtPtr;
        }

        // Expand macro
        for (int i = mnt.mdtPtr; i < mdtEndPtr; i++) {
            String instruction = mdt.get(i);
            
            // Replace parameters (e.g., P1, P2) with actual values
            for (Map.Entry<String, String> entry : argMap.entrySet()) {
                instruction = instruction.replace("(" + entry.getKey() + ")", entry.getValue());
            }
            
            writer.println(instruction.trim());
        }
    }


    public static void main(String[] args) {
        sp_ass5 processor = new sp_ass5();
        processor.readTables("output.txt");
        processor.processSourceFile("source.txt", "expanded_source.txt");
        // Print the APTAB entries
        System.out.println("APTAB entries:");
        for (APTAB aptab : processor.aptabList) {
            System.out.println(aptab);
        }
    }
}
