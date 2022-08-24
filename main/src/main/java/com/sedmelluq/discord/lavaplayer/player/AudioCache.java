package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public interface AudioCache {
	void store(AudioReference reference, AudioTrack track);
	boolean tryGet(AudioReference reference, AudioLoadResultHandler loadHandler);
}
