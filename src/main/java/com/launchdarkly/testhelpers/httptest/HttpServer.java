package com.launchdarkly.testhelpers.httptest;

import com.launchdarkly.testhelpers.httptest.impl.JettyHttpServerDelegate;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * A simplified wrapper for an embedded test HTTP server.
 * <p>
 * See {@link com.launchdarkly.testhelpers.httptest} for more details and examples.
 */
public final class HttpServer implements Closeable {
  private final Delegate delegate;
  private final int port;
  private final URI uri;
  private final RequestRecorder recorder;
  
  /**
   * An abstraction for the part of the server implementation that could vary by platform.
   */
  public interface Delegate extends Closeable {
    /**
     * Starts the server and returns the port it's listening on.
     * 
     * @return the port
     * @throws IOException if starting the server fails
     */
    int start() throws IOException;
  }
  
  private HttpServer(Delegate delegate, int port, URI uri, RequestRecorder recorder) {
    this.delegate = delegate;
    this.port = port;
    this.uri = uri;
    this.recorder = recorder;
  }
  
  /**
   * Starts a new test server on a specific port.
   * 
   * @param port the port to listen on
   * @param handler An object or lambda that will handle all requests to this server. Use
   *   the factory methods in {@link Handlers} for standard handlers. If you will need
   *   to change the behavior of the handler during the lifetime of the server, use
   *   {@link HandlerSwitcher}.
   * @return the started server instance
   */
  public static HttpServer start(int port, Handler handler) {
    return startInternal(port, handler, null);
  }

  /**
   * Starts a new test server on any available port.
   * 
   * @param handler An object or lambda that will handle all requests to this server. Use
   *   the factory methods in {@link Handlers} for standard handlers. If you will need
   *   to change the behavior of the handler during the lifetime of the server, use
   *   {@link HandlerSwitcher}.
   * @return the started server instance
   */
  public static HttpServer start(Handler handler) {
    return start(0, handler);
  }

  /**
   * Starts a new HTTPS test server on a specific port.
   * 
   * @param tlsConfig certificate and key data; to use a self-signed certificate, call
   *   {@link ServerTLSConfiguration#makeSelfSignedCertificate()} 
   * @param port the port to listen on
   * @param handler An object or lambda that will handle all requests to this server. Use
   *   the factory methods in {@link Handlers} for standard handlers. If you will need
   *   to change the behavior of the handler during the lifetime of the server, use
   *   {@link HandlerSwitcher}.
   * @return the started server instance
   */
  public static HttpServer startSecure(ServerTLSConfiguration tlsConfig, int port, Handler handler) {
    return startInternal(port, handler, tlsConfig);
  }
  
  /**
   * Starts a new HTTPS test server on any available port.
   * 
   * @param certData certificate and key data; to use a self-signed certificate, call
   *   {@link ServerTLSConfiguration#makeSelfSignedCertificate()} 
   * @param handler An object or lambda that will handle all requests to this server. Use
   *   the factory methods in {@link Handlers} for standard handlers. If you will need
   *   to change the behavior of the handler during the lifetime of the server, use
   *   {@link HandlerSwitcher}.
   * @return the started server instance
   */
  public static HttpServer startSecure(ServerTLSConfiguration certData, Handler handler) {
    return startSecure(certData, 0, handler);
  }
  
  private static HttpServer startInternal(int port, Handler handler, ServerTLSConfiguration tlsConfig) {
    RequestRecorder recorder = new RequestRecorder();
    Handler rootHandler = ctx -> {
      recorder.apply(ctx);
      try {
        handler.apply(ctx);
      } catch (IllegalArgumentException e) {
        ctx.setStatus(400);
      } catch (Exception e) {
        ctx.setStatus(500);
      }
    };
    
    Delegate delegate = new JettyHttpServerDelegate(port, rootHandler, tlsConfig);

    int realPort;
    try {
      realPort = delegate.start();
    } catch (IOException e) {
      try {
        delegate.close();
      } catch (Exception ignore) {}
      throw new RuntimeException(e);
    }
    
    return new HttpServer(
        delegate,
        realPort,
        URI.create(String.format("%s://localhost:%d/",
            tlsConfig == null ? "http" : "https", realPort)),
        recorder
        );
  }
  
  /**
   * Returns the server's port.
   * 
   * @return the port
   */
  public int getPort() {
    return port;
  }
  
  /**
   * Returns the server's base URI.
   * 
   * @return the base URI
   */
  public URI getUri() {
    return uri;
  }
  
  /**
   * Returns the server's base URI.
   * 
   * @return the base URI as a URL
   */
  public URL getUrl() {
    try {
      return uri.toURL();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns the {@link RequestRecorder} that receives all requests to this server,
   * unless you disable it with {@link RequestRecorder#setEnabled(boolean)}.
   * 
   * @return the recorder
   */
  public RequestRecorder getRecorder() {
    return recorder;
  }
  
  /**
   * Shuts down the server.
   */
  @Override
  public void close() {
    try {
      delegate.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
