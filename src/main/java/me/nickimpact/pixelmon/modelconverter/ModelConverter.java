package me.nickimpact.pixelmon.modelconverter;

import me.nickimpact.pixelmon.modelconverter.generations.GenerationsTranslator;
import me.nickimpact.pixelmon.modelconverter.reforged.ReforgedTranslator;
import me.nickimpact.pixelmon.modelconverter.ui.MCInterface;
import me.nickimpact.pixelmon.modelconverter.util.Time;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModelConverter {

	public static Pattern JAR_PATTERN = Pattern.compile("assets/pixelmon/models/pokemon/(?<species>[a-z-0-9]+)[/](?<rest>.+)");
	public static boolean debug = true;

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

				Instant start = Instant.now();
				try {
					if(in.getName().endsWith(".jar") && state) {
						JarFile file = new JarFile(in);
						Pattern suffix = Pattern.compile("[.][a-zA-Z]+");
						Set<JarEntry> entries = file.stream()
								.filter(in -> {
									Matcher matcher = suffix.matcher(in.getName());
									if (in.getName().startsWith("assets/pixelmon/models/pokemon/") && matcher.find()) {
										return translator.getValidFileSuffixes().contains(matcher.group());
									}
									return false;
								})
								.sorted(Comparator.comparing(JarEntry::getName))
								.collect(Collectors.toCollection(LinkedHashSet::new));

						total.setText("Total: " + entries.size());
						for (JarEntry entry : entries) {
							try {
								translator.process(file, entry, out);
							} catch (Exception e) {
								Matcher matcher = JAR_PATTERN.matcher(entry.getName());
								matcher.find();
								status.setText("Failure parsing species: " + matcher.group("species"));
								e.printStackTrace();
								break;
							} finally {
								processed.setText("Processed: " + ModelConverter.processed.get());
								successful.setText("Successful: " + ModelConverter.successful.get());
								progress.setValue((int) (ModelConverter.processed.get() / (double) entries.size() * 100.0));
								publish();
							}

							Instant end = Instant.now();

							Duration duration = Duration.between(start, end);
							status.setText("Successfully parsed " + ModelConverter.successful.get() + " out of " + entries.size() + " files! (Took " + new Time(duration.toMillis()) + ")");
							publish();
						}
					} else if(in.getName().endsWith(".jar")) {
						status.setText("Reading .jar files is only available for deserialization!");
					} else {
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
					}
				} catch (Exception e) {
					status.setText("Encountered a failure, consult the latest generated log file!");
					File errors = new File("PMC-Errors");
					errors.mkdirs();
					File now = new File(errors, Instant.now() + ".log");
					BufferedWriter writer = new BufferedWriter(new FileWriter(now));
					try(StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
						e.printStackTrace(pw);
						pw.flush();
						String[] trace = sw.toString().split("(\r)?\n");
						for(String s : trace) {
							writer.write(s);
						}
						writer.flush();
						writer.close();
					}

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
