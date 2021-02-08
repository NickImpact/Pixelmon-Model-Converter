package me.nickimpact.pixelmon.modelconverter.reforged;

import com.google.common.collect.Lists;
import me.nickimpact.pixelmon.modelconverter.ModelConverter;
import me.nickimpact.pixelmon.modelconverter.Translator;
import me.nickimpact.pixelmon.modelconverter.util.PrettyPrinter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.nickimpact.pixelmon.modelconverter.ModelConverter.*;

public abstract class ReforgedTranslator implements Translator {

    public static class ReforgedDeserializer extends ReforgedTranslator implements Deserializer {

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
                        throw new RuntimeException("Failed to read BMD for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()), e);
                    }
                } else if(file.getName().endsWith(".smd")) {
                    debug("Cloning SMD: " + file.getName());
                    processed.getAndIncrement();
                    try {
                        Files.copy(file.toPath(), output.toPath().resolve(directory.getName()).resolve(file.getName()));
                        successful.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to clone SMD for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()), e);
                    }
                } else if(file.getName().endsWith(".pqc")) {
                    debug("Cloning PQC: " + file.getName());
                    processed.getAndIncrement();
                    try {
                        Files.copy(file.toPath(), output.toPath().resolve(directory.getName()).resolve(file.getName()));
                        successful.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to clone PQC for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()), e);
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

    public static class ReforgedSerializer extends ReforgedTranslator implements Serializer {

        @Override
        public List<String> getValidFileSuffixes() {
            return Lists.newArrayList(".smd");
        }

        @Override
        public void process(File directory, File output) {
            debug("Processing Directory: " + directory.getAbsolutePath());
            output.toPath().resolve(directory.getName()).toFile().mkdirs();

            for(File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.getName().endsWith(".smd")) {
                    debug("Encoding SMD: " + file.getName());
                    processed.getAndIncrement();

                    try {
                        encode(file, new File(output, directory.getName()));
                        successful.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("Failed to read SMD for pokemon: " + String.format("%s (%s) -", directory.getName(), file.getName()) + e.getMessage());
                        e.printStackTrace();
                        System.exit(1);
                    }

                } else if(file.isDirectory()) {
                    process(file, output.toPath().resolve(directory.getName()).toFile());
                }
            }
        }

        private void encode(File input, File output) throws Exception {
            File outFile = new File(output, input.getName().replaceFirst("[.][^.]+$", "") + ".bmd");
            outFile.getParentFile().mkdirs();
            BufferedReader reader = new BufferedReader(new FileReader(input));

            BufferedOutputStream bin = new BufferedOutputStream(new FileOutputStream(outFile));
            DataOutputStream out = new DataOutputStream(bin);

            try {
                Marker marker = Marker.None;
                Queue<String> nodes = new LinkedList<>();
                Queue<String> skeleton = new LinkedList<>();
                Queue<String> triangles = new LinkedList<>();

                PrettyPrinter printer = new PrettyPrinter(80);
                printer.add("File Contents for " + input.getName()).center();
                printer.hr('-');
                printer.add("Marker: " + marker.name());

                String next;
                while ((next = reader.readLine()) != null) {
                    Optional<Marker> update = Marker.get(next);
                    if (update.isPresent()) {
                        marker = update.get();
                        printer.add("Marker: " + marker.name());
                    } else {
                        if (!next.equals("end") && marker != Marker.None) {
                            switch (marker) {
                                case Nodes:
                                    printer.add("  Node: " + next);
                                    nodes.add(next);
                                    break;
                                case Skeleton:
                                    printer.add("  Skeleton: " + next);
                                    skeleton.add(next);
                                    break;
                                case Triangles:
                                    printer.add("  Triangles: " + next);
                                    triangles.add(next);
                                    break;
                            }
                        }
                    }
                }

                out.writeByte(1);
                out.writeShort(nodes.size());
                Pattern nodeMatcher = Pattern.compile("([0-9\\-]+) \"((?:\\\\\"|.)*?)\" +([0-9\\-]+)");
                for (String node : nodes) {
                    Matcher matcher = nodeMatcher.matcher(node);
                    if(!matcher.matches()) {
                        throw new Exception("Failed to read SMD node");
                    }
                    out.writeShort(Integer.parseInt(matcher.group(1)));
                    out.writeShort(Integer.parseInt(matcher.group(3)));
                    out.writeChars(matcher.group(2) + '\u0000');
                }

                List<Skeleton> skeletons = Lists.newArrayList();
                boolean start = true;
                Skeleton.Builder working = Skeleton.builder();
                for (String in : skeleton) {
                    if (in.startsWith("time")) {
                        if (!start) {
                            skeletons.add(working.build());
                            working = Skeleton.builder();
                        }

                        start = false;
                        working = working.time(Integer.parseInt(in.split(" ")[1]));
                    } else {
                        working.append(in);
                    }
                }

                skeletons.add(working.build());

                printer.hr('-');
                out.writeShort(skeletons.size());
                for (Skeleton sk : skeletons) {
                    out.writeShort(sk.components.size());
                    for (String comp : sk.components) {
                        String[] tokens = comp.split("[ ]+");
                        out.writeShort(Integer.parseInt(tokens[0]));
                        out.writeFloat(Float.parseFloat(tokens[1]));
                        out.writeFloat(Float.parseFloat(tokens[2]));
                        out.writeFloat(Float.parseFloat(tokens[3]));
                        out.writeFloat(Float.parseFloat(tokens[4]));
                        out.writeFloat(Float.parseFloat(tokens[5]));
                        out.writeFloat(Float.parseFloat(tokens[6]));
                    }
                }

                if(debug) {
                    printer.log(System.out);
                }

                List<Triangle> tris = Lists.newArrayList();
                while (!triangles.isEmpty()) {
                    Triangle.Builder builder = Triangle.builder();
                    String mat = triangles.poll();
                    builder.name(mat);

                    for (int i = 0; i < 3; i++) {
                        builder.append(triangles.poll());
                    }

                    tris.add(builder.build());
                }

                List<String> materials = Lists.newArrayList();
                for (Triangle triangle : tris) {
                    if (!materials.contains(triangle.name)) {
                        materials.add(triangle.name);
                    }
                }
                out.writeShort(materials.size());

                for (String material : materials) {
                    out.writeChars(material + '\u0000');
                }
                out.writeShort(tris.size());

                for (Triangle triangle : tris) {
                    out.writeByte(materials.indexOf(triangle.name));

                    for (String line : triangle.components) {
                        String split[] = line.split("[ ]+");
                        out.writeShort(Integer.parseInt(split[0]));
                        out.writeFloat(Float.parseFloat(split[1]));
                        out.writeFloat(Float.parseFloat(split[2]));
                        out.writeFloat(Float.parseFloat(split[3]));
                        out.writeFloat(Float.parseFloat(split[4]));
                        out.writeFloat(Float.parseFloat(split[5]));
                        out.writeFloat(Float.parseFloat(split[6]));
                        out.writeFloat(Float.parseFloat(split[7]));
                        out.writeFloat(Float.parseFloat(split[8]));
                        out.writeByte(Integer.parseInt(split[9]));

                        int links = Byte.parseByte(split[9]);

                        for(int w = 0; w < links; w++) {
                            out.writeShort(Integer.parseInt(split[10 + w * 2]));
                            out.writeFloat(Float.parseFloat(split[11 + w * 2]));
                        }
                    }
                }

            } finally {
                out.close();
                reader.close();
            }
        }

        private enum Marker {
            None("XXXXXXXXXXXXXXXXXXXXXXXXXXXX"),
            Nodes("nodes"),
            Skeleton("skeleton"),
            Triangles("triangles"),
            VertexAnim("vertexanimation")
            ;

            private String key;

            Marker(String key) {
                this.key = key;
            }

            public static Optional<Marker> get(String input) {
                return Arrays.stream(values()).filter(x -> x != None)
                        .filter(x -> x.key.equalsIgnoreCase(input))
                        .findAny();
            }
        }

        private static class Skeleton {

            private final int time;
            private final List<String> components;

            private Skeleton(Builder builder) {
                this.time = builder.time;
                this.components = builder.pieces;
            }

            public static Builder builder() {
                return new Builder();
            }

            public static class Builder {

                private int time;
                private List<String> pieces = Lists.newArrayList();

                public Builder time(int time) {
                    this.time = time;
                    return this;
                }

                public Builder append(String component) {
                    this.pieces.add(component);
                    return this;
                }

                public Skeleton build() {
                    return new Skeleton(this);
                }
            }
        }

        private static class Triangle {
            private final String name;
            private final List<String> components;

            private Triangle(Builder builder) {
                this.name = builder.name;
                this.components = builder.pieces;
            }

            public static Builder builder() {
                return new Builder();
            }

            public static class Builder {

                private String name;
                private List<String> pieces = Lists.newArrayList();

                public Builder name(String name) {
                    this.name = name;
                    return this;
                }

                public Builder append(String component) {
                    this.pieces.add(component);
                    return this;
                }

                public Triangle build() {
                    return new Triangle(this);
                }
            }
        }
    }
}
