using System.Net.Sockets;

namespace HwMinimal;

public sealed class TcpByteTransport : IByteTransport, IAsyncDisposable
{
    private readonly TcpClient _client;
    private readonly NetworkStream _stream;

    private TcpByteTransport(TcpClient client)
    {
        _client = client;
        _stream = client.GetStream();
    }

    public static async Task<TcpByteTransport> ConnectAsync(string host, int port, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(host))
        {
            throw new InvalidOperationException("主机是空的，未连接。");
        }
        if (port is < 1 or > 65535)
        {
            throw new InvalidOperationException("端口不在 1–65535，未连接。");
        }
        using var limit = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        limit.CancelAfter(TimeSpan.FromSeconds(3));
        var client = new TcpClient { NoDelay = true };
        try
        {
            await client.ConnectAsync(host.Trim(), port, limit.Token).ConfigureAwait(false);
            return new TcpByteTransport(client);
        }
        catch
        {
            client.Dispose();
            throw;
        }
    }

    public ValueTask WriteAsync(ReadOnlyMemory<byte> data, CancellationToken cancellationToken)
    {
        return _stream.WriteAsync(data, cancellationToken);
    }

    public async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken)
    {
        using var limit = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        limit.CancelAfter(TimeSpan.FromMilliseconds(200));
        try
        {
            int n = await _stream.ReadAsync(buffer, limit.Token).ConfigureAwait(false);
            return n == 0 ? -1 : n;
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return 0;
        }
    }

    public async ValueTask DisposeAsync()
    {
        await _stream.DisposeAsync().ConfigureAwait(false);
        _client.Dispose();
    }
}
