package com.sedmelluq.discord.lavaplayer.source.romstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RomStreamTrackInfo {
	public int index;
	public String key;
	public String[] names;
	public RomStreamRomData romData;
}
