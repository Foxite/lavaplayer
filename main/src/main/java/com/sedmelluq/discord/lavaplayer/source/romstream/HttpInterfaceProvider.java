package com.sedmelluq.discord.lavaplayer.source.romstream;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

public interface HttpInterfaceProvider extends AudioSourceManager {
	HttpInterface getHttpInterface();
}
