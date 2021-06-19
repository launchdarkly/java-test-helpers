package com.launchdarkly.testhelpers.httptest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simplified wrapper for an embedded test HTTP server.
 * <p>
 * See {@link com.launchdarkly.testhelpers.httptest} for more details and examples.
 */
public final class HttpServer implements Closeable {
  private final Server server;
  private final RequestRecorder recorder;
  private final int port;
  private final URI uri;
  private volatile boolean recording;
  
  private HttpServer(int port, Handler handler) {
    this.server = new Server(port);
    this.recorder = new RequestRecorder();
    this.recording = true;
    
    server.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest req, HttpServletResponse resp)
          throws IOException, ServletException {
        baseRequest.setHandled(true);
        RequestContext ctx = new RequestContext(new RequestInfo(req), resp);
        if (recording) {
          recorder.apply(ctx);
        }
        try {
          handler.apply(ctx);
        } catch (IllegalArgumentException e) {
          resp.setStatus(400);
        } catch (Exception e) {
          resp.setStatus(500);
        }
      }
    });
    
    server.setStopTimeout(100); // without this, Jetty does not interrupt worker threads on shutdown
    
    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    if (port == 0) {
      this.port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    } else {
      this.port = port;
    }
    this.uri = URI.create("http://localhost:" + this.port + "/");
  }
  
  /**
   * Starts a new test server on a specific port.
   * 
   * @param port The port to listen on
   * @param handler An object or lambda that will handle all requests to this server. Use
   *   the factory methods in {@link Handlers} for standard handlers. If you will need
   *   to change the behavior of the handler during the lifetime of the server, use
   *   {@link HandlerSwitcher}.
   * @return the started server instance
   */
  public static HttpServer start(int port, Handler handler) {
    return new HttpServer(port, handler);
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
    return new HttpServer(0, handler);
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
   * Returns the {@link RequestRecorder} that receives all requests to this server whenever
   * {@link #isRecording()} is true.
   * 
   * @return the recorder
   */
  public RequestRecorder getRecorder() {
    return recorder;
  }
  
  /**
   * Returns true if the server is recording requests. This is true by default.
   * 
   * @return true if recording requests
   */
  public boolean isRecording() {
    return recording;
  }
  
  /**
   * Sets whether to record requests. This is true by default.
   * 
   * @param recording true to record requests
   */
  public void setRecording(boolean recording) {
    this.recording = recording;
  }

  /**
   * Shuts down the server.
   */
  @Override
  public void close() throws IOException {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
