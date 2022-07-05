package com.sedmelluq.discord.lavaplayer.source.romstream;

import com.sedmelluq.discord.lavaplayer.container.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.*;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dmfs.httpessentials.apache4.ApacheExecutor;
import org.dmfs.httpessentials.client.HttpRequest;
import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.entities.XWwwFormUrlEncodedEntity;
import org.dmfs.httpessentials.exceptions.ProtocolError;
import org.dmfs.httpessentials.exceptions.ProtocolException;
import org.dmfs.iterables.elementary.PresentValues;
import org.dmfs.jems.iterable.composite.Joined;
import org.dmfs.jems.iterable.elementary.Seq;
import org.dmfs.jems.pair.elementary.ValuePair;
import org.dmfs.jems.single.elementary.ValueSingle;
import org.dmfs.oauth2.client.*;
import org.dmfs.oauth2.client.http.requests.AbstractAccessTokenRequest;
import org.dmfs.oauth2.client.http.requests.parameters.GrantTypeParam;
import org.dmfs.oauth2.client.http.requests.parameters.OptionalScopeParam;
import org.dmfs.oauth2.client.http.requests.parameters.PasswordParam;
import org.dmfs.oauth2.client.http.requests.parameters.UsernameParam;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.fetchResponseAsJson;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.getHeaderValue;

/**
 * Audio source manager that implements finding Bandcamp tracks based on URL.
 */
public class RomStreamAudioSourceManager extends ProbingAudioSourceManager implements HttpConfigurable, HttpInterfaceProvider {
  private static final Pattern URL_REGEX = Pattern.compile("^romstream:(?<rom>.+?)/(?<track>.+?)$");
  private static final String ROMSTREAM_ENDPOINT = System.getenv("ROMSTREAM_ENDPOINT");
  private static final String AUTH_ENDPOINT = System.getenv("ROMSTREAM_AUTH_ENDPOINT");
  private static final String TOKEN_ENDPOINT = System.getenv("ROMSTREAM_TOKEN_ENDPOINT");
  private static final String CLIENT_ID = System.getenv("ROMSTREAM_CLIENT_ID");
  private static final String BOT_USERNAME = System.getenv("ROMSTREAM_BOT_USERNAME");
  private static final String BOT_PASSWORD = System.getenv("ROMSTREAM_BOT_PASSWORD");
  private static final String REDIRECT_URL = System.getenv("ROMSTREAM_REDIRECT_URL");

  private final HttpInterfaceManager httpInterfaceManager;
  private BasicOAuth2Client authClient;
  private OAuth2AccessToken cachedAuthToken;
  private CharSequence cachedAccessToken;

  /**
   * Create a new instance with default media container registry.
   */
  public RomStreamAudioSourceManager() {
    this(MediaContainerRegistry.DEFAULT_REGISTRY);
  }

  /**
   * Create a new instance.
   */
  public RomStreamAudioSourceManager(MediaContainerRegistry containerRegistry) {
    super(containerRegistry);

    httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
            HttpClientTools
                    .createSharedCookiesHttpBuilder()
                    .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
            HttpClientTools.DEFAULT_REQUEST_CONFIG
    );
  }

  @Override
  public String getSourceName() {
    return "romstream";
  }

  @Override
  public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
    TrackQuery urlInfo = parseUrl(reference.identifier);

    if (urlInfo != null) {
      String urlPrefix;
      String urlSuffix;
      try {
        urlPrefix = ROMSTREAM_ENDPOINT + "/Tracks/" + URLEncoder.encode(urlInfo.romName, "UTF-8") + "/";
        urlSuffix = "?trackQuery=" + URLEncoder.encode(urlInfo.trackQuery, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // Not supposed to happen
        throw new RuntimeException(e);
      }

      AudioTrackInfo audioTrackInfo;
      try {
        String searchUrl = urlPrefix + "Search" + urlSuffix;
        RomStreamTrackInfo trackInfo;
        HttpGet infoRequest = new HttpGet(searchUrl);
        infoRequest.addHeader("authorization", getAccessToken());
        try (CloseableHttpResponse response = getHttpInterface().execute(infoRequest)) {
          trackInfo = JsonBrowser.parse(response.getEntity().getContent()).index(0).as(RomStreamTrackInfo.class);
        }

        audioTrackInfo = AudioTrackInfoBuilder.create(reference, null)
                .setAuthor(trackInfo.romData.fullName)
                .setTitle(trackInfo.names[0] + " (" + trackInfo.key + ")")
                .build();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      String playUrl = urlPrefix + "Play" + urlSuffix;
      reference = new AudioReference(playUrl, urlInfo.trackQuery);

      AudioReference httpReference = getAsHttpReference(reference);
      if (httpReference == null) {
        return null;
      }

      if (httpReference.containerDescriptor != null) {
        return createTrack(audioTrackInfo, httpReference.containerDescriptor);
      } else {
        return handleLoadResult(detectContainer(httpReference));
      }
    }

    return null;
  }

  private TrackQuery parseUrl(String url) {
    Matcher matcher = URL_REGEX.matcher(url);

    if (matcher.matches()) {
      return new TrackQuery(matcher.group("rom"), matcher.group("track"));
    } else {
      return null;
    }
  }

  private void initializeAuthClient() {
    if (authClient != null) {
      return;
    }

    // Create OAuth2 provider
    OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
            URI.create(AUTH_ENDPOINT),
            URI.create(TOKEN_ENDPOINT),
            new Duration(1,0,3600) /* default expiration time in case the server doesn't return any */);

    // Create OAuth2 client
    authClient = new BasicOAuth2Client(
            provider,
            new OAuth2ClientCredentials() {
              @Override
              public <T> HttpRequest<T> authenticatedRequest(HttpRequest<T> request) {
                return request;
              }

              @Override
              public String clientId() {
                return CLIENT_ID;
              }
            },
            new LazyUri(new Precoded(REDIRECT_URL)) /* Redirect URL */);
  }

  private ApacheExecutor getHttpExecutor() {
    return new ApacheExecutor(new ValueSingle<>(getHttpInterface().getHttpClient()));
  }

  private OAuth2AccessToken getAuthToken() throws ProtocolException, IOException, ProtocolError {
    initializeAuthClient();

    OAuth2Grant grant = new OAuth2Grant() {
      @Override
      public OAuth2AccessToken accessToken(HttpRequestExecutor executor) throws IOException, ProtocolError, ProtocolException {
        AuthentikMachineToMachineTokenRequest tokenRequest = new AuthentikMachineToMachineTokenRequest(new BasicScope("openid", "roles"), CLIENT_ID, BOT_USERNAME, BOT_PASSWORD);
        return authClient.accessToken(tokenRequest, executor);
      }
    };

    HttpRequestExecutor executor = getHttpExecutor();
    return grant.accessToken(executor);
  }

  private String getAccessToken() {
    try {
      if (cachedAuthToken == null || cachedAuthToken.expirationDate().before(DateTime.now().addDuration(Duration.parse("PT30S")))) {
        cachedAuthToken = getAuthToken();
        cachedAccessToken = cachedAuthToken.accessToken();
      }

      return "Bearer " + cachedAccessToken;
    } catch (Exception e) {
      throw new FriendlyException("Error authenticating with RomStream.", FriendlyException.Severity.FAULT, e);
    }
  }

  private static class TrackQuery {
    public final String romName;
    public final String trackQuery;

    public TrackQuery(String romName, String trackQuery) {
      this.romName = romName;
      this.trackQuery = trackQuery;
    }
  }

  private static class AuthentikMachineToMachineTokenRequest extends AbstractAccessTokenRequest {
    public AuthentikMachineToMachineTokenRequest(OAuth2Scope scope, String clientId, String botUsername, String botPassword) {
      super(scope,
        new XWwwFormUrlEncodedEntity(
          new Joined<>(
            new Seq<>(
              new GrantTypeParam("client_credentials"),
              new ValuePair<>("client_id", clientId),
              new UsernameParam(botUsername),
              new PasswordParam(botPassword)
            ),
            new PresentValues<>(new OptionalScopeParam(scope))
          )
        )
      );
    }
  }

  @Override
  protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerDescriptor) {
    return new HttpAudioTrack(trackInfo, containerDescriptor, this, getAccessToken());
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  public static AudioReference getAsHttpReference(AudioReference reference) {
    if (reference.identifier.startsWith("https://") || reference.identifier.startsWith("http://")) {
      return reference;
    } else if (reference.identifier.startsWith("icy://")) {
      return new AudioReference("http://" + reference.identifier.substring(6), reference.title);
    }
    return null;
  }

  private MediaContainerDetectionResult detectContainer(AudioReference reference) {
    MediaContainerDetectionResult result;

    try (HttpInterface httpInterface = getHttpInterface()) {
      result = detectContainerWithClient(httpInterface, reference);
    } catch (IOException e) {
      throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
    }

    return result;
  }

  private MediaContainerDetectionResult detectContainerWithClient(HttpInterface httpInterface, AudioReference reference) throws IOException {
    String authToken = getAccessToken();

    try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(reference.identifier), Units.CONTENT_LENGTH_UNKNOWN, authToken)) {
      int statusCode = inputStream.checkStatusCode();
      String redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.getCurrentResponse());

      if (redirectUrl != null) {
        return refer(null, new AudioReference(redirectUrl, null));
      } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
      }

      MediaContainerHints hints = MediaContainerHints.from(getHeaderValue(inputStream.getCurrentResponse(), "Content-Type"), null);
      return new MediaContainerDetection(containerRegistry, reference, inputStream, hints).detectContainer();
    } catch (URISyntaxException e) {
      throw new FriendlyException("Not a valid URL.", COMMON, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    encodeTrackFactory(((HttpAudioTrack) track).getContainerTrackFactory(), output);
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    MediaContainerDescriptor containerTrackFactory = decodeTrackFactory(input);

    if (containerTrackFactory != null) {
      return new HttpAudioTrack(trackInfo, containerTrackFactory, this, null);
    }

    return null;
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }
}
