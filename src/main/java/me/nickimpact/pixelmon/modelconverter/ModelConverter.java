package me.nickimpact.pixelmon.modelconverter;

import me.nickimpact.pixelmon.modelconverter.generations.GenerationsTranslator;
import me.nickimpact.pixelmon.modelconverter.reforged.ReforgedTranslator;
import me.nickimpact.pixelmon.modelconverter.util.Time;

import java.io.File;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ModelConverter {

	public static boolean debug = false;

	public static AtomicInteger processed = new AtomicInteger(0);
	public static AtomicInteger successful = new AtomicInteger(0);

	public static void main(String[] args) {
		List<String> arguments = Arrays.stream(args).filter(a -> !a.startsWith("-")).collect(Collectors.toList());
		List<String> flags = Arrays.stream(args).filter(a -> a.startsWith("-")).collect(Collectors.toList());

		if(arguments.size() < 4) {
			System.err.println("Usage: java -jar (jar name) [-debug] (encode/decode) (pixelmon version) (input directory) (output directory)");
			System.exit(1);
		}

		if(!arguments.get(0).equalsIgnoreCase("decode") && !arguments.get(0).equalsIgnoreCase("encode")) {
			System.err.println("Unsure what procedure to run! Ensure you supply encode or decode as the first argument!");
		}

		boolean type = arguments.get(0).equalsIgnoreCase("decode");
		String version = arguments.get(1);
		File base = new File(arguments.get(2));
		File output = new File(arguments.get(3));

		if(flags.contains("-debug")) {
			debug = true;
		}

		Translator translator = parse(type, version).orElseGet(() -> {
			System.out.println("Invalid Pixelmon version... Must be Reforged or Generations");
			System.exit(1);
			return null;
		});

		output.mkdirs();

		try {
			final int total;
			System.out.println("Attempting to parse " + (total = (int) Files.find(
					base.toPath(),
					10,
					(p, a) -> {
						File file = p.toFile();
						return file.isDirectory() || matches(translator.getValidFileSuffixes(), file);
					})
					.filter(path -> !path.toFile().isDirectory())
					.count()) + " files...");

			final DecimalFormat df = new DecimalFormat("#0.00");
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			final ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
				System.out.println("Progress: " + df.format(processed.get() / (double) total * 100.0) + "% (" + processed.get() + "/" + total + ")");
			}, 2, 2, TimeUnit.SECONDS);

			Instant start = Instant.now();
			for (File pokemon : base.listFiles()) {
				translator.process(pokemon, output);
			}
			Instant end = Instant.now();

			Duration duration = Duration.between(start, end);
			task.cancel(true);
			scheduler.shutdownNow();
			System.out.println("Successfully parsed " + successful.get() + " out of " + total + " files! (Took " + new Time(duration.getSeconds()) + ")");
		} catch (Exception e) {
			System.err.println("Input directory appears invalid...");
			e.printStackTrace();
		}
	}

	public static void debug(String message) {
		if(debug) {
			System.out.println(message);
		}
	}

	private static Optional<Translator> parse(boolean type, String version) {
		if(version.toLowerCase().equals("reforged")) {
			return Optional.of(type ? new ReforgedTranslator.ReforgedDeserializer() : new ReforgedTranslator.ReforgedSerializer());
		} else if(version.toLowerCase().equals("generations")) {
			return Optional.of(type ? new GenerationsTranslator.GensDeserializer() : new GenerationsTranslator.GensSerializer());
		}

		return Optional.empty();
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
