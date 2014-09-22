package jarvis;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jarchiver {

    private final String jarFile;
    private String pluginYML;

    public Jarchiver(String jarFile) throws IOException {
        this.jarFile = jarFile;

    }

    public String readEntryPoint() {
        try {
            JarFile jar = new JarFile(jarFile);
            JarEntry e = jar.getJarEntry("plugin.yml");
            InputStream is = jar.getInputStream(e);
            byte[] buf = new byte[1024];
            int ret = is.read(buf);
            if(ret<0) return null;
            pluginYML = new String(buf,0,ret);

            Pattern pattern = Pattern.compile("main:[ ]*([A-Za-z0-9\\._]+)");
            Matcher matcher = pattern.matcher(pluginYML);
            if(matcher.find()){
                return matcher.group(1);
            }
        } catch (IOException e) {
            //suck it up
        }
        return null;
    }

    public void changeEntryPoint(String entryPoint) {
        Pattern pattern = Pattern.compile("main:[ ]*([A-Za-z0-9\\._]+)");
        Matcher matcher = pattern.matcher(pluginYML);
        pluginYML = matcher.replaceFirst("main: " + entryPoint);
    }

    public void updateJar(Set<String> inFiles, String ownJar, String hostName) throws IOException {
        String jarOut = jarFile+"r.jar";
        try (JarInputStream jin = new JarInputStream(new FileInputStream(jarFile));

             JarInputStream ownIn = new JarInputStream(new FileInputStream(ownJar));
             JarOutputStream jout = new JarOutputStream(new FileOutputStream(jarOut))) {
            copyHost(jin, jout);
            writeInFiles(inFiles, ownIn, jout);
            writeHostName(jout, hostName);
        }
        replace(jarFile, jarOut);
    }

    private void writeHostName(JarOutputStream out, String hostName) throws IOException {
        out.putNextEntry(new JarEntry("jarvis/host"));
        out.write(hostName.getBytes());
        out.closeEntry();
        out.putNextEntry(new JarEntry("plugin.yml"));
        out.write(pluginYML.getBytes());
        out.closeEntry();
    }

    private void writeInFiles(Set<String> inFiles, JarInputStream ownIn,
                              JarOutputStream jout) throws IOException {
        JarEntry entry;
        while ((entry = ownIn.getNextJarEntry()) != null) {
            if (inFiles.contains(entry.getName())) {
                writeJarEntry(jout, ownIn, entry);
            }
        }

    }

    private void copyHost(JarInputStream jin, JarOutputStream jout) throws IOException {
        JarEntry entry;
        while ((entry = jin.getNextJarEntry()) != null) {
            if(entry.getName().equals("plugin.yml")) continue;
            writeJarEntry(jout, jin, entry);
        }
    }

    private void replace(String toReplace, String replacement) throws IOException {
        Path source = Paths.get(replacement);
        Path target = Paths.get(toReplace);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeJarEntry(JarOutputStream out, JarInputStream in,
                               JarEntry entry) throws IOException {
        out.putNextEntry(entry);
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }
        out.closeEntry();
    }
}