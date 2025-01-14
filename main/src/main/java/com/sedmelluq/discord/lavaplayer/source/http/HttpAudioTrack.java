package com.sedmelluq.discord.lavaplayer.source.http;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.romstream.HttpInterfaceProvider;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles processing HTTP addresses as audio tracks.
 */
public class HttpAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(HttpAudioTrack.class);

  private final MediaContainerDescriptor containerTrackFactory;
  private final HttpInterfaceProvider sourceManager;
  private final String authorization;

  /**
   * @param trackInfo Track info
   * @param containerTrackFactory Container track factory - contains the probe with its parameters.
   * @param sourceManager Source manager used to load this track
   * @param authorization Authorization header value, or null.
   */
  public HttpAudioTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerTrackFactory,
                        HttpInterfaceProvider sourceManager, String authorization) {

    super(trackInfo);

    this.containerTrackFactory = containerTrackFactory;
    this.sourceManager = sourceManager;
    this.authorization = authorization;
  }

  /**
   * @return The media probe which handles creating a container-specific delegated track for this track.
   */
  public MediaContainerDescriptor getContainerTrackFactory() {
    return containerTrackFactory;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      log.debug("Starting http track from URL: {}", trackInfo.identifier);

      try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(trackInfo.identifier), Units.CONTENT_LENGTH_UNKNOWN, authorization)) {
        processDelegate((InternalAudioTrack) containerTrackFactory.createTrack(trackInfo, inputStream), localExecutor);
      }
    }
  }

  @Override
  protected AudioTrack makeShallowClone() {
    return new HttpAudioTrack(trackInfo, containerTrackFactory, sourceManager, authorization);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
