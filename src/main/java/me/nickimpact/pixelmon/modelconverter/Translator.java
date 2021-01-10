package me.nickimpact.pixelmon.modelconverter;

import java.io.File;
import java.util.List;

public interface Translator {

    List<String> getValidFileSuffixes();

    void process(File directory, File output);

}
