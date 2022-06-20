package com.sedmelluq.discord.lavaplayer.container.l16;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;

public class L16ContainerProbe implements MediaContainerProbe {
	private static final Logger LOGGER = LoggerFactory.getLogger(L16ContainerProbe.class);

	@Override
	public String getName() {
		return "l16";
	}

	@Override
	public boolean matchesHints(MediaContainerHints hints) {
		return hints.mimeType != null && hints.mimeType.startsWith("audio/l16");
	}

	@Override
	public MediaContainerDetectionResult probe(MediaContainerHints hints, AudioReference reference, SeekableInputStream inputStream) throws IOException {
		LOGGER.info(hints.mimeType);

		AudioTrackInfoProvider file = new L16TrackProvider(reference);

		return supportedFormat(this, hints.mimeType, AudioTrackInfoBuilder.create(reference, inputStream).apply(file).setIsStream(true).build());
	}

	@Override
	public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
		String[] splitParameters = parameters.split(";");
		HashMap<String, String> parsedParameters = new HashMap<>();
		for (int i = 1; i < splitParameters.length; i++) {
			String[] components = splitParameters[i].split("=");
			parsedParameters.put(components[0].trim(), components[1].trim());
		}

		int rate = Integer.parseInt(parsedParameters.getOrDefault("rate", "48000"));
		int channels = Integer.parseInt(parsedParameters.getOrDefault("channel", "1"));

		LOGGER.info(String.valueOf(rate));
		LOGGER.info(String.valueOf(channels));

		return new L16AudioTrack(trackInfo, inputStream, rate, channels);
	}
}
