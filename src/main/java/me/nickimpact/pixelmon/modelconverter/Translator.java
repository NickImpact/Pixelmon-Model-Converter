package me.nickimpact.pixelmon.modelconverter;

import java.io.File;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public interface Translator {

    List<String> getValidFileSuffixes();

    void process(File directory, File output);

    void process(JarFile file, JarEntry entry, File output);

    interface Deserializer extends Translator {}

    interface Serializer extends Translator {}

}
