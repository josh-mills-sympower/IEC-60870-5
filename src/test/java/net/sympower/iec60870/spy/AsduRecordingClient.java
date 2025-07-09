package net.sympower.iec60870.spy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sympower.iec60870.common.api.IEC60870EventListener;
import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.ASduType;

public class AsduRecordingClient implements IEC60870EventListener, ClientSpy {
  private volatile boolean connectionReady = false;
  private volatile boolean connectionLost = false;
  private final AtomicReference<ASdu> lastReceivedAsdu = new AtomicReference<>();
  private final ConcurrentLinkedQueue<ASdu> receivedAsdus = new ConcurrentLinkedQueue<>();

  @Override
  public void onConnectionReady() {
    this.connectionReady = true;
    this.connectionLost = false;
  }

  @Override
  public void onAsduReceived(ASdu asdu) {
    lastReceivedAsdu.set(asdu);
    receivedAsdus.offer(asdu);
  }

  @Override
  public void onConnectionLost(IOException cause) {
    this.connectionLost = true;
    this.connectionReady = false;
  }

  @Override
  public boolean isConnectionReady() {
    return connectionReady;
  }

  public boolean isConnectionLost() {
    return connectionLost;
  }

  @Override
  public boolean hasReceivedAsdu() {
    return lastReceivedAsdu.get() != null;
  }

  public ASdu getLastReceivedAsdu() {
    return lastReceivedAsdu.get();
  }

  @Override
  public ASdu getLastReceivedAsduOfType(ASduType type) {
    return receivedAsdus.stream()
        .filter(asdu -> asdu.getTypeIdentification() == type)
        .reduce((first, second) -> second)
        .orElse(null);
  }
}
