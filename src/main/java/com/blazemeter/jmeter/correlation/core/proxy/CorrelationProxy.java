package com.blazemeter.jmeter.correlation.core.proxy;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import org.apache.jmeter.protocol.http.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
This class allow us to deliver recorded samplers in same order as received requests.

With default implementation of Proxy, one request might get proxied (get request from client, send
request to server, get response from server and send response to client) and subsequent request
might be proxied before the first request is actually delivered (because first one might still be
processing correlations), which results in a recorded test plan with incorrect order of samplers and
incorrectly applied correlation rules (e.g.: we may be overwriting an extracted value with the value
extracted from a previous response, or replacing a value extracted in a later request).

To avoid these issues with default implementation of Proxy, this class notifies
CorrelationProxyControl for each new proxy (one proxy is created for each request), which adds it
to a list (to keep the order of requests). Then, when the request is ready for delivery,
CorrelationProxyControl will link the Proxy instance (which is the thread executing the
CorrelationProxyControl.deliverSampler method) to the actual sampler, result and children
test elements. Finally, when the proxy ends execution, it notifies the CorrelationProxyControl which
will deliver the Proxy associated sampler if all previous requests have already been delivered,
otherwise it will mark it as complete and whenever all previous requests are completed
the request will be eventually delivered by CorrelationProxyControl.

We can't mark as complete the Proxy execution (as we do with the final step of the proxy execution)
in deliverSampler method since deliverSampler method is not called for every request, and would end
up leaving unfinished proxies blocking any further progress in subsequent proxies.
 */
public class CorrelationProxy extends Proxy {
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxy.class);
  private static final Field TARGET_FIELD = ReflectionUtils.getField(Proxy.class, "target");
  private static final Field CLIENT_SOCKET_FIELD = ReflectionUtils
      .getField(Proxy.class, "clientSocket");

  public CorrelationProxy() {
    ReflectionUtils.checkFields(Proxy.class, TARGET_FIELD, CLIENT_SOCKET_FIELD);
  }

  @Override
  public void run() {
    CorrelationProxyControl proxyControl = getField(TARGET_FIELD, CorrelationProxyControl.class);
    wrapClientSocketWithProxyControlNotifierSocket(proxyControl);
    try {
      super.run();
    } catch (org.jsoup.UncheckedIOException e) {
      LOG.warn("Error while processing a request: {}", e.getMessage());
    } catch (Exception exception) {
      LOG.warn("There was an unexpected error while processing the last request: {}",
          exception.getMessage());
    }
    proxyControl.endedProxy(this);
  }

  private void wrapClientSocketWithProxyControlNotifierSocket(
      CorrelationProxyControl proxyControl) {
    Socket clientSocket = getField(CLIENT_SOCKET_FIELD, Socket.class);
    try {
      CLIENT_SOCKET_FIELD.set(this, new ProxyControlNotifierSocket(clientSocket, proxyControl));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T getField(Field field, Class<T> fieldType) {
    try {
      return fieldType.cast(field.get(this));
    } catch (IllegalAccessException e) {
      // this should never happen since we modify the visibility of the field
      throw new RuntimeException(e);
    }
  }

  /*
   * Class that wraps a Socket and notifies a correlationProxy when output stream is obtained to
   * properly order requests.
   *
   * The plugin now relies on getOutputStream to be the marker to order requests for later placing
   * in recorded test plan. Using proxy start time has proven to be not good enough, since in some
   * cases multiple connections are established at same time, but first attended connection is not
   * necessarily the first request to process (connection might get blocked until
   * request is sent by browser, which might happen after another concurrent request is actually
   * sent). Using getOutputStream, which is invoked right after request is read and parsed, is
   * better for getting proper order.
   */
  private static class ProxyControlNotifierSocket extends Socket {

    private final Socket clientSocket;
    private final CorrelationProxyControl correlationProxyControl;

    private ProxyControlNotifierSocket(Socket clientSocket,
        CorrelationProxyControl correlationProxyControl) {
      this.clientSocket = clientSocket;
      this.correlationProxyControl = correlationProxyControl;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      correlationProxyControl.startedProxy(Thread.currentThread());
      return clientSocket.getOutputStream();
    }

    // from this point on there are only delegations and no custom code

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
      clientSocket.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
      clientSocket.connect(endpoint, timeout);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
      clientSocket.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
      return clientSocket.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
      return clientSocket.getLocalAddress();
    }

    @Override
    public int getPort() {
      return clientSocket.getPort();
    }

    @Override
    public int getLocalPort() {
      return clientSocket.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
      return clientSocket.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
      return clientSocket.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
      return clientSocket.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return clientSocket.getInputStream();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
      return clientSocket.getTcpNoDelay();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
      clientSocket.setTcpNoDelay(on);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
      clientSocket.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
      return clientSocket.getSoLinger();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
      clientSocket.sendUrgentData(data);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
      return clientSocket.getOOBInline();
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
      clientSocket.setOOBInline(on);
    }

    @Override
    public int getSoTimeout() throws SocketException {
      return clientSocket.getSoTimeout();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      clientSocket.setSoTimeout(timeout);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
      return clientSocket.getSendBufferSize();
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
      clientSocket.setSendBufferSize(size);
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
      return clientSocket.getReceiveBufferSize();
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
      clientSocket.setReceiveBufferSize(size);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
      return clientSocket.getKeepAlive();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
      clientSocket.setKeepAlive(on);
    }

    @Override
    public int getTrafficClass() throws SocketException {
      return clientSocket.getTrafficClass();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
      clientSocket.setTrafficClass(tc);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
      return clientSocket.getReuseAddress();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
      clientSocket.setReuseAddress(on);
    }

    @Override
    public void close() throws IOException {
      clientSocket.close();
    }

    @Override
    public void shutdownInput() throws IOException {
      clientSocket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
      clientSocket.shutdownOutput();
    }

    @Override
    public String toString() {
      return clientSocket.toString();
    }

    @Override
    public boolean isConnected() {
      return clientSocket.isConnected();
    }

    @Override
    public boolean isBound() {
      return clientSocket.isBound();
    }

    @Override
    public boolean isClosed() {
      return clientSocket.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
      return clientSocket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
      return clientSocket.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
      clientSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
  }
}
