/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.gc.simulator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.chemclipse.logging.core.Logger;

import net.openchrom.xxd.control.supplier.gc.simulator.protocol.GcAcqPayloadEncoder;
import net.openchrom.xxd.control.supplier.gc.simulator.simulation.PesticideChromatogramSimulator;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcCommand;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcFrame;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcFrameCodec;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcFrameParser;

public final class VirtualGcClientSession implements Runnable {

	private static final Logger logger = Logger.getLogger(VirtualGcClientSession.class);
	private static final byte ACK_OK = 0x00;
	private static final int SAMPLES_PER_BATCH = 8;

	private final Socket socket;
	private final Consumer<String> statusListener;
	private final Runnable onClosed;
	private final GcFrameParser frameParser = new GcFrameParser();
	private final AtomicBoolean acquiring = new AtomicBoolean(false);
	private final AtomicInteger deviceSequence = new AtomicInteger(0);
	private final PesticideChromatogramSimulator simulator = new PesticideChromatogramSimulator();

	private Thread acquisitionThread;
	private int sampleIntervalMs = 100;
	private volatile OutputStream outputStream;

	public VirtualGcClientSession(Socket socket, Consumer<String> statusListener, Runnable onClosed) {

		this.socket = socket;
		this.statusListener = statusListener;
		this.onClosed = onClosed;
	}

	@Override
	public void run() {

		try(InputStream inputStream = socket.getInputStream()) {
			outputStream = socket.getOutputStream();
			notifyStatus("Client connected: " + socket.getRemoteSocketAddress());
			byte[] buffer = new byte[4096];
			while(!socket.isClosed()) {
				int read = inputStream.read(buffer);
				if(read < 0) {
					break;
				}
				if(read == 0) {
					continue;
				}
				byte[] chunk = new byte[read];
				System.arraycopy(buffer, 0, chunk, 0, read);
				handleChunk(chunk);
			}
		} catch(IOException e) {
			if(!socket.isClosed()) {
				logger.warn("Virtual GC client session ended", e);
			}
		} finally {
			stopAcquisition();
			closeQuietly();
			notifyStatus("Client disconnected");
			onClosed.run();
		}
	}

	public void close() {

		stopAcquisition();
		closeQuietly();
	}

	private void handleChunk(byte[] chunk) {

		frameParser.append(chunk);
		for(GcFrame frame : frameParser.drainFrames()) {
			handleFrame(frame);
		}
	}

	private void handleFrame(GcFrame frame) {

		try {
			switch(frame.getCommand()) {
				case GcCommand.START_ACQ -> handleStartAcquisition(frame);
				case GcCommand.STOP_ACQ -> handleStopAcquisition(frame);
				case GcCommand.HEARTBEAT -> sendAck(frame.getSequence(), GcCommand.HEARTBEAT);
				default -> sendNack(frame.getSequence(), frame.getCommand());
			}
		} catch(IOException e) {
			logger.warn("Failed to handle frame " + frame, e);
			closeQuietly();
		}
	}

	private void handleStartAcquisition(GcFrame frame) throws IOException {

		sampleIntervalMs = parseSampleInterval(frame.getPayload());
		sendAck(frame.getSequence(), GcCommand.START_ACQ);
		startAcquisition();
		notifyStatus("Acquisition started, interval=" + sampleIntervalMs + " ms");
	}

	private void handleStopAcquisition(GcFrame frame) throws IOException {

		sendAck(frame.getSequence(), GcCommand.STOP_ACQ);
		stopAcquisition();
		notifyStatus("Acquisition stopped by workstation");
	}

	private void startAcquisition() {

		stopAcquisition();
		acquiring.set(true);
		acquisitionThread = Thread.ofVirtual().name("virtual-gc-acquisition").start(this::runAcquisition);
	}

	private void stopAcquisition() {

		acquiring.set(false);
		if(acquisitionThread != null) {
			acquisitionThread.interrupt();
			acquisitionThread = null;
		}
	}

	private void runAcquisition() {

		int retentionTimeMs = 0;
		int totalSamples = 0;
		int batchSequence = 0;
		int runTimeMs = PesticideChromatogramSimulator.defaultRunTimeMs();
		List<GcAcqSample> batch = new ArrayList<>(SAMPLES_PER_BATCH);

		try {
			while(acquiring.get() && retentionTimeMs <= runTimeMs) {
				float signal = simulator.signalAt(retentionTimeMs);
				batch.add(new GcAcqSample(retentionTimeMs, signal));
				totalSamples++;
				if(batch.size() >= SAMPLES_PER_BATCH) {
					sendDataBatch(batchSequence++, batch);
					batch.clear();
				}
				retentionTimeMs += sampleIntervalMs;
				Thread.sleep(sampleIntervalMs);
			}
			if(!batch.isEmpty()) {
				sendDataBatch(batchSequence++, batch);
				batch.clear();
			}
			if(totalSamples > 0) {
				sendDone(totalSamples, Math.max(0, batchSequence - 1));
				notifyStatus("Acquisition completed, samples=" + totalSamples);
			}
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch(IOException e) {
			logger.warn("Acquisition stream failed", e);
			notifyStatus("Acquisition failed: " + e.getMessage());
		} finally {
			acquiring.set(false);
		}
	}

	private void sendDataBatch(int batchSequence, List<GcAcqSample> samples) throws IOException {

		byte[] payload = GcAcqPayloadEncoder.encodeDataBatch(batchSequence, samples);
		sendFrame(GcCommand.ACQ_DATA, nextDeviceSequence(), payload);
	}

	private void sendDone(int totalSamples, int lastBatchSequence) throws IOException {

		byte[] payload = GcAcqPayloadEncoder.encodeDone(totalSamples, lastBatchSequence);
		sendFrame(GcCommand.ACQ_DONE, nextDeviceSequence(), payload);
	}

	private void sendAck(byte sequence, byte originalCommand) throws IOException {

		byte[] payload = new byte[] {originalCommand, ACK_OK};
		sendFrame(GcCommand.ACK, sequence, payload);
	}

	private void sendNack(byte sequence, byte originalCommand) throws IOException {

		byte[] payload = new byte[] {originalCommand, 0x01};
		sendFrame(GcCommand.NACK, sequence, payload);
	}

	private synchronized void sendFrame(byte command, byte sequence, byte[] payload) throws IOException {

		if(outputStream == null || socket.isClosed()) {
			throw new IOException("Client socket closed");
		}
		byte[] frame = GcFrameCodec.encode(command, sequence, payload);
		outputStream.write(frame);
		outputStream.flush();
	}

	private byte nextDeviceSequence() {

		return (byte)(deviceSequence.updateAndGet(value -> (value + 1) & 0xFF) & 0xFF);
	}

	private static int parseSampleInterval(byte[] payload) {

		if(payload == null || payload.length < 2) {
			return 100;
		}
		int interval = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
		return Math.max(20, Math.min(interval, 5_000));
	}

	private void notifyStatus(String message) {

		if(statusListener != null) {
			statusListener.accept(message);
		}
	}

	private void closeQuietly() {

		try {
			socket.close();
		} catch(IOException e) {
			logger.warn("Failed to close client socket", e);
		}
	}
}
