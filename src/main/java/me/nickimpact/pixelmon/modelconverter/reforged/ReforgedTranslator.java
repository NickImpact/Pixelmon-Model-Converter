package me.nickimpact.pixelmon.modelconverter.reforged;

import com.google.common.collect.Lists;
import me.nickimpact.pixelmon.modelconverter.Translator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static me.nickimpact.pixelmon.modelconverter.ModelConverter.*;

public class ReforgedTranslator implements Translator {

    @Override
    public List<String> getValidFileSuffixes() {
        return Lists.newArrayList(".bmd", ".smd", ".pqc");
    }

    @Override
    public void process(File directory, File output) {
        debug("Processing Directory: " + directory.getAbsolutePath());

        output.toPath().resolve(directory.getName()).toFile().mkdirs();

        for(File file : Objects.requireNonNull(directory.listFiles())) {
            if(file.getName().endsWith(".bmd")) {
                debug("Parsing BMD: " + file.getName());
                processed.getAndIncrement();
                try {
                    decode(file, new File(output, directory.getName()));
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to read BMD for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()));
                }
            } else if(file.getName().endsWith(".smd")) {
                debug("Cloning SMD: " + file.getName());
                processed.getAndIncrement();
                try {
                    Files.copy(file.toPath(), output.toPath().resolve(directory.getName()).resolve(file.getName()));
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to clone SMD for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()));
                }
            } else if(file.getName().endsWith(".pqc")) {
                debug("Cloning PQC: " + file.getName());
                processed.getAndIncrement();
                try {
                    Files.copy(file.toPath(), output.toPath().resolve(directory.getName()).resolve(file.getName()));
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to clone PQC for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()));
                    e.printStackTrace();
                }
            } else if(file.isDirectory()) {
                process(file, output.toPath().resolve(directory.getName()).toFile());
            }
        }
    }

    private static void decode(File input, File output) throws Exception {
        File outFile = new File(output, input.getName().replaceFirst("[.][^.]+$", "") + ".smd");
        outFile.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(outFile);

        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(input));

        DataInputStream in = new DataInputStream(bin);
        byte version = in.readByte();

        writeLine(writer, "version " + version);

        int numNodes = in.readShort();
        writeLine(writer, "nodes");

        for(int i = 0; i < numNodes; i++) {
            short id = in.readShort();
            short amount = in.readShort();
            String name = readNullTerm(in);
            writeLine(writer, String.format("%d \"%s\" %d", id, name, amount));
        }
        writeLine(writer, "end");

        writeLine(writer, "skeleton");
        int numSkeletons = in.readShort();

        for(int i = 0; i < numSkeletons; i++) {
            writeLine(writer, String.format("time %d", i));

            short amount = in.readShort();
            for(int k = 0; k < amount; k++) {
                short triangles = in.readShort();
                float lx = in.readFloat();
                float ly = in.readFloat();
                float lz = in.readFloat();

                float rotx = in.readFloat();
                float roty = in.readFloat();
                float rotz = in.readFloat();

                if(allZero(lx, ly, lz, rotx, roty, rotz)) {
                    writeLine(writer, String.format("%d  0 0 0  0 0 0", triangles));
                } else {
                    writeLine(writer, String.format(Locale.US, "%d  %f %f %f  %f %f %f", triangles, lx, ly, lz, rotx, roty, rotz));
                }
            }
        }
        writeLine(writer, "end");

        List<String> names = Lists.newArrayList();
        int max = in.readShort();
        for(int i = 0; i < max; i++) {
            names.add(readNullTerm(in));
        }

        int numTriangles = in.readShort();
        if(numTriangles > 0) {
            writeLine(writer, "triangles");

            for (int i = 0; i < numTriangles; i++) {
                String name = names.get(in.readByte());
                writeLine(writer, name);
                for (int k = 0; k < 3; k++) {
                    in.readShort();
                    float x = in.readFloat();
                    float y = in.readFloat();
                    float z = in.readFloat();
                    float normX = in.readFloat();
                    float normY = in.readFloat();
                    float normZ = in.readFloat();
                    float u = in.readFloat();
                    float v = in.readFloat();

                    byte links = in.readByte();
                    String out = String.format(Locale.US, "0  %f %f %f  %f %f %f  %f %f %d",
                            x, y, z, normX, normY, normZ, u, v, links
                    );
                    for (int j = 0; j < links; j++) {
                        int bone = in.readShort();
                        float weight = in.readFloat();
                        out = String.format(Locale.US, "%s %d %f", out, bone, weight);
                    }

                    writeLine(writer, out);
                }
            }
            writeLine(writer, "end");
        }

        writer.flush();
        in.close();
        bin.close();
        writer.close();
    }

    private static void writeLine(FileWriter writer, String out) throws Exception {
        writer.write(out);
        writer.write("\n");
        writer.flush();
    }

    private static String readNullTerm(DataInputStream in) throws IOException {
        StringBuilder str = new StringBuilder();
        char ch = 0;

        do {
            if (ch != 0) {
                str.append(ch);
            }

            ch = in.readChar();
        } while(ch != 0);

        return str.toString();
    }

    private static boolean allZero(float... in) {
        for(float x : in) {
            if(x != 0) {
                return false;
            }
        }

        return true;
    }
}
