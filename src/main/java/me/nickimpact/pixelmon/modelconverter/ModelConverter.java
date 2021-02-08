package me.nickimpact.pixelmon.modelconverter;

import me.nickimpact.pixelmon.modelconverter.generations.GenerationsTranslator;
import me.nickimpact.pixelmon.modelconverter.reforged.ReforgedTranslator;
import me.nickimpact.pixelmon.modelconverter.ui.MCInterface;
import me.nickimpact.pixelmon.modelconverter.util.Time;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelConverter {

	public static boolean debug = false;

	public static AtomicInteger processed = new AtomicInteger(0);
	public static AtomicInteger successful = new AtomicInteger(0);

	public static void main(String[] args) {
		new MCInterface().open();
	}

	public static void run(boolean mode, boolean state, File in, File out, JTextField status, JLabel total, JLabel processed, JLabel successful, JProgressBar progress) {
		Translator translator = parse(mode, state);
		out.mkdirs();

		ModelConverter.processed.set(0);
		ModelConverter.successful.set(0);

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				total.setText("Total: " + 0);
				processed.setText("Processed: " + ModelConverter.processed.get());
				successful.setText("Successful: " + ModelConverter.successful.get());
				publish();

				try {
					final int t = (int) Files.find(
							in.toPath(),
							10,
							(p, a) -> {
								File file = p.toFile();
								return file.isDirectory() || matches(translator.getValidFileSuffixes(), file);
							})
							.filter(path -> !path.toFile().isDirectory())
							.count();
					total.setText("Total: " + t);

					Instant start = Instant.now();
					for (File pokemon : in.listFiles()) {
						try {
							translator.process(pokemon, out);
						} catch (Exception e) {
							status.setText("Failure parsing species: " + pokemon.getName());
							e.printStackTrace();
							break;
						} finally {
							processed.setText("Processed: " + ModelConverter.processed.get());
							successful.setText("Successful: " + ModelConverter.successful.get());
							progress.setValue((int) (ModelConverter.processed.get() / (double) t * 100.0));
							publish();
						}
					}

					Instant end = Instant.now();

					Duration duration = Duration.between(start, end);
					status.setText("Successfully parsed " + ModelConverter.successful.get() + " out of " + t + " files! (Took " + new Time(duration.toMillis()) + ")");
					publish();
				} catch (Exception e) {
					status.setText("Encountered a failure, consult the latest generated log file!");
					e.printStackTrace();
					publish();
				}

				return null;
			}
		};

		worker.execute();
	}

	public static void debug(String message) {
		if(debug) {
			System.out.println(message);
		}
	}

	private static Translator parse(boolean version, boolean type) {
		if(version) {
			return type ? new ReforgedTranslator.ReforgedDeserializer() : new ReforgedTranslator.ReforgedSerializer();
		} else {
			return type ? new GenerationsTranslator.GensDeserializer() : new GenerationsTranslator.GensSerializer();
		}
	}

	private static boolean matches(List<String> suffixes, File check) {
		for(String suffix : suffixes) {
			if(check.getName().endsWith(suffix)) {
				return true;
			}
		}

		return false;
	}

}
