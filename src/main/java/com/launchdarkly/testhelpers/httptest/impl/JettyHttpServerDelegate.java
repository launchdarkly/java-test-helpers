package com.launchdarkly.testhelpers.httptest.impl;

import com.launchdarkly.testhelpers.httptest.Handler;
import com.launchdarkly.testhelpers.httptest.HttpServer;
import com.launchdarkly.testhelpers.httptest.RequestContext;
import com.launchdarkly.testhelpers.httptest.RequestInfo;
import com.launchdarkly.testhelpers.httptest.ServerTLSConfiguration;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
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

    server.setGracefulShutdown(100); // without this, Jetty does not interrupt worker threads on shutdown    
  }
  
  public int start() {
    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return server.getConnectors()[0].getLocalPort();
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

    final PrivateKey privateKey = tlsConfig.getPrivateKey();
    final X509Certificate certificate = tlsConfig.getCertificate();
    
    X509ExtendedKeyManager keyManager = new X509ExtendedKeyManager() {
      private static final String ARBITRARY_SERVER_ALIAS = "server";
      
      @Override
      public String[] getServerAliases(String arg0, Principal[] arg1) {
        return new String[] { ARBITRARY_SERVER_ALIAS }; 
      }
      
      @Override
      public PrivateKey getPrivateKey(String arg0) {
        return privateKey;
      }
      
      @Override
      public String[] getClientAliases(String arg0, Principal[] arg1) {
        return null;
      }
      
      @Override
      public X509Certificate[] getCertificateChain(String arg0) {
        return new X509Certificate[] { certificate };
      }
      
      @Override
      public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
        return ARBITRARY_SERVER_ALIAS;
      }
      
      @Override
      public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
        return null;
      }
    };
    
    X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
      
      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }
      
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }
      
      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
      }
      
      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
      }
      
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
      }
      
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
      }
    };
    
    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(new KeyManager[] { keyManager }, new TrustManager[] { trustManager }, new SecureRandom());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (KeyManagementException e) {
      throw new RuntimeException();
    }
    
    SslSocketConnector connector = new SslSocketConnector();
    connector.setPort(port);
    connector.setSslContext(sslContext);
    server.setConnectors(new Connector[] { connector });
    
    return server;
  }
}
