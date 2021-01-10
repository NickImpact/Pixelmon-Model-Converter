package me.nickimpact.pixelmon.modelconverter.generations;

import com.google.common.collect.Lists;
import me.nickimpact.pixelmon.modelconverter.Translator;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.formats.SMDBinaryReader;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.NodesBlock;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.SMDFile;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.SMDFileBlock;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.SkeletonBlock;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.TrianglesBlock;
import me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl.VertexAnimationBlock;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.nickimpact.pixelmon.modelconverter.ModelConverter.*;

public class GenerationsTranslator implements Translator {

    @Override
    public List<String> getValidFileSuffixes() {
        return Lists.newArrayList(".smdx", ".smd", "pqc");
    }

    @Override
    public void process(File directory, File output) {
        debug("Processing Directory: " + directory.getAbsolutePath());

        output.toPath().resolve(directory.getName()).toFile().mkdirs();

        for(File file : Objects.requireNonNull(directory.listFiles())) {
            if(file.getName().endsWith(".smdx")) {
                debug("Parsing SMDX: " + file.getName());
                processed.getAndIncrement();
                try {
                    this.write(this.read(file), file, new File(output, directory.getName()));
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

    private SMDFile read(File in) throws Exception{
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(in));

        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(inputStream)) {
            return new SMDBinaryReader().read(unpacker);
        }
    }

    private void write(SMDFile smd, File focus, File target) throws Exception {
        File outFile = new File(target, focus.getName().replaceFirst("[.][^.]+$", "") + ".smd");
        FileWriter fw = new FileWriter(outFile);
        this.writeLine(fw, "version 1");
        this.writeLine(fw, "nodes");
        int n = 0;
        for(NodesBlock node : this.filter(smd.blocks, NodesBlock.class)) {
            this.writeLine(fw, String.format("%d \"%s\" %d", node.bones.get(n).id, node.bones.get(n).name, node.bones.get(n).parent));
        }
        this.writeLine(fw, "end");

        this.writeLine(fw, "skeleton");
        for(SkeletonBlock skeleton : this.filter(smd.blocks, SkeletonBlock.class)) {
            for(SkeletonBlock.Keyframe frame : skeleton.keyframes) {
                this.writeLine(fw, String.format("time %d", frame.time));
                for(SkeletonBlock.BoneState state : frame.states) {
                    if(allZero(state.posX, state.posY, state.posZ, state.rotX, state.rotY, state.rotZ)) {
                        writeLine(fw, String.format("%d  0 0 0  0 0 0", state.bone));
                    } else {
                        writeLine(fw, String.format(Locale.US, "%d  %f %f %f  %f %f %f", state.bone, state.posX, state.posY, state.posZ, state.rotX, state.rotY, state.rotZ));
                    }
                }
            }
        }
        this.writeLine(fw, "end");

        for(TrianglesBlock block : this.filter(smd.blocks, TrianglesBlock.class)) {
            if(block.triangles.size() > 0) {
                this.writeLine(fw, "triangles");
                for(TrianglesBlock.Triangle triangle : block.triangles) {
                    this.writeLine(fw, triangle.material);
                    for(TrianglesBlock.Vertex vertex : triangle.vertices) {
                        String result = String.format(
                                Locale.US,
                                "%d  %f %f %f  %f %f %f  %f %f %d",
                                vertex.parentBone,
                                vertex.posX, vertex.posY, vertex.posZ,
                                vertex.normX, vertex.normY, vertex.normZ,
                                vertex.u, vertex.v,
                                vertex.links.size()
                        );

                        for(TrianglesBlock.Link link : vertex.links) {
                            result = String.format(Locale.US, "%s %d %f", result, link.bone, link.weight);
                        }
                        this.writeLine(fw, result);
                    }
                }
                this.writeLine(fw, "end");
            }

        }

        if(this.filter(smd.blocks, VertexAnimationBlock.class).size() > 0) {
            this.writeLine(fw, "vertexanimation");
            for(VertexAnimationBlock block : this.filter(smd.blocks, VertexAnimationBlock.class)) {
                for(VertexAnimationBlock.Keyframe frame : block.keyframes) {
                    this.writeLine(fw, String.format("time %d", frame.time));
                    for(VertexAnimationBlock.VertexState state : frame.states) {
                        if(allZero(state.posX, state.posY, state.posZ, state.normX, state.normY, state.normZ)) {
                            writeLine(fw, String.format("%d  0 0 0  0 0 0", state.vertex));
                        } else {
                            writeLine(fw, String.format(Locale.US, "%d  %f %f %f  %f %f %f", state.vertex, state.posX, state.posY, state.posZ, state.normX, state.normY, state.normZ));
                        }
                    }
                }
            }
            this.writeLine(fw, "end");
        }

        fw.flush();
        fw.close();
    }

    private void writeLine(FileWriter writer, String out) throws Exception {
        writer.write(out);
        writer.write("\n");
        writer.flush();
    }

    private <T extends SMDFileBlock> List<T> filter(List<SMDFileBlock> input, Class<T> desired) {
        return input.stream().filter(b -> desired.isAssignableFrom(b.getClass())).map(desired::cast).collect(Collectors.toList());
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
