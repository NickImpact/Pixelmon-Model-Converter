package me.nickimpact.pixelmon.modelconverter.generations;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import me.nickimpact.pixelmon.modelconverter.ModelConverter;
import me.nickimpact.pixelmon.modelconverter.Translator;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.formats.SMDBinaryReader;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.formats.SMDBinaryWriter;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.formats.SMDTextReader;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.formats.SMDTextWriter;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.SMDFile;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import static me.nickimpact.pixelmon.modelconverter.ModelConverter.*;

public abstract class GenerationsTranslator implements Translator {

    public static class GensDeserializer extends GenerationsTranslator implements Deserializer {

        @Override
        public List<String> getValidFileSuffixes() {
            return Lists.newArrayList(".smdx", ".smd", ".pqc");
        }

        @Override
        public void process(File directory, File output) {
            debug("Processing Directory: " + directory.getAbsolutePath());
            output.toPath().resolve(directory.getName()).toFile().mkdirs();

            for(File file : Objects.requireNonNull(directory.listFiles())) {
                debug("  - Reading file: " + file.getAbsolutePath());
                if(file.getName().endsWith(".smdx")) {
                    debug("Decoding SMDX: " + file.getName());
                    processed.getAndIncrement();
                    try {
                        this.write(this.read(new FileInputStream(file)), file.getName(), new File(output, directory.getName()));
                        successful.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("Failed to read SMDX for pokemon: " + String.format("%s (%s)", directory.getName(), file.getName()));
                        e.printStackTrace();
                        System.exit(1);
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

        @Override
        public void process(JarFile file, JarEntry entry, File output) {
            debug("Processing entry: " + entry.getName());
            Matcher matcher = JAR_PATTERN.matcher(entry.getName());
            matcher.find();
            String[] remaining = matcher.group("rest").split("[/]");

            Path target = output.toPath().resolve(matcher.group("species"));
            String work;
            if(remaining.length > 1) {
                for(int i = 0; i < remaining.length - 1; i++) {
                    target = target.resolve(remaining[i]);
                }

                work = remaining[remaining.length - 1];
            } else {
                work = remaining[0];
            }
            target.toFile().mkdirs();

            if(entry.getName().endsWith(".smdx")) {
                debug("Decoding SMDX: " + work);

                processed.getAndIncrement();
                try {
                    this.write(this.read(file.getInputStream(entry)), work, target.toFile());
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to read SMDX for pokemon: " + String.format("%s", entry.getName()));
                    e.printStackTrace();
                    System.exit(1);
                }
            } else if(entry.getName().endsWith(".smd")) {
                debug("Cloning SMD: " + work);
                processed.getAndIncrement();
                try {
                    this.copy(file.getInputStream(entry), target.resolve(work));
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to clone SMD for pokemon: " + String.format("%s", entry.getName()));
                }
            } else if(entry.getName().endsWith(".pqc")) {
                debug("Cloning PQC: " + work);
                processed.getAndIncrement();
                try {
                    this.copy(file.getInputStream(entry), target.resolve(work));
                    successful.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to clone PQC for pokemon: " + String.format("%s", entry.getName()));
                    e.printStackTrace();
                }
            }
        }

        private void copy(InputStream in, Path target) throws Exception {
            FileOutputStream stream = new FileOutputStream(target.toFile());
            ByteStreams.copy(in, stream);
            stream.flush();
            stream.close();
        }

        private SMDFile read(InputStream in) throws Exception {
            BufferedInputStream inputStream = new BufferedInputStream(in);

            try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(inputStream)) {
                return new SMDBinaryReader().read(unpacker);
            }
        }

        private void write(SMDFile smd, String name, File target) throws Exception {
            File outFile = new File(target, name.replaceFirst("[.][^.]+$", "") + ".smd");

            String result = new SMDTextWriter().write(smd);
            FileWriter writer = new FileWriter(outFile);
            writer.write(result);
            writer.flush();
            writer.close();
        }
    }

    public static class GensSerializer extends GenerationsTranslator implements Serializer {

        @Override
        public List<String> getValidFileSuffixes() {
            return Lists.newArrayList(".smd");
        }

        @Override
        public void process(File directory, File output) {
            debug("Processing Directory: " + directory.getAbsolutePath());
            output.toPath().resolve(directory.getName()).toFile().mkdirs();

            for(File file : Objects.requireNonNull(directory.listFiles())) {
                if(file.getName().endsWith(".smd")) {
                    debug("Encoding SMD: " + file.getName());
                    processed.getAndIncrement();

                    try {
                        SMDFile smd = this.read(file);
                        File outFile = new File(output, directory.getName() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + ".smdx");
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(outFile));

                        try (MessagePacker pack = MessagePack.newDefaultPacker(stream)) {
                            new SMDBinaryWriter().write(smd, pack);
                        }
                        successful.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(file.isDirectory()) {
                    process(file, output.toPath().resolve(directory.getName()).toFile());
                }
            }
        }

        @Override
        public void process(JarFile file, JarEntry entry, File output) {
            throw new UnsupportedOperationException();
        }

        private SMDFile read(File in) throws Exception{
            BufferedReader input = new BufferedReader(new FileReader(in));
            StringJoiner joiner = new StringJoiner("\n");
            String next;
            while((next = input.readLine()) != null) {
                joiner.add(next);
            }

            return new SMDTextReader().read(joiner.toString());
        }
    }

}
