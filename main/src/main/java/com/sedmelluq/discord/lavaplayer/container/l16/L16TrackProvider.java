package com.sedmelluq.discord.lavaplayer.container.l16;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

public class L16TrackProvider implements AudioTrackInfoProvider {
	private final AudioReference reference;

	public L16TrackProvider(AudioReference reference) {
		this.reference = reference;
	}

	public String getTitle()      { return null; }
	public String getAuthor()     { return null; }
	public Long getLength()       { return null; }
	public String getIdentifier() { return reference.getIdentifier(); }
	public String getUri()        { return reference.getUri(); }
}
