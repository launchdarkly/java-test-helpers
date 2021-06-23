package com.launchdarkly.testhelpers.httptest.impl;

import com.launchdarkly.testhelpers.httptest.Handler;
import com.launchdarkly.testhelpers.httptest.HttpServer;
import com.launchdarkly.testhelpers.httptest.RequestContext;
import com.launchdarkly.testhelpers.httptest.RequestInfo;
import com.launchdarkly.testhelpers.httptest.ServerTLSConfiguration;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Jetty implementation used by {@link HttpServer}.
 * @author elibishop
 *
 */
public final class JettyHttpServerDelegate implements HttpServer.Delegate {
  private final Server server;
  
  /**
   * Creates the Jetty server.
   * 
   * @param port the port to listen on, or zero
   * @param handler the root handler
   * @param tlsConfig TLS configuration for an HTTPS server, or null for an HTTP server
   */
  public JettyHttpServerDelegate(int port, Handler handler, ServerTLSConfiguration tlsConfig) {
    server = makeServer(port, tlsConfig);

    server.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest req, HttpServletResponse resp)
          throws IOException, ServletException {
        baseRequest.setHandled(true);
        RequestContext ctx = new ServletRequestContextImpl(new RequestInfo(req), resp);
        handler.apply(ctx);
      }
    });

    server.setStopTimeout(100); // without this, Jetty does not interrupt worker threads on shutdown    
  }
  
  public int start() {
    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }    
    return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
  }
  
  public void close() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private static Server makeServer(int port, ServerTLSConfiguration tlsConfig) {
    if (tlsConfig == null) {
      return new Server(port);
    }
    
    Server server = new Server();
    
    HttpConfiguration httpsConfig = new HttpConfiguration();
    httpsConfig.addCustomizer(new SecureRequestCustomizer());
    
    SslContextFactory sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.addExcludeProtocols("TLSv1.3");
    try {
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      ks.load(null);
      ks.setCertificateEntry("localhost", tlsConfig.getCertificate());
      ks.setEntry("localhost",
          new KeyStore.PrivateKeyEntry(tlsConfig.getPrivateKey(), new Certificate[] { tlsConfig.getCertificate() }),
          new KeyStore.PasswordProtection("secret".toCharArray()));
      sslContextFactory.setKeyStore(ks);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    sslContextFactory.setKeyManagerPassword("secret");
    sslContextFactory.setKeyStorePassword("secret");
    sslContextFactory.setTrustAll(true);
    sslContextFactory.setValidateCerts(false);
    
    ServerConnector connector = new ServerConnector(server, sslContextFactory);
    connector.setPort(port);
    server.setConnectors(new Connector[] { connector });
    
    return server;
  }
}
