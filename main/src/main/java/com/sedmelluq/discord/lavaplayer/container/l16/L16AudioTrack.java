package com.sedmelluq.discord.lavaplayer.container.l16;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.MPEG1_SAMPLES_PER_FRAME;

public class L16AudioTrack extends BaseAudioTrack {
	private static final Logger LOGGER = LoggerFactory.getLogger(L16ContainerProbe.class);

	private final InputStream inputStream;
	private final int rate;
	private final int channels;

	public L16AudioTrack(AudioTrackInfo trackInfo, InputStream inputStream, int rate, int channels) {
		super(trackInfo);
		this.inputStream = inputStream;
		this.rate = rate;
		this.channels = channels;
	}

	@Override
	public boolean isSeekable() {
		return false;
	}

	@Override
	public void process(LocalAudioTrackExecutor executor) throws Exception {
		AudioProcessingContext apc = executor.getProcessingContext();
		AudioPipeline pipeline = AudioPipelineFactory.create(apc, new PcmFormat(channels, rate));

		short[] shorts = new short[2048];
		byte[] bytes = new byte[shorts.length * 2];

		executor.executeProcessingLoop(() -> {
			int bytesRead = inputStream.read(bytes);
			if (bytesRead <= 0) {
				LOGGER.info("GONE " + String.valueOf(bytesRead));
				throw new EOFException();
			}

			int shortsRead = bytesRead / 2;
			for (int i = 0; i < shortsRead; i++) {
				shorts[i] = (short) (bytes[i] << 8 | bytes[i * 2 + 1]);
			}

			LOGGER.info(String.valueOf(shortsRead));
			pipeline.process(shorts, 0, shortsRead);
		}, null);
	}
}
