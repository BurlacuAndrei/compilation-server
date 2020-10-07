package tcpdataserver;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class CompileProject {
    private String ProjectFolderName;
    private String CurrentFolder;
    private String projectPath;
    private List<String> codeFilePath;
    private String compileOuput;
    private String compilerPath;
    private HashSet<String> headersPath = new HashSet<>();
    private boolean isCProject = false;
    private boolean isCppProject = false;
    private boolean isCsharpProject = false;
    private boolean isFsharpProject = false;
    private boolean isJavaProject = false;

    private String mainClass = "";

    public CompileProject(String f, String compilerPath) {
        ProjectFolderName = f;
        CurrentFolder = ProjectFolderName;
        codeFilePath = new ArrayList<String>();
        projectPath = new File(ProjectFolderName).getAbsolutePath();
        this.compilerPath = compilerPath;
    }

    private boolean ifFirstSourceFile() {
        return !(isCProject | isCppProject | isCsharpProject | isFsharpProject | isJavaProject);
    }

    public byte[] getOutput() throws IOException {
        return compileOuput.getBytes("UTF-8");
    }

    public short getProjectType() {
        if (isCProject) return 1;
        if (isCppProject) return 2;
        if (isCsharpProject) return 3;
        if (isFsharpProject) return 4;
        if (isJavaProject) return 5;
        return 0;
    }

    public void startCompiling() throws IOException {
        System.out.println("Prepare to compile");
        IterateInFolder();
        System.out.println("Start compiling");
        Compile();
    }

    // populate codeFilePath list with *.c file paths
    private void IterateInFolder() throws IOException {
        File dir  = new File(CurrentFolder);
        File[] fileList = dir.listFiles();

        // TO DO
        if (fileList == null) return;

        for (int i = 0; i < fileList.length; ++i) {
            File f = fileList[i];
            if ( f.isDirectory()) {
                CurrentFolder = f.getAbsolutePath();
                IterateInFolder();
            }
            else {
                if (f.getName().endsWith(".c")) {
                    if (ifFirstSourceFile())
                        isCProject = true;
                    else if (!isCProject) throw new IOException("Nu sunt permise fisiere *.c in acest proiect");
                    codeFilePath.add(f.getAbsolutePath());
                    System.out.println("Found *.c file: " + f.getAbsolutePath());
                }
                else if (f.getName().endsWith(".h") || f.getName().endsWith(".hpp")) {
                    String headerPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf('\\'));
                    if (!headersPath.contains(headerPath)) {
                        headersPath.add(headerPath);
                    }
                    System.out.println("Found header file: " + f.getAbsolutePath());
                }
                else if (f.getName().endsWith(".cpp")) {
                    if (ifFirstSourceFile())
                        isCppProject = true;
                    else if (!isCppProject) throw new IOException("Nu sunt permise fisiere *.cpp in acest proiect");
                    codeFilePath.add(f.getAbsolutePath());
                    System.out.println("Found *.cpp file: " + f.getAbsolutePath());
                }
                else if (f.getName().endsWith(".cs")) {
                    if (ifFirstSourceFile())
                        isCsharpProject = true;
                    else if (!isCsharpProject) throw new IOException("Nu sunt permise fisiere *.cs in acest proiect");
                    codeFilePath.add(f.getAbsolutePath());
                    System.out.println("Found *.cs file: " + f.getAbsolutePath());
                }
                else if (f.getName().endsWith(".fs")) {
                    if (ifFirstSourceFile())
                        isFsharpProject = true;
                    else if (!isFsharpProject) throw new IOException("Nu sunt permise fisiere *.fs in acest proiect");
                    codeFilePath.add(f.getAbsolutePath());
                    System.out.println("Found *.fs file: " + f.getAbsolutePath());
                }
                else if (f.getName().endsWith(".java")) {
                    if (ifFirstSourceFile())
                        isJavaProject = true;
                    else if (!isJavaProject) throw new IOException("Nu sunt permise fisiere *.java in acest proiect");
                    if (FolderUtilities.isMainClass(f.getAbsolutePath()))
                        mainClass = FolderUtilities.getPackage(f.getAbsolutePath()) + "." + f.getName().replace(".java", "");
                    codeFilePath.add(f.getAbsolutePath());
                    System.out.println("Found *.java file: " + f.getAbsolutePath());
                }
            }
        }
    }

    // compile via VS2015 compiler
    private void Compile() {
        String files = codeFilePath.stream().collect(joining(" "));
        if (isCProject | isCppProject) {
            String includeHeaders = "";
            if (!headersPath.isEmpty()) {
                includeHeaders = headersPath.stream().collect(joining(" /I "));
                includeHeaders = "/I " + includeHeaders;
            }
            String cppTag = "";
            // C++ parameter
            if (isCppProject) cppTag = " /EHsc ";

            System.out.println("\"" + compilerPath + "vc\\vcvarsall.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "mkdir " + projectPath + "\\obj " +
                    "&& cd " + projectPath + "\\obj && " + "cl" + cppTag + includeHeaders + " /W4 " + files
                    + " /link /out:" + projectPath + "\\program" + ".exe");

            FolderUtilities.DeleteDirectory(projectPath + "\\obj");

            compileOuput = ServerUtilities.getCommandOutput("\"" + compilerPath + "vc\\vcvarsall.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "mkdir " + projectPath + "\\obj " +
                    "&& cd " + projectPath + "\\obj && " + "cl" + cppTag + includeHeaders + " /W4 " + files
                    + " /link /out:" + projectPath + "\\program" + ".exe");
        }
        else if (isCsharpProject) {
            System.out.println("\"" + compilerPath + "Common7\\Tools\\vsdevcmd.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "csc -out:" + projectPath + "\\program.exe " + files);

            compileOuput = ServerUtilities.getCommandOutput("\"" + compilerPath + "Common7\\Tools\\vsdevcmd.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "csc -out:" + projectPath + "\\program.exe " + files);
        }
        else if (isFsharpProject) {
            System.out.println("\"" + compilerPath + "Common7\\Tools\\vsdevcmd.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "fsc --out:" + projectPath + "\\program.exe " + files);

            compileOuput = ServerUtilities.getCommandOutput("\"" + compilerPath + "Common7\\Tools\\vsdevcmd.bat \" && "
                    + projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "fsc --out:" + projectPath + "\\program.exe " + files);
        }
        else if (isJavaProject) {
            System.out.println(projectPath.substring(0, projectPath.indexOf('\\')) + " && " +
                    "mkdir " + projectPath + "\\obj " +
                    "&& cd " + projectPath + "\\obj && " + "javac " + files);

            compileOuput = ServerUtilities.getCommandOutput(projectPath.substring(0, projectPath.indexOf('\\'))
                    + " && " + "mkdir " + projectPath + "\\obj " +
                    "&& cd " + projectPath + "\\obj && " + "javac -d ..\\obj " + files);

            compileOuput += "\r\n";

            System.out.println(projectPath.substring(0, projectPath.indexOf('\\')) +
                    "&& cd " + projectPath + " && " + "jar cfe program.jar " + mainClass + " -C obj .");
            compileOuput += ServerUtilities.getCommandOutput(projectPath.substring(0, projectPath.indexOf('\\')) +
                    "&& cd " + projectPath + " && " + "jar cfe program.jar " + mainClass + " -C obj .");
            if (compileOuput.trim().isEmpty())
                compileOuput = "Your java project is compiled successful";
        }
        else {
            compileOuput = "Error: There is nothing to compile";
        }
    }
}
