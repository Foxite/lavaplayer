package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Mostly for testing. Tends to cause a memory leak.
 */
public class MemoryAudioCache implements AudioCache {
	private static final Logger log = LoggerFactory.getLogger(MemoryAudioCache.class);

	private final HashMap<AudioReference, AudioTrack> cache = new HashMap<>();

	@Override
	public void store(AudioReference reference, AudioTrack track) {
		cache.put(reference, track);
	}

	@Override
	public boolean tryGet(AudioReference reference, AudioLoadResultHandler loadHandler) {
		AudioTrack cachedItem = cache.getOrDefault(reference, null);
		if (cachedItem != null) {
			log.debug("Loaded from cache: " + reference.identifier);
			loadHandler.trackLoaded(cachedItem);
			return true;
		} else {
			log.debug("Not in cache: " + reference.identifier);
			return false;
		}
	}
}
