package org.ziggrid.utils.metrics;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;

public class CodeHaleMetrics {
	
	public final static MetricRegistry metrics = new MetricRegistry();
	
	public static void configureReports(String metricsDirectory, int frequencyInSeconds) {
		final CsvReporter reporter = CsvReporter.forRegistry(metrics)
				.formatFor(Locale.US)
	            .convertRatesTo(TimeUnit.SECONDS)
	            .convertDurationsTo(TimeUnit.MILLISECONDS)
	            .build(new File(metricsDirectory));
		reporter.start(frequencyInSeconds, TimeUnit.SECONDS);
	}

}
