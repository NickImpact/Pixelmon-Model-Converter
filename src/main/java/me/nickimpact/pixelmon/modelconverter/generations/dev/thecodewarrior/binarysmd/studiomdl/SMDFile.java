package me.nickimpact.pixelmon.modelconverter.generations.dev.thecodewarrior.binarysmd.studiomdl;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SMDFile {
    public @NotNull List<@NotNull SMDFileBlock> blocks = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SMDFile)) return false;
        SMDFile smdFile = (SMDFile) o;
        return blocks.equals(smdFile.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks);
    }
}
