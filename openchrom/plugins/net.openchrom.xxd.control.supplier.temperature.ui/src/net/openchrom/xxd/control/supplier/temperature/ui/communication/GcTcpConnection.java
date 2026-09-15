/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.chemclipse.logging.core.Logger;

public class GcTcpConnection {

	private static final Logger logger = Logger.getLogger(GcTcpConnection.class);
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 30000;
	private static final int READ_BUFFER_BYTES = 4096;

	private final Object writeLock = new Object();
	private final List<IGcFrameListener> frameListeners = new CopyOnWriteArrayList<>();
	private final ConcurrentHashMap<Integer, CompletableFuture<GcFrame>> pendingResponses = new ConcurrentHashMap<>();
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private final AtomicInteger sequenceCounter = new AtomicInteger(0);
	private final GcFrameParser frameParser = new GcFrameParser();
	private final AtomicReference<AsciiCapture> asciiCapture = new AtomicReference<>();
	private final ReentrantLock asciiExchangeLock = new ReentrantLock();

	private Runnable connectionLostHandler;

	private GcDeviceEndpoint endpoint;
	private Socket socket;
	private OutputStream outputStream;
	private Thread readerThread;
	private final AtomicLong lastInboundNanos = new AtomicLong(System.nanoTime());

	public boolean isConnected() {

		return connected.get();
	}

	public GcDeviceEndpoint getEndpoint() {

		return endpoint;
	}

	public void setConnectionLostHandler(Runnable connectionLostHandler) {

		this.connectionLostHandler = connectionLostHandler;
	}

	public void connect(GcDeviceEndpoint endpoint) throws IOException {

		Objects.requireNonNull(endpoint, "endpoint");
		synchronized(writeLock) {
			disconnectInternal();
			Socket newSocket = new Socket();
			try {
				newSocket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), CONNECT_TIMEOUT_MS);
				newSocket.setTcpNoDelay(true);
				/*
				 * F407 app_tcp_server.c: GCWS\tHELLO\tPC → HELLO_OK\n
				 * Must complete before the binary reader thread starts consuming the stream.
				 * HELLO PC can replace a stale owner after an abnormal disconnect.
				 */
				logger.info("Performing GCWS handshake with " + endpoint);
				GcAsciiProtocol.performWorkstationHandshake(newSocket);
				newSocket.setSoTimeout(READ_TIMEOUT_MS);

				this.endpoint = endpoint;
				this.socket = newSocket;
				this.outputStream = newSocket.getOutputStream();
				this.connected.set(true);
				noteInbound();
				frameParser.reset();
				startReader();
				logger.info("Connected and handshake OK with GC device at " + endpoint);
			} catch(IOException e) {
				closeQuietly(newSocket);
				disconnectInternal();
				throw e;
			}
		}
	}

	/**
	 * Sends one ASCII protocol line (CRLF appended), matching F407 {@code app_tcp_server.c}.
	 */
	public void sendProtocolLine(String lineWithoutCrLf) throws IOException {

		Objects.requireNonNull(lineWithoutCrLf, "lineWithoutCrLf");
		synchronized(writeLock) {
			if(!connected.get() || outputStream == null) {
				throw new IOException("Not connected to GC device");
			}
			byte[] bytes = GcAsciiProtocol.encodeLine(lineWithoutCrLf);
			outputStream.write(bytes);
			outputStream.flush();
			logger.info("Sent ASCII line: " + lineWithoutCrLf);
		}
	}

	/**
	 * F407 {@code GCWS\tREAD_OVEN_PROGRAM} → JSON from {@code w25q32_kvdb_column_program_json}.
	 */
	public ColumnOvenProgram readOvenProgram(long timeoutMs) throws IOException {

		String json = exchangeAscii(GcAsciiProtocol.READ_OVEN_PROGRAM_LINE, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return ColumnOvenProgram.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid oven program JSON from device: " + e.getMessage(), e);
		}
	}

	/**
	 * F407 {@code GCWS\tWRITE_OVEN_PROGRAM} + JSON → {@code WRITE_OK} / {@code WRITE_FAIL}.
	 */
	public void writeOvenProgram(ColumnOvenProgram program, long timeoutMs) throws IOException {

		Objects.requireNonNull(program, "program");
		String json = program.toDeviceJson();
		if(json.length() >= 1536) {
			throw new IOException("Oven program JSON exceeds device buffer (1536 bytes)");
		}
		String ack = exchangeAscii(GcAsciiProtocol.WRITE_OVEN_PROGRAM_LINE, json, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		String normalized = ack == null ? "" : ack.toUpperCase(Locale.ROOT);
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ACK)) {
			return;
		}
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ERR)) {
			throw new IOException("Device rejected oven program (WRITE_FAIL)");
		}
		throw new IOException("Unexpected oven write reply: " + (ack == null ? "(empty)" : ack.strip()));
	}

	/**
	 * F407 {@code GCWS\tREAD_OVEN_PID} → JSON {@code {"kp","ki","kd"}}.
	 */
	public OvenPid readOvenPid(long timeoutMs) throws IOException {

		return readDevicePid(GcAsciiProtocol.READ_OVEN_PID_LINE, timeoutMs, "oven PID");
	}

	/**
	 * F407 {@code GCWS\tWRITE_OVEN_PID} + JSON → {@code WRITE_OK} / {@code WRITE_FAIL}.
	 */
	public void writeOvenPid(OvenPid pid, long timeoutMs) throws IOException {

		writeDevicePid(GcAsciiProtocol.WRITE_OVEN_PID_LINE, pid, timeoutMs, "oven PID");
	}

	public OvenPid readInletPid(long timeoutMs) throws IOException {

		return readDevicePid(GcAsciiProtocol.READ_INLET_PID_LINE, timeoutMs, "inlet PID");
	}

	public void writeInletPid(OvenPid pid, long timeoutMs) throws IOException {

		writeDevicePid(GcAsciiProtocol.WRITE_INLET_PID_LINE, pid, timeoutMs, "inlet PID");
	}

	public OvenPid readDetectorPid(long timeoutMs) throws IOException {

		return readDevicePid(GcAsciiProtocol.READ_DETECTOR_PID_LINE, timeoutMs, "detector PID");
	}

	public void writeDetectorPid(OvenPid pid, long timeoutMs) throws IOException {

		writeDevicePid(GcAsciiProtocol.WRITE_DETECTOR_PID_LINE, pid, timeoutMs, "detector PID");
	}

	private OvenPid readDevicePid(String readLine, long timeoutMs, String label) throws IOException {

		String json = exchangeAscii(readLine, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return OvenPid.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid " + label + " JSON from device: " + e.getMessage(), e);
		}
	}

	private void writeDevicePid(String writeLine, OvenPid pid, long timeoutMs, String label) throws IOException {

		Objects.requireNonNull(pid, "pid");
		String json = pid.toDeviceJson();
		String ack = exchangeAscii(writeLine, json, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		String normalized = ack == null ? "" : ack.toUpperCase(Locale.ROOT);
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ACK)) {
			return;
		}
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ERR)) {
			throw new IOException("Device rejected " + label + " (WRITE_FAIL)");
		}
		throw new IOException("Unexpected " + label + " write reply: " + (ack == null ? "(empty)" : ack.strip()));
	}

	/**
	 * F407 {@code GCWS\tREAD_OVEN_TEMP} → JSON setpoint/actual from F103 live data.
	 */
	public OvenLiveTemp readOvenTemp(long timeoutMs) throws IOException {

		String json = exchangeAscii(GcAsciiProtocol.READ_OVEN_TEMP_LINE, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return OvenLiveTemp.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid oven temp JSON from device: " + e.getMessage(), e);
		}
	}

	public AuxLiveTemp readInletTemp(long timeoutMs) throws IOException {

		return readAuxTemp(GcAsciiProtocol.READ_INLET_TEMP_LINE, timeoutMs, "inlet");
	}

	public void writeInletTemp(float setpointC, long timeoutMs) throws IOException {

		writeAuxTemp(GcAsciiProtocol.WRITE_INLET_TEMP_LINE, setpointC, timeoutMs, "inlet");
	}

	public AuxLiveTemp readDetectorTemp(long timeoutMs) throws IOException {

		return readAuxTemp(GcAsciiProtocol.READ_DETECTOR_TEMP_LINE, timeoutMs, "detector");
	}

	public void writeDetectorTemp(float setpointC, long timeoutMs) throws IOException {

		writeAuxTemp(GcAsciiProtocol.WRITE_DETECTOR_TEMP_LINE, setpointC, timeoutMs, "detector");
	}

	public void writeZoneSelect(boolean inlet, boolean detector, boolean oven, long timeoutMs) throws IOException {

		int sel = 0;
		if(inlet) {
			sel |= 1;
		}
		if(detector) {
			sel |= 2;
		}
		if(oven) {
			sel |= 4;
		}
		String json = "{\"sel\":\"" + sel + "\"}";
		String ack = exchangeAscii(GcAsciiProtocol.WRITE_ZONE_SELECT_LINE, json, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		assertWriteOk(ack, "WRITE_ZONE_SELECT");
	}

	private AuxLiveTemp readAuxTemp(String readLine, long timeoutMs, String label) throws IOException {

		String json = exchangeAscii(readLine, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return AuxLiveTemp.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid " + label + " temp JSON from device: " + e.getMessage(), e);
		}
	}

	private void writeAuxTemp(String writeLine, float setpointC, long timeoutMs, String label) throws IOException {

		String json = AuxLiveTemp.toDeviceJson(setpointC);
		String ack = exchangeAscii(writeLine, json, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		assertWriteOk(ack, "WRITE_" + label.toUpperCase(Locale.ROOT) + "_TEMP");
	}

	public void startInletHeat(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.START_INLET_HEAT_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "START_INLET_HEAT");
	}

	public void stopInletHeat(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.STOP_INLET_HEAT_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "STOP_INLET_HEAT");
	}

	public void startDetectorHeat(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.START_DETECTOR_HEAT_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "START_DETECTOR_HEAT");
	}

	public void stopDetectorHeat(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.STOP_DETECTOR_HEAT_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "STOP_DETECTOR_HEAT");
	}

	/**
	 * F407 {@code GCWS\tSTART_OVEN_CONTROL} → enable heater + run KVDB oven program → WRITE_OK.
	 */
	public void startOvenControl(long timeoutMs) throws IOException {

		String ack = exchangeAscii(GcAsciiProtocol.START_OVEN_CONTROL_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		assertWriteOk(ack, "START_OVEN_CONTROL");
	}

	/**
	 * F407 {@code GCWS\tSTOP_OVEN_CONTROL} → stop program + heater off → WRITE_OK.
	 */
	public void stopOvenControl(long timeoutMs) throws IOException {

		String ack = exchangeAscii(GcAsciiProtocol.STOP_OVEN_CONTROL_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs);
		assertWriteOk(ack, "STOP_OVEN_CONTROL");
	}

	public void startFidIgnite(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.START_FID_IGNITE_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "START_FID_IGNITE");
	}

	public void stopFidValves(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.STOP_FID_VALVES_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "STOP_FID_VALVES");
	}

	public void startFidValves(long timeoutMs) throws IOException {

		assertWriteOk(exchangeAscii(GcAsciiProtocol.START_FID_VALVES_LINE, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), "START_FID_VALVES");
	}

	public void setFidAutoIgnite(boolean enable, long timeoutMs) throws IOException {

		String line = enable ? GcAsciiProtocol.FID_AUTO_ON_LINE : GcAsciiProtocol.FID_AUTO_OFF_LINE;
		assertWriteOk(exchangeAscii(line, null, AsciiCapture.Mode.WRITE_ACK, timeoutMs), enable ? "FID_AUTO_ON" : "FID_AUTO_OFF");
	}

	public FidStatus readFidStatus(long timeoutMs) throws IOException {

		String json = exchangeAscii(GcAsciiProtocol.READ_FID_STATUS_LINE, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return FidStatus.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid FID status JSON from device: " + e.getMessage(), e);
		}
	}

	public GasPressure readGasPressure(long timeoutMs) throws IOException {

		String json = exchangeAscii(GcAsciiProtocol.READ_GAS_PRESSURE_LINE, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return GasPressure.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid gas pressure JSON from device: " + e.getMessage(), e);
		}
	}

	public ControlStatus readControlStatus(long timeoutMs) throws IOException {

		String json = exchangeAscii(GcAsciiProtocol.READ_CONTROL_STATUS_LINE, null, AsciiCapture.Mode.JSON_OBJECT, timeoutMs);
		try {
			return ControlStatus.fromDeviceJson(json);
		} catch(RuntimeException e) {
			throw new IOException("Invalid control status JSON from device: " + e.getMessage(), e);
		}
	}

	/**
	 * Keeps the F407 PC-owner watchdog alive. Skips the ASCII round-trip when
	 * inbound TCP data (including acquisition frames) was seen recently, or when
	 * another ASCII command already holds the channel.
	 *
	 * @return {@code false} if skipped because the link already has recent traffic
	 */
	public boolean tryOwnerHeartbeat(long timeoutMs) throws IOException {

		if(!connected.get()) {
			throw new IOException("Not connected to GC device");
		}
		if(hasRecentInbound(1500L)) {
			return false;
		}
		if(!asciiExchangeLock.tryLock()) {
			return false;
		}
		asciiExchangeLock.unlock();
		try {
			readControlStatus(timeoutMs);
		} catch(IOException ex) {
			String message = ex.getMessage();
			if(message != null && message.contains("previous ASCII protocol exchange")) {
				return false;
			}
			throw ex;
		}
		return true;
	}

	public boolean hasRecentInbound(long maxAgeMs) {

		long ageNs = System.nanoTime() - lastInboundNanos.get();
		if(ageNs < 0L) {
			return true;
		}
		return TimeUnit.NANOSECONDS.toMillis(ageNs) < maxAgeMs;
	}

	private void noteInbound() {

		lastInboundNanos.set(System.nanoTime());
	}

	private static void assertWriteOk(String ack, String op) throws IOException {

		String normalized = ack == null ? "" : ack.toUpperCase(Locale.ROOT);
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ACK)) {
			return;
		}
		if(normalized.contains(GcAsciiProtocol.WRITE_DENIED)) {
			throw new IOException("Device denied " + op + " (DENIED: PC holds write lock)");
		}
		if(normalized.contains(GcAsciiProtocol.WRITE_OVEN_ERR)) {
			throw new IOException("Device rejected " + op + " (WRITE_FAIL)");
		}
		throw new IOException("Unexpected " + op + " reply: " + (ack == null ? "(empty)" : ack.strip()));
	}

	public void disconnect() {

		synchronized(writeLock) {
			disconnectInternal();
		}
	}

	private String exchangeAscii(String commandLine, String payloadAfterLine, AsciiCapture.Mode mode, long timeoutMs) throws IOException {

		long waitMs = Math.max(200L, timeoutMs);
		boolean locked;
		try {
			locked = asciiExchangeLock.tryLock(waitMs, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted waiting for ASCII channel", e);
		}
		if(!locked) {
			throw new IOException("Timed out waiting for previous ASCII protocol exchange");
		}
		CompletableFuture<String> responseFuture = new CompletableFuture<>();
		AsciiCapture capture = new AsciiCapture(mode, responseFuture);
		asciiCapture.set(capture);
		try {
			synchronized(writeLock) {
				if(!connected.get() || outputStream == null) {
					throw new IOException("Not connected to GC device");
				}
				outputStream.write(GcAsciiProtocol.encodeLine(commandLine));
				if(payloadAfterLine != null && !payloadAfterLine.isEmpty()) {
					outputStream.write(payloadAfterLine.getBytes(StandardCharsets.UTF_8));
				}
				outputStream.flush();
				logger.info("Sent ASCII exchange: " + commandLine + (payloadAfterLine == null ? "" : " +JSON(" + payloadAfterLine.length() + ")"));
			}
			return responseFuture.get(waitMs, TimeUnit.MILLISECONDS);
		} catch(TimeoutException e) {
			throw new IOException("Timed out waiting for ASCII reply to " + commandLine, e);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted waiting for ASCII reply to " + commandLine, e);
		} catch(Exception e) {
			if(e instanceof IOException io) {
				throw io;
			}
			Throwable cause = e.getCause();
			if(cause instanceof IOException io) {
				throw io;
			}
			throw new IOException("ASCII exchange failed for " + commandLine + ": " + e.getMessage(), e);
		} finally {
			asciiCapture.compareAndSet(capture, null);
			if(!responseFuture.isDone()) {
				responseFuture.cancel(true);
			}
			asciiExchangeLock.unlock();
		}
	}

	public void sendFrame(byte command, byte[] payload) throws IOException {

		synchronized(writeLock) {
			byte sequence = nextSequence();
			byte[] frame = GcFrameCodec.encode(command, sequence, payload);
			writeFrame(frame);
			logger.info("Sent frame: " + GcCommand.toString(command) + " seq=" + (sequence & 0xFF) + " payload=" + frame.length + " bytes");
		}
	}

	public GcFrame sendFrameAndWait(byte command, byte[] payload, long timeoutMs) throws IOException {

		byte sequence;
		CompletableFuture<GcFrame> responseFuture = new CompletableFuture<>();
		synchronized(writeLock) {
			sequence = nextSequence();
			pendingResponses.put(sequence & 0xFF, responseFuture);
			byte[] frame = GcFrameCodec.encode(command, sequence, payload);
			writeFrame(frame);
			logger.info("Sent frame (wait): " + GcCommand.toString(command) + " seq=" + (sequence & 0xFF));
		}
		try {
			GcFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
			if(response.getCommand() == GcCommand.NACK) {
				throw new IOException("Device NACK for " + GcCommand.toString(command));
			}
			return response;
		} catch(TimeoutException e) {
			throw new IOException("Response timeout for " + GcCommand.toString(command), e);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for " + GcCommand.toString(command), e);
		} catch(Exception e) {
			if(e instanceof IOException io) {
				throw io;
			}
			throw new IOException("Failed to send frame " + GcCommand.toString(command), e);
		} finally {
			pendingResponses.remove(sequence & 0xFF, responseFuture);
		}
	}

	public void addFrameListener(IGcFrameListener listener) {

		frameListeners.add(listener);
	}

	public void removeFrameListener(IGcFrameListener listener) {

		frameListeners.remove(listener);
	}

	private void disconnectInternal() {

		connected.set(false);
		failPendingResponses(new IOException("Disconnected"));
		failAsciiCapture(new IOException("Disconnected"));
		if(readerThread != null) {
			readerThread.interrupt();
			readerThread = null;
		}
		closeQuietly(outputStream);
		outputStream = null;
		closeQuietly(socket);
		socket = null;
		endpoint = null;
		frameParser.reset();
	}

	private byte nextSequence() {

		return (byte)(sequenceCounter.updateAndGet(value -> (value + 1) & 0xFF) & 0xFF);
	}

	private void writeFrame(byte[] frame) throws IOException {

		if(!connected.get() || outputStream == null) {
			throw new IOException("Not connected to GC device");
		}
		outputStream.write(frame);
		outputStream.flush();
	}

	private void startReader() {

		readerThread = Thread.ofVirtual().name("gc-tcp-reader").start(() -> {
			try(InputStream inputStream = socket.getInputStream()) {
				byte[] buffer = new byte[READ_BUFFER_BYTES];
				while(connected.get()) {
					int read;
					try {
						read = inputStream.read(buffer);
					} catch(SocketTimeoutException e) {
						continue;
					}
					if(read < 0) {
						break;
					}
					if(read == 0) {
						continue;
					}
					noteInbound();
					byte[] chunk = new byte[read];
					System.arraycopy(buffer, 0, chunk, 0, read);
					dispatchChunk(chunk);
				}
			} catch(IOException e) {
				if(connected.get()) {
					String msg = e.getMessage() == null ? "" : e.getMessage();
					/*
					 * Connection reset / closed are common when F407 reboots, re-flashes,
					 * or the user reconnects — log briefly without a full stack dump.
					 */
					if(e instanceof SocketException
							&& (msg.contains("Connection reset")
									|| msg.contains("Socket closed")
									|| msg.contains("Broken pipe"))) {
						logger.info("GC TCP disconnected by device: " + msg);
					} else {
						logger.warn("GC TCP reader stopped", e);
					}
				}
			} finally {
				boolean wasConnected = connected.getAndSet(false);
				failPendingResponses(new IOException("Connection closed"));
				failAsciiCapture(new IOException("Connection closed"));
				closeQuietly(socket);
				socket = null;
				if(wasConnected && connectionLostHandler != null) {
					connectionLostHandler.run();
				}
			}
		});
	}

	private void dispatchChunk(byte[] chunk) {

		/*
		 * ASCII temperature/status exchanges and binary ACQ_DATA share one TCP
		 * stream. Always run the frame parser so a READ_*_TEMP wait cannot
		 * swallow chromatogram batches. AsciiCapture skips AA55 frames so JSON
		 * / WRITE_OK are not corrupted by the same bytes.
		 */
		AsciiCapture capture = asciiCapture.get();
		if(capture != null) {
			if(capture.feed(chunk)) {
				asciiCapture.compareAndSet(capture, null);
				if(!capture.future.isDone()) {
					capture.future.complete(capture.buffer.toString());
				}
			}
		}
		frameParser.append(chunk);
		for(GcFrame frame : frameParser.drainFrames()) {
			logger.info("Received frame: " + frame);
			completePendingResponse(frame);
			for(IGcFrameListener listener : frameListeners) {
				try {
					listener.frameReceived(frame);
				} catch(RuntimeException e) {
					logger.warn("Frame listener failed", e);
				}
			}
		}
	}

	private void failAsciiCapture(IOException reason) {

		AsciiCapture capture = asciiCapture.getAndSet(null);
		if(capture != null && !capture.future.isDone()) {
			capture.future.completeExceptionally(reason);
		}
	}

	private void completePendingResponse(GcFrame frame) {

		CompletableFuture<GcFrame> responseFuture = pendingResponses.remove(frame.getSequence() & 0xFF);
		if(responseFuture != null && !responseFuture.isDone()) {
			responseFuture.complete(frame);
		}
	}

	private void failPendingResponses(IOException reason) {

		for(CompletableFuture<GcFrame> responseFuture : pendingResponses.values()) {
			responseFuture.completeExceptionally(reason);
		}
		pendingResponses.clear();
	}

	private static void closeQuietly(Socket socket) {

		if(socket == null) {
			return;
		}
		try {
			socket.close();
		} catch(IOException e) {
			logger.warn("Failed to close socket", e);
		}
	}

	private static void closeQuietly(OutputStream outputStream) {

		if(outputStream == null) {
			return;
		}
		try {
			outputStream.close();
		} catch(IOException e) {
			logger.warn("Failed to close output stream", e);
		}
	}
	/**
	 * Column-oven PID (F407 KVDB key ovenPid / TCP READ_OVEN_PID).
	 */
	public static final class OvenPid {

		private final float kp;
		private final float ki;
		private final float kd;

		public OvenPid(float kp, float ki, float kd) {

			this.kp = kp;
			this.ki = ki;
			this.kd = kd;
		}

		public float getKp() {

			return kp;
		}

		public float getKi() {

			return ki;
		}

		public float getKd() {

			return kd;
		}

		public String toDeviceJson() {

			return "{\n"
					+ "  \"kp\": \"" + ColumnOvenProgram.formatNumber(kp) + "\",\n"
					+ "  \"ki\": \"" + ColumnOvenProgram.formatNumber(ki) + "\",\n"
					+ "  \"kd\": \"" + ColumnOvenProgram.formatNumber(kd) + "\"\n"
					+ "}\n";
		}

		public static OvenPid fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			return new OvenPid(parseQuotedFloat(json, "kp"), parseQuotedFloat(json, "ki"), parseQuotedFloat(json, "kd"));
		}

		static float parseQuotedFloat(String json, String key) {

			String pattern = "\"" + key + "\"";
			int keyIdx = json.indexOf(pattern);
			if(keyIdx < 0) {
				throw new IllegalArgumentException("Missing key " + key);
			}
			int colon = json.indexOf(':', keyIdx);
			if(colon < 0) {
				throw new IllegalArgumentException("Missing ':' after " + key);
			}
			int i = colon + 1;
			while(i < json.length() && Character.isWhitespace(json.charAt(i))) {
				i++;
			}
			if(i >= json.length() || json.charAt(i) != '"') {
				throw new IllegalArgumentException("Expected quoted value for " + key);
			}
			i++;
			int start = i;
			while(i < json.length() && json.charAt(i) != '"') {
				i++;
			}
			if(i >= json.length()) {
				throw new IllegalArgumentException("Unterminated value for " + key);
			}
			return Float.parseFloat(json.substring(start, i).trim());
		}

		static String parseQuotedString(String json, String key) {

			String pattern = "\"" + key + "\"";
			int keyIdx = json.indexOf(pattern);
			if(keyIdx < 0) {
				throw new IllegalArgumentException("Missing key " + key);
			}
			int colon = json.indexOf(':', keyIdx);
			if(colon < 0) {
				throw new IllegalArgumentException("Missing ':' after " + key);
			}
			int i = colon + 1;
			while(i < json.length() && Character.isWhitespace(json.charAt(i))) {
				i++;
			}
			if(i >= json.length() || json.charAt(i) != '"') {
				throw new IllegalArgumentException("Expected quoted value for " + key);
			}
			i++;
			int start = i;
			while(i < json.length() && json.charAt(i) != '"') {
				i++;
			}
			if(i >= json.length()) {
				throw new IllegalArgumentException("Unterminated value for " + key);
			}
			return json.substring(start, i);
		}
	}

	/**
	 * Live column-oven setpoint + measured temperature (F407 READ_OVEN_TEMP).
	 */
	public static final class OvenLiveTemp {

		private final float setpoint;
		private final float actual;
		private final boolean valid;
		private final int state;
		private final int stage;
		private final int duty;
		private final int mode;
		private final boolean fault;
		private final String faultReason;

		public OvenLiveTemp(float setpoint, float actual, boolean valid, int state, int stage, int duty, int mode, boolean fault, String faultReason) {

			this.setpoint = setpoint;
			this.actual = actual;
			this.valid = valid;
			this.state = state;
			this.stage = stage;
			this.duty = duty;
			this.mode = mode;
			this.fault = fault;
			this.faultReason = faultReason == null ? "" : faultReason;
		}

		public float getSetpoint() {

			return setpoint;
		}

		public float getActual() {

			return actual;
		}

		public boolean isValid() {

			return valid;
		}

		public int getState() {

			return state;
		}

		public int getStage() {

			return stage;
		}

		public int getDuty() {

			return duty;
		}

		public int getMode() {

			return mode;
		}

		public boolean isFault() {

			return fault;
		}

		public String getFaultReason() {

			return faultReason;
		}

		public static OvenLiveTemp fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			float setpoint = OvenPid.parseQuotedFloat(json, "setpoint");
			float actual = OvenPid.parseQuotedFloat(json, "actual");
			boolean valid = (int)OvenPid.parseQuotedFloat(json, "valid") != 0;
			int state = (int)OvenPid.parseQuotedFloat(json, "state");
			int stage = (int)OvenPid.parseQuotedFloat(json, "stage");
			int duty = json.contains("\"duty\"") ? (int)OvenPid.parseQuotedFloat(json, "duty") : 0;
			int mode = json.contains("\"mode\"") ? (int)OvenPid.parseQuotedFloat(json, "mode") : 0;
			boolean fault = false;
			String faultReason = "";
			if(json.contains("\"fault\"")) {
				fault = (int)OvenPid.parseQuotedFloat(json, "fault") != 0;
			}
			if(json.contains("\"faultReason\"")) {
				faultReason = OvenPid.parseQuotedString(json, "faultReason");
			}
			return new OvenLiveTemp(setpoint, actual, valid, state, stage, duty, mode, fault, faultReason);
		}
	}

	/**
	 * Inlet / detector live setpoint + measured temperature
	 * (F407 {@code READ_INLET_TEMP} / {@code READ_DETECTOR_TEMP}).
	 */
	public static final class AuxLiveTemp {

		private final float setpoint;
		private final float actual;
		private final boolean valid;
		private final int duty;

		public AuxLiveTemp(float setpoint, float actual, boolean valid, int duty) {

			this.setpoint = setpoint;
			this.actual = actual;
			this.valid = valid;
			this.duty = duty;
		}

		public float getSetpoint() {

			return setpoint;
		}

		public float getActual() {

			return actual;
		}

		public boolean isValid() {

			return valid;
		}

		public int getDuty() {

			return duty;
		}

		public static String toDeviceJson(float setpointC) {

			return "{\n"
					+ "  \"setpoint\": \"" + ColumnOvenProgram.formatNumber(setpointC) + "\"\n"
					+ "}\n";
		}

		public static AuxLiveTemp fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			float setpoint = OvenPid.parseQuotedFloat(json, "setpoint");
			float actual = OvenPid.parseQuotedFloat(json, "actual");
			boolean valid = json.contains("\"valid\"") && (int)OvenPid.parseQuotedFloat(json, "valid") != 0;
			int duty = json.contains("\"duty\"") ? (int)OvenPid.parseQuotedFloat(json, "duty") : 0;
			return new AuxLiveTemp(setpoint, actual, valid, duty);
		}
	}

	public static final class FidStatus {

		private final boolean online;
		private final boolean autoIgnite;
		private final boolean busy;
		private final boolean flame;
		private final boolean fire1;
		private final boolean valve1;
		private final boolean valve2;
		private final int vlogUv;
		private final int currentPa;
		private final float detectorC;
		private final boolean detectorValid;
		private final String state;

		public FidStatus(boolean online, boolean autoIgnite, boolean busy, boolean flame, boolean fire1, boolean valve1, boolean valve2, int vlogUv, int currentPa, float detectorC, boolean detectorValid, String state) {

			this.online = online;
			this.autoIgnite = autoIgnite;
			this.busy = busy;
			this.flame = flame;
			this.fire1 = fire1;
			this.valve1 = valve1;
			this.valve2 = valve2;
			this.vlogUv = vlogUv;
			this.currentPa = currentPa;
			this.detectorC = detectorC;
			this.detectorValid = detectorValid;
			this.state = state == null ? "idle" : state;
		}

		public boolean isOnline() {

			return online;
		}

		public boolean isAutoIgnite() {

			return autoIgnite;
		}

		public boolean isBusy() {

			return busy;
		}

		public boolean isFlame() {

			return flame;
		}

		public boolean isFire1() {

			return fire1;
		}

		public boolean isValve1() {

			return valve1;
		}

		public boolean isValve2() {

			return valve2;
		}

		public boolean areValvesOpen() {

			return valve1 || valve2;
		}

		public int getVlogUv() {

			return vlogUv;
		}

		public int getCurrentPa() {

			return currentPa;
		}

		public float getDetectorC() {

			return detectorC;
		}

		public boolean isDetectorValid() {

			return detectorValid;
		}

		public String getState() {

			return state;
		}

		public static FidStatus fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			return new FidStatus(
					quotedFlag(json, "online"),
					quotedFlag(json, "auto"),
					quotedFlag(json, "busy"),
					quotedFlag(json, "flame"),
					quotedFlag(json, "fire1"),
					quotedFlag(json, "valve1"),
					quotedFlag(json, "valve2"),
					quotedInt(json, "vlogUv"),
					quotedInt(json, "currentPa"),
					json.contains("\"detectorC\"") ? OvenPid.parseQuotedFloat(json, "detectorC") : 0.0f,
					quotedFlag(json, "detectorValid"),
					json.contains("\"state\"") ? OvenPid.parseQuotedString(json, "state") : "idle");
		}

		private static boolean quotedFlag(String json, String key) {

			return json.contains("\"" + key + "\"") && (int)OvenPid.parseQuotedFloat(json, key) != 0;
		}

		private static int quotedInt(String json, String key) {

			return json.contains("\"" + key + "\"") ? (int)OvenPid.parseQuotedFloat(json, key) : 0;
		}
	}

	public static final class GasPressure {

		private final float h2Mpa;
		private final float airMpa;

		public GasPressure(float h2Mpa, float airMpa) {

			this.h2Mpa = h2Mpa;
			this.airMpa = airMpa;
		}

		public float getH2Mpa() {

			return h2Mpa;
		}

		public float getAirMpa() {

			return airMpa;
		}

		public static GasPressure fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			float h2 = json.contains("\"h2\"") ? OvenPid.parseQuotedFloat(json, "h2") : OvenPid.parseQuotedFloat(json, "adc1");
			float air = json.contains("\"air\"") ? OvenPid.parseQuotedFloat(json, "air") : OvenPid.parseQuotedFloat(json, "adc2");
			return new GasPressure(h2, air);
		}
	}

	/**
	 * F407 {@code READ_CONTROL_STATUS}: {@code owner} is {@code PC} or {@code NONE}.
	 * {@code heat} is {@code 1} while inlet, detector, or oven heat is running.
	 */
	public static final class ControlStatus {

		private final String owner;
		private final boolean acquiring;
		private final boolean heatRunning;
		private final int selMask;

		public ControlStatus(String owner, boolean acquiring) {

			this(owner, acquiring, false);
		}

		public ControlStatus(String owner, boolean acquiring, boolean heatRunning) {

			this(owner, acquiring, heatRunning, -1);
		}

		public ControlStatus(String owner, boolean acquiring, boolean heatRunning, int selMask) {

			this.owner = owner == null || owner.isBlank() ? "NONE" : owner;
			this.acquiring = acquiring;
			this.heatRunning = heatRunning;
			this.selMask = selMask;
		}

		public String getOwner() {

			return owner;
		}

		public boolean isPcOnline() {

			return "PC".equalsIgnoreCase(owner);
		}

		public boolean isAcquiring() {

			return acquiring;
		}

		public boolean isHeatRunning() {

			return heatRunning;
		}

		/** Bitmask from F407 {@code sel}, or {@code -1} if the key is missing. */
		public int getSelMask() {

			return selMask;
		}

		public boolean hasSel() {

			return selMask >= 0;
		}

		public static ControlStatus fromDeviceJson(String json) {

			Objects.requireNonNull(json, "json");
			return new ControlStatus(extractQuoted(json, "owner"), quotedTruthy(json, "acq"), quotedTruthy(json, "heat"), parseSelMask(json));
		}

		private static int parseSelMask(String json) {

			if(json.indexOf("\"sel\"") < 0) {
				return -1;
			}
			String value = extractQuoted(json, "sel");
			if(value.isEmpty()) {
				return -1;
			}
			try {
				int sel = Integer.parseInt(value.trim());
				if(sel < 0) {
					return 0;
				}
				return Math.min(sel, 7);
			} catch(NumberFormatException ex) {
				return -1;
			}
		}

		private static String extractQuoted(String json, String key) {

			String needle = "\"" + key + "\"";
			int keyIndex = json.indexOf(needle);
			if(keyIndex < 0) {
				return "";
			}
			int colon = json.indexOf(':', keyIndex + needle.length());
			if(colon < 0) {
				return "";
			}
			int firstQuote = json.indexOf('"', colon + 1);
			if(firstQuote < 0) {
				return "";
			}
			int secondQuote = json.indexOf('"', firstQuote + 1);
			if(secondQuote < 0) {
				return "";
			}
			return json.substring(firstQuote + 1, secondQuote);
		}

		private static boolean quotedTruthy(String json, String key) {

			String value = extractQuoted(json, key);
			return "1".equals(value) || "true".equalsIgnoreCase(value);
		}
	}
}

final class AsciiCapture {

	enum Mode {
		JSON_OBJECT, WRITE_ACK
	}

	final Mode mode;
	final CompletableFuture<String> future;
	final StringBuilder buffer = new StringBuilder(1536);
	private int braceDepth;
	private boolean inString;
	private boolean escape;
	private boolean started;
	private int frameHdrCount;
	private int frameSkipRemain;
	private final byte[] frameHdr = new byte[6];

	AsciiCapture(Mode mode, CompletableFuture<String> future) {

		this.mode = mode;
		this.future = future;
	}

	boolean feed(byte[] chunk) {

		for(byte value : chunk) {
			if(skipBinaryFrameByte(value)) {
				continue;
			}
			char c = (char)(value & 0xFF);
			if(mode == Mode.WRITE_ACK) {
				if(c > 127 || (c < 32 && c != '\r' && c != '\n' && c != '\t')) {
					continue;
				}
				buffer.append(c);
				String text = buffer.toString().toUpperCase(Locale.ROOT);
				if(text.contains(GcAsciiProtocol.WRITE_OVEN_ACK) || text.contains(GcAsciiProtocol.WRITE_OVEN_ERR) || text.contains(GcAsciiProtocol.WRITE_DENIED)) {
					return true;
				}
				if(buffer.length() > 64) {
					future.completeExceptionally(new IOException("Oven write ACK overflow: " + buffer));
					return true;
				}
				continue;
			}
			/*
			 * JSON_OBJECT: wait for '{', then brace-balance like app_tcp_server.c.
			 */
			if(!started) {
				if(Character.isWhitespace(c)) {
					continue;
				}
				if(c != '{') {
					continue;
				}
				started = true;
				braceDepth = 0;
				inString = false;
				escape = false;
			}
			buffer.append(c);
			if(buffer.length() >= 1536) {
				future.completeExceptionally(new IOException("Oven program JSON exceeds 1536 bytes"));
				return true;
			}
			if(inString) {
				if(escape) {
					escape = false;
				} else if(c == '\\') {
					escape = true;
				} else if(c == '"') {
					inString = false;
				}
				continue;
			}
			if(c == '"') {
				inString = true;
			} else if(c == '{') {
				braceDepth++;
			} else if(c == '}') {
				braceDepth--;
				if(braceDepth == 0) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Skip one byte of an {@code AA55} frame so JSON / WRITE_OK capture is not
	 * poisoned by ACQ_DATA that arrives during the same ASCII wait.
	 */
	private boolean skipBinaryFrameByte(byte value) {

		if(frameSkipRemain > 0) {
			frameSkipRemain--;
			return true;
		}
		if(frameHdrCount == 0) {
			if(value != GcFrameCodec.HEADER_0) {
				return false;
			}
			frameHdr[0] = value;
			frameHdrCount = 1;
			return true;
		}
		if(frameHdrCount == 1) {
			if(value != GcFrameCodec.HEADER_1) {
				frameHdrCount = 0;
				return true;
			}
			frameHdr[1] = value;
			frameHdrCount = 2;
			return true;
		}
		frameHdr[frameHdrCount++] = value;
		if(frameHdrCount < 6) {
			return true;
		}
		int payloadLength = (frameHdr[4] & 0xFF) | ((frameHdr[5] & 0xFF) << 8);
		frameHdrCount = 0;
		if(payloadLength <= 4096) {
			frameSkipRemain = payloadLength + 4;
		}
		return true;
	}
}

/**
 * ASCII line protocol used by F407 app_tcp_server.c:
 * Handshake: host sends GCWS\tHELLO\r\n, device replies HELLO_OK\n
 * Oven: READ_OVEN_PROGRAM / WRITE_OVEN_PROGRAM (+ JSON) → WRITE_OK
 * PID: READ_OVEN_PID / WRITE_OVEN_PID (+ JSON) → WRITE_OK
 *      READ_INLET_PID / WRITE_INLET_PID, READ_DETECTOR_PID / WRITE_DETECTOR_PID
 * Temp: READ_OVEN_TEMP → JSON setpoint/actual
 *       READ/WRITE_INLET_TEMP, READ/WRITE_DETECTOR_TEMP
 * Dual-client roles (panel read + e-stop, PC primary write):
 * docs/GCWS-DUAL-CONTROL.md
 */
final class GcAsciiProtocol {

	static final String HANDSHAKE_LINE = "GCWS\tHELLO";
	static final String HANDSHAKE_PC_LINE = "GCWS\tHELLO\tPC";
	static final String HANDSHAKE_ACK = "HELLO_OK";
	static final String READ_OVEN_PROGRAM_LINE = "GCWS\tREAD_OVEN_PROGRAM";
	static final String WRITE_OVEN_PROGRAM_LINE = "GCWS\tWRITE_OVEN_PROGRAM";
	static final String READ_OVEN_PID_LINE = "GCWS\tREAD_OVEN_PID";
	static final String WRITE_OVEN_PID_LINE = "GCWS\tWRITE_OVEN_PID";
	static final String READ_INLET_PID_LINE = "GCWS\tREAD_INLET_PID";
	static final String WRITE_INLET_PID_LINE = "GCWS\tWRITE_INLET_PID";
	static final String READ_DETECTOR_PID_LINE = "GCWS\tREAD_DETECTOR_PID";
	static final String WRITE_DETECTOR_PID_LINE = "GCWS\tWRITE_DETECTOR_PID";
	static final String READ_OVEN_TEMP_LINE = "GCWS\tREAD_OVEN_TEMP";
	static final String READ_INLET_TEMP_LINE = "GCWS\tREAD_INLET_TEMP";
	static final String WRITE_INLET_TEMP_LINE = "GCWS\tWRITE_INLET_TEMP";
	static final String READ_DETECTOR_TEMP_LINE = "GCWS\tREAD_DETECTOR_TEMP";
	static final String WRITE_DETECTOR_TEMP_LINE = "GCWS\tWRITE_DETECTOR_TEMP";
	static final String WRITE_ZONE_SELECT_LINE = "GCWS\tWRITE_ZONE_SELECT";
	static final String START_INLET_HEAT_LINE = "GCWS\tSTART_INLET_HEAT";
	static final String STOP_INLET_HEAT_LINE = "GCWS\tSTOP_INLET_HEAT";
	static final String START_DETECTOR_HEAT_LINE = "GCWS\tSTART_DETECTOR_HEAT";
	static final String STOP_DETECTOR_HEAT_LINE = "GCWS\tSTOP_DETECTOR_HEAT";
	static final String START_OVEN_CONTROL_LINE = "GCWS\tSTART_OVEN_CONTROL";
	static final String STOP_OVEN_CONTROL_LINE = "GCWS\tSTOP_OVEN_CONTROL";
	static final String START_FID_IGNITE_LINE = "GCWS\tSTART_FID_IGNITE";
	static final String STOP_FID_VALVES_LINE = "GCWS\tSTOP_FID_VALVES";
	static final String START_FID_VALVES_LINE = "GCWS\tSTART_FID_VALVES";
	static final String FID_AUTO_ON_LINE = "GCWS\tFID_AUTO_ON";
	static final String FID_AUTO_OFF_LINE = "GCWS\tFID_AUTO_OFF";
	static final String READ_FID_STATUS_LINE = "GCWS\tREAD_FID_STATUS";
	static final String READ_GAS_PRESSURE_LINE = "GCWS\tREAD_GAS_PRESSURE";
	static final String READ_CONTROL_STATUS_LINE = "GCWS\tREAD_CONTROL_STATUS";
	static final String WRITE_OVEN_ACK = "WRITE_OK";
	static final String WRITE_OVEN_ERR = "WRITE_FAIL";
	static final String WRITE_DENIED = "DENIED";
	static final int DEFAULT_HANDSHAKE_TIMEOUT_MS = 5000;
	private static final int MAX_HANDSHAKE_RESPONSE_BYTES = 256;

	private GcAsciiProtocol() {

	}

	static byte[] encodeLine(String lineWithoutCrLf) {

		Objects.requireNonNull(lineWithoutCrLf, "lineWithoutCrLf");
		return (lineWithoutCrLf + "\r\n").getBytes(StandardCharsets.US_ASCII);
	}

	static boolean isHandshakeOk(String responseText) {

		if(responseText == null || responseText.isBlank()) {
			return false;
		}
		return responseText.toUpperCase(Locale.ROOT).contains(HANDSHAKE_ACK);
	}

	static void performHandshake(Socket socket) throws IOException {

		performHandshake(socket, HANDSHAKE_LINE, DEFAULT_HANDSHAKE_TIMEOUT_MS);
	}

	static void performWorkstationHandshake(Socket socket) throws IOException {

		performHandshake(socket, HANDSHAKE_PC_LINE, DEFAULT_HANDSHAKE_TIMEOUT_MS);
	}

	static void performHandshake(Socket socket, int timeoutMs) throws IOException {

		performHandshake(socket, HANDSHAKE_LINE, timeoutMs);
	}

	private static void performHandshake(Socket socket, String helloLine, int timeoutMs) throws IOException {

		Objects.requireNonNull(socket, "socket");
		Objects.requireNonNull(helloLine, "helloLine");
		if(!socket.isConnected() || socket.isClosed()) {
			throw new IOException("Socket is not connected");
		}
		int previousTimeout = socket.getSoTimeout();
		try {
			socket.setSoTimeout(Math.max(200, timeoutMs));
			OutputStream outputStream = socket.getOutputStream();
			InputStream inputStream = socket.getInputStream();
			outputStream.write(encodeLine(helloLine));
			outputStream.flush();
			String response = readResponseText(inputStream, MAX_HANDSHAKE_RESPONSE_BYTES, timeoutMs);
			if(!isHandshakeOk(response)) {
				String detail = response == null || response.isBlank() ? "(empty)" : response.strip();
				throw new IOException("GC handshake failed: expected HELLO_OK, got: " + detail);
			}
		} finally {
			try {
				socket.setSoTimeout(previousTimeout);
			} catch(IOException e) {
				/*
				 * Socket may already be closed after a failed handshake.
				 */
			}
		}
	}

	static String readResponseText(InputStream inputStream, int maxBytes, int timeoutMs) throws IOException {

		Objects.requireNonNull(inputStream, "inputStream");
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[128];
		long deadline = System.currentTimeMillis() + Math.max(200, timeoutMs);
		while(buffer.size() < maxBytes && System.currentTimeMillis() < deadline) {
			try {
				int read = inputStream.read(chunk);
				if(read < 0) {
					break;
				}
				if(read == 0) {
					continue;
				}
				buffer.write(chunk, 0, read);
				String text = buffer.toString(StandardCharsets.US_ASCII);
				if(isHandshakeOk(text) || text.indexOf('\n') >= 0) {
					return text;
				}
			} catch(SocketTimeoutException e) {
				if(buffer.size() > 0) {
					return buffer.toString(StandardCharsets.US_ASCII);
				}
			}
		}
		if(buffer.size() == 0) {
			throw new IOException("GC handshake timed out waiting for HELLO_OK");
		}
		return buffer.toString(StandardCharsets.US_ASCII);
	}
}
