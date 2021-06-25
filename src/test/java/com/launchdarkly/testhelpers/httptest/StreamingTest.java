package com.launchdarkly.testhelpers.httptest;

import com.google.common.collect.ImmutableList;
import com.launchdarkly.testhelpers.BaseTest;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.launchdarkly.testhelpers.httptest.TestUtil.simpleGet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import okhttp3.Response;

@SuppressWarnings("javadoc")
public class StreamingTest extends BaseTest {
  @Test
  public void basicChunkedResponseWithNoCharsetInHeader() throws Exception {
    List<String> chunks = ImmutableList.of("first.", "second.", "third");
    List<Handler> handlers = new ArrayList<>();
    for (String chunk: chunks) {
      handlers.add(Handlers.writeChunkString(chunk));
    }
    doStreamingTest(
        Handlers.startChunks("text/plain", null),
        handlers,
        Handlers.hang(),
        "text/plain",
        chunks
        );
  }
  
  @Test
  public void basicChunkedResponseWithCharsetInHeader() throws Exception {
    List<String> chunks = ImmutableList.of("first.", "second.", "third");
    List<Handler> handlers = new ArrayList<>();
    for (String chunk: chunks) {
      handlers.add(Handlers.writeChunkString(chunk));
    }
    doStreamingTest(
        Handlers.startChunks("text/plain", Charset.forName("UTF-8")),
        handlers,
        Handlers.hang(),
        "text/plain;charset=utf-8",
        chunks
        );
  }
  
  @Test
  public void sseStream() throws Exception {
    doStreamingTest(
        Handlers.SSE.start(),
        ImmutableList.of(
             Handlers.SSE.event("e1", "d1"),
             Handlers.SSE.comment("comment"),
             Handlers.SSE.event("e2", "d2"),
             Handlers.SSE.event("data: all done")
        ),
        Handlers.SSE.leaveOpen(),
        "text/event-stream;charset=utf-8",
        ImmutableList.of(
            "event: e1\ndata: d1\n\n",
            ":comment\n",
            "event: e2\ndata: d2\n\n",
            "data: all done\n\n"
            )
        );
  }
  
  private void doStreamingTest(
      Handler startAction,
      List<Handler> chunkActions,
      Handler endAction,
      String expectedContentType,
      List<String> expectedChunks
      ) throws Exception {
    Semaphore[] didWriteChunk = new Semaphore[expectedChunks.size()];
    Semaphore[] didReadChunk = new Semaphore[expectedChunks.size()];
    for (int i = 0; i < expectedChunks.size(); i++) {
      didWriteChunk[i] = new Semaphore(0);
      didReadChunk[i] = new Semaphore(0);
    }
    
    Handler handler = Handlers.all(
        startAction,
        ctx -> {
          for (int i = 0; i < expectedChunks.size(); i++) {
            chunkActions.get(i).apply(ctx);
            didWriteChunk[i].release();
            try {
              didReadChunk[i].acquire();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        },
        endAction
        );
    
    try (HttpServer server = HttpServer.start(handler)) {
      try (Response resp = simpleGet(server.getUri())) {
        assertThat(resp.code(), equalTo(200));
        assertThat(resp.header("Content-Type"), Matchers.equalToIgnoringCase(expectedContentType));
        
        InputStream stream = resp.body().byteStream();
        
        for (int i = 0; i < expectedChunks.size(); i++) {
          didWriteChunk[i].acquire();
          
          byte[] buf = new byte[100];
          int n = stream.read(buf);
          String s = new String(buf, 0, n);
          assertThat(s, equalTo(expectedChunks.get(i)));
          
          didReadChunk[i].release();
        }
      }
    }
  }
}
